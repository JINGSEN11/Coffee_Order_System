package com.vincent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;

/**
 * 按路径区分 JSON 字段命名风格。
 *
 * 背景：application.yml 里配置的是全局 {@code spring.jackson.property-naming-strategy: SNAKE_CASE}，
 * 这是为小程序（/api/app/**）加的——C 端契约、cof_sys 代码都以下划线字段为准。
 * 但管理端（/admin/**）的契约是驼峰：《管理端-API联调用例.md》《管理端-API联调用例.postman_collection.json》
 * 以及 admin-web 源码读的都是 camelCase（realName / orderNo / warnStock / categoryId ...）。
 * 全局下划线把管理端一起带偏了，表现为：
 *  - 响应侧：管理端表格里 categoryName / orderNo / createdAt / warnStock / productId 全部取不到值；
 *  - 请求侧：新增商品 {categoryId} 不绑定、入库 {productId} 不绑定（报「SKU不存在」）、
 *            预警 {warnStock} 不绑定（设置了没反应）。
 *
 * 做法：在既有 Jackson 转换器前面插一个「仅对 /admin/** 生效」的转换器，
 * 它的 ObjectMapper 由原 mapper rebuild 而来（保留 Boot 配置的日期格式、JavaTime 模块），
 * 只把命名策略换成 LOWER_CAMEL_CASE。/api/app/** 仍走原来的 SNAKE_CASE 转换器。
 *
 * canRead/canWrite 会按当前请求线程判断路径，转换器本身无共享可变状态，线程安全。
 */
@Configuration
public class JacksonNamingConfig implements WebMvcConfigurer {

    /** 管理端请求前缀，命中即使用驼峰命名 */
    private static final String ADMIN_PATH_PREFIX = "/admin";

    /** 管理端 LocalDateTime 的输出格式，与 application.yml 里 time-format 的意图一致 */
    private static final DateTimeFormatter DATE_TIME_OUT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 入参格式：同时接受 "yyyy-MM-dd HH:mm:ss" 和 ISO 的 "yyyy-MM-ddTHH:mm:ss"，
     * 避免历史调用方（Postman 集合等）在格式切换后连不上。
     */
    private static final DateTimeFormatter DATE_TIME_IN = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd")
            .optionalStart().appendLiteral('T').optionalEnd()
            .optionalStart().appendLiteral(' ').optionalEnd()
            .appendPattern("HH:mm:ss")
            .toFormatter();

    private static boolean isAdminRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return false;
        }
        return attrs.getRequest().getRequestURI().startsWith(ADMIN_PATH_PREFIX);
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        for (int i = 0; i < converters.size(); i++) {
            HttpMessageConverter<?> scoped = AdminCamelCaseConverter.wrap(converters.get(i));
            if (scoped != null) {
                // 插到原转换器之前，管理端请求由它接管
                converters.add(i, scoped);
                return;
            }
        }
    }

    /**
     * application.yml 里的 spring.jackson.time-format 其实不是 Spring Boot 的有效属性
     * （有效的是 date-format，且只作用于 java.util.Date），所以 LocalDateTime 一直在用
     * Jackson 默认的 ISO 格式输出，客户端按 "yyyy-MM-dd HH:mm:ss" 提交时会报
     * DateTimeParseException（例如新增优惠券带有效期必 500）。
     * 这里显式给管理端注册 JavaTime 的读写格式。
     */
    private static SimpleModule localDateTimeModule() {
        SimpleModule module = new SimpleModule("admin-local-date-time");
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DATE_TIME_OUT));
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DATE_TIME_IN));
        return module;
    }

    /** 只接管 /admin/** 的 Jackson 转换器，命名策略为驼峰 */
    private static final class AdminCamelCaseConverter extends JacksonJsonHttpMessageConverter {

        private AdminCamelCaseConverter(JsonMapper mapper) {
            super(mapper);
        }

        /**
         * 若 given 是 Jackson 转换器则派生一个管理端专用实例，否则返回 null。
         */
        static HttpMessageConverter<?> wrap(HttpMessageConverter<?> converter) {
            if (!(converter instanceof JacksonJsonHttpMessageConverter source)
                    || converter instanceof AdminCamelCaseConverter) {
                return null;
            }
            JsonMapper camelCaseMapper = source.getMapper().<JsonMapper, JsonMapper.Builder>rebuild()
                    .propertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
                    .addModule(localDateTimeModule())
                    .build();

            AdminCamelCaseConverter scoped = new AdminCamelCaseConverter(camelCaseMapper);
            scoped.setSupportedMediaTypes(source.getSupportedMediaTypes());
            return scoped;
        }

        @Override
        public boolean canRead(ResolvableType type, MediaType mediaType) {
            return isAdminRequest() && super.canRead(type, mediaType);
        }

        @Override
        public boolean canRead(Class<?> clazz, MediaType mediaType) {
            return isAdminRequest() && super.canRead(clazz, mediaType);
        }

        @Override
        public boolean canWrite(ResolvableType type, Class<?> clazz, MediaType mediaType) {
            return isAdminRequest() && super.canWrite(type, clazz, mediaType);
        }

        @Override
        public boolean canWrite(Class<?> clazz, MediaType mediaType) {
            return isAdminRequest() && super.canWrite(clazz, mediaType);
        }
    }
}
