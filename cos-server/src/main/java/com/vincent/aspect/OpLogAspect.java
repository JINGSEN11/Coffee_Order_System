package com.vincent.aspect;

import com.vincent.annotation.OpLog;
import com.vincent.common.BaseContext;
import com.vincent.mapper.OpLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 管理端操作日志切面（F-A03）。
 *
 * 只在三个条件同时满足时落库，其余一律不记：
 *   1. 请求落在 /admin/**；
 *   2. HTTP 方法是写操作（POST/PUT/DELETE/PATCH）—— 读接口记了只会把日志表刷成流水账；
 *   3. 方法正常返回 —— 抛异常说明操作没成功，而 op_log 表没有「结果」列，记下来反而误导。
 *
 * 日志写入失败绝不影响业务：整段包在 try/catch 里，只打 error 日志。
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class OpLogAspect {

    /** URI 里的模块兜底：/admin/{module}/... 的 {module} → 中文 */
    private static final Map<String, String> MODULE_BY_URI = Map.ofEntries(
            Map.entry("product", "商品"),
            Map.entry("category", "商品分类"),
            Map.entry("sku", "SKU"),
            Map.entry("stock", "库存"),
            Map.entry("order", "订单"),
            Map.entry("coupon", "优惠券"),
            Map.entry("banner", "轮播图"),
            Map.entry("notice", "公告"),
            Map.entry("member", "会员"),
            Map.entry("points", "积分"),
            Map.entry("shop", "门店"),
            Map.entry("table", "桌码"),
            Map.entry("employee", "员工"),
            Map.entry("role", "角色"),
            Map.entry("menu", "菜单"),
            Map.entry("config", "系统配置"),
            Map.entry("review", "评价"),
            Map.entry("upload", "文件上传")
    );

    /** 参数名命中即整体脱敏 */
    private static final Pattern SENSITIVE_NAME =
            Pattern.compile("(?i)(password|passwd|pwd|secret|token|api[_-]?key|access[_-]?key)");

    /**
     * JSON 里敏感字段的值抹成 ***。
     * 不能靠 DTO 的 Lombok toString()：EmployeeCreateDTO 的 toString 会把 password 原样带出来。
     */
    private static final Pattern SENSITIVE_JSON = Pattern.compile(
            "(\"[^\"]*(?:password|passwd|pwd|secret|token|api[_-]?key|access[_-]?key)[^\"]*\"\\s*:\\s*)\"(?:[^\"\\\\]|\\\\.)*\"");

    private static final int MAX_DETAIL = 1000;

    private final OpLogMapper opLogMapper;
    private final ObjectMapper objectMapper;

    @Around("execution(public * com.vincent.controller.admin..*.*(..))")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        HttpServletRequest request = currentRequest();
        // 非管理端、读操作、以及登录/刷新（认证事件不是「操作」，且那时还没有身份，
        // 记下来 operator 只能是「未知」）—— 都直接放行不记
        if (request == null || !isWrite(request) || !shouldLog(request.getRequestURI())) {
            return pjp.proceed();
        }

        Object result = pjp.proceed();

        try {
            record(pjp, request);
        } catch (Exception e) {
            // 记日志失败不能反过来把业务搞挂
            log.error("写操作日志失败：{} {}", request.getMethod(), request.getRequestURI(), e);
        }
        return result;
    }

    /** 只记管理端业务写操作：/admin/** 且不是 /admin/auth/**（登录、刷新令牌） */
    private boolean shouldLog(String uri) {
        return uri.startsWith("/admin") && !uri.startsWith("/admin/auth/");
    }

    /* ---------------- 内部方法 ---------------- */

    private void record(ProceedingJoinPoint pjp, HttpServletRequest request) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        OpLog annotation = signature.getMethod().getAnnotation(OpLog.class);
        if (annotation == null) {
            annotation = pjp.getTarget().getClass().getAnnotation(OpLog.class);
        }

        String module = annotation != null && StringUtils.hasText(annotation.module())
                ? annotation.module()
                : moduleFromUri(request.getRequestURI());
        String action = annotation != null && StringUtils.hasText(annotation.action())
                ? annotation.action()
                : signature.getMethod().getName();
        boolean withArgs = annotation == null || annotation.args();

        com.vincent.entity.OpLog entity = new com.vincent.entity.OpLog();
        entity.setOperator(currentOperator());
        entity.setModule(module);
        entity.setAction(action);
        entity.setIp(clientIp(request));
        entity.setDetail(truncate(describe(signature, pjp.getArgs(), withArgs)
                + " ｜ " + request.getMethod() + " " + request.getRequestURI(), MAX_DETAIL));
        entity.setCreatedAt(LocalDateTime.now());
        opLogMapper.insert(entity);
    }

    /** 拼 detail：路径参数与查询参数是要点（哪个订单被退了），复杂 DTO 按注解开关决定带不带 */
    private String describe(MethodSignature signature, Object[] args, boolean withArgs) {
        List<String> parts = new ArrayList<>();
        String[] names = signature.getParameterNames();
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (isInfrastructure(arg)) {
                continue;
            }
            String name = names != null && i < names.length ? names[i] : ("arg" + i);
            if (SENSITIVE_NAME.matcher(name).find()) {
                parts.add(name + "=***");
                continue;
            }
            if (isScalar(arg)) {
                parts.add(name + "=" + arg);
                continue;
            }
            if (!withArgs) {
                continue;
            }
            parts.add(name + "=" + safeJson(arg));
        }
        return parts.isEmpty() ? "-" : String.join(", ", parts);
    }

    /** DTO → JSON，敏感字段值抹掉；序列化不了就退回类名，绝不抛出去 */
    private String safeJson(Object value) {
        if (value == null) {
            return "null";
        }
        try {
            String json = objectMapper.writeValueAsString(value);
            return SENSITIVE_JSON.matcher(json).replaceAll("$1\"***\"");
        } catch (Exception e) {
            return value.getClass().getSimpleName();
        }
    }

    private boolean isWrite(HttpServletRequest request) {
        String method = request.getMethod();
        return "POST".equals(method) || "PUT".equals(method)
                || "DELETE".equals(method) || "PATCH".equals(method);
    }

    private boolean isInfrastructure(Object arg) {
        return arg instanceof HttpServletRequest
                || arg instanceof HttpServletResponse
                || arg instanceof MultipartFile
                || arg instanceof BindingResult;
    }

    private boolean isScalar(Object arg) {
        return arg == null
                || arg instanceof CharSequence
                || arg instanceof Number
                || arg instanceof Boolean
                || arg instanceof BigDecimal
                || arg instanceof Temporal
                || arg instanceof Enum<?>;
    }

    private String moduleFromUri(String uri) {
        // /admin/order/refund/status/1 → segments[2] = "order"
        String[] segments = uri.split("/");
        if (segments.length > 2) {
            String key = segments[2];
            return MODULE_BY_URI.getOrDefault(key, key);
        }
        return "管理端";
    }

    private String currentOperator() {
        BaseContext.Principal principal = BaseContext.get();
        if (principal == null) {
            return "未知";
        }
        return StringUtils.hasText(principal.username())
                ? principal.username()
                : "员工#" + principal.id();
    }

    private String clientIp(HttpServletRequest request) {
        for (String header : List.of("X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP")) {
            String value = request.getHeader(header);
            if (StringUtils.hasText(value) && !"unknown".equalsIgnoreCase(value)) {
                // X-Forwarded-For 可能是「客户端, 代理1, 代理2」，取第一个
                return value.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }

    private String truncate(String text, int max) {
        if (text == null || text.length() <= max) {
            return text;
        }
        return text.substring(0, max) + "…";
    }

}
