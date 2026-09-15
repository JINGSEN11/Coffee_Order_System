package com.vincent.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vincent.common.BaseContext;
import com.vincent.common.JwtUtil;
import com.vincent.common.exception.ServiceException;
import com.vincent.config.JwtProperties;
import com.vincent.config.WechatProperties;
import com.vincent.dto.AppBindPhoneDTO;
import com.vincent.dto.AppLoginDTO;
import com.vincent.entity.Member;
import com.vincent.mapper.MemberMapper;
import com.vincent.service.AppAuthService;
import com.vincent.vo.AppLoginVO;
import com.vincent.vo.AppMemberBriefVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 小程序端认证服务实现。
 * 复用既有 JwtUtil 与 JwtProperties#userSecretKey / userTtl，与 LoginInterceptor 的校验口径一致。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppAuthServiceImpl implements AppAuthService {

    private static final String CODE2SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session";
    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";
    private static final String PHONE_URL = "https://api.weixin.qq.com/wxa/business/getuserphonenumber";

    /**
     * 未配置小程序密钥时的开发态 openid（固定值）。
     * 必须固定：开发者工具的 wx.login code 每次都变，跟着 code 走会不停新建会员。
     */
    private static final String DEV_OPENID = "dev_openid_local_debug";

    private final MemberMapper memberMapper;
    private final JwtProperties jwtProperties;
    private final WechatProperties wechatProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppLoginVO login(AppLoginDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getCode())) {
            throw new ServiceException("登录凭证不能为空");
        }

        String openid = resolveOpenid(dto.getCode());
        Member member = memberMapper.selectOne(
                new LambdaQueryWrapper<Member>().eq(Member::getOpenid, openid).last("LIMIT 1")
        );

        if (member == null) {
            // 静默注册
            member = new Member();
            member.setOpenid(openid);
            member.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : "微信用户");
            member.setAvatar(StringUtils.hasText(dto.getAvatar()) ? dto.getAvatar() : "");
            member.setPoints(0);
            member.setStatus(1);
            member.setCreatedAt(LocalDateTime.now());
            member.setUpdatedAt(LocalDateTime.now());
            memberMapper.insert(member);
            log.info("微信静默注册新会员：id={}, openid={}", member.getId(), openid);
        } else if (member.getStatus() != null && member.getStatus() != 1) {
            throw new ServiceException("账号已被禁用，请联系管理员");
        } else if (StringUtils.hasText(dto.getNickname()) || StringUtils.hasText(dto.getAvatar())) {
            // 首次授权时补全昵称/头像
            Member update = new Member();
            update.setId(member.getId());
            if (StringUtils.hasText(dto.getNickname())) {
                update.setNickname(dto.getNickname());
                member.setNickname(dto.getNickname());
            }
            if (StringUtils.hasText(dto.getAvatar())) {
                update.setAvatar(dto.getAvatar());
                member.setAvatar(dto.getAvatar());
            }
            memberMapper.updateById(update);
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("id", member.getId());
        claims.put("openid", member.getOpenid());
        // 身份域：AppAuthInterceptor 会校验它必须为 user。
        // 会员 id 与员工 id 是两套主键，只有 scope 能区分，缺了它就无法判断令牌属于哪一端。
        claims.put("scope", BaseContext.SCOPE_USER);
        String token = JwtUtil.createToken(
                jwtProperties.getUserSecretKey(),
                jwtProperties.getUserTtl(),
                claims
        );

        AppMemberBriefVO brief = new AppMemberBriefVO();
        brief.setId(member.getId());
        brief.setNickname(member.getNickname());
        brief.setAvatar(member.getAvatar());
        brief.setPhone(member.getPhone());
        brief.setPoints(member.getPoints());
        brief.setStatus(member.getStatus());

        AppLoginVO vo = new AppLoginVO();
        vo.setToken(token);
        vo.setMember(brief);
        log.info("会员 {} 登录成功", member.getId());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String bindPhone(AppBindPhoneDTO dto) {
        Long memberId = BaseContext.getCurrentId();
        Member member = memberId == null ? null : memberMapper.selectById(memberId);
        if (member == null) {
            throw new ServiceException("登录状态无效，请重新登录");
        }
        if (dto == null || !StringUtils.hasText(dto.getCode())) {
            throw new ServiceException("请先授权手机号");
        }

        String phone = StringUtils.hasText(wechatProperties.getAppid())
                && StringUtils.hasText(wechatProperties.getSecret())
                ? fetchPhoneFromWechat(dto.getCode())
                : devFallbackPhone(dto.getCode());

        Member update = new Member();
        update.setId(member.getId());
        update.setPhone(phone);
        update.setUpdatedAt(LocalDateTime.now());
        memberMapper.updateById(update);
        log.info("会员 {} 绑定手机号 {}", member.getId(), phone);
        return phone;
    }

    /**
     * code → openid。
     *
     * 未配置 cos.wechat.appid/secret 时使用**固定**的开发 openid 兜底。
     * 注意：这里不能用 code 的哈希——开发者工具每次 wx.login 返回的 code 都不同，
     * 用哈希会导致「每刷新一次就新建一个会员」，本地测试数据（订单/积分/券）全都找不到。
     * 生产环境必须配置 appid/secret，走真实的 code2session 分支。
     */
    private String resolveOpenid(String code) {
        if (!StringUtils.hasText(wechatProperties.getAppid()) || !StringUtils.hasText(wechatProperties.getSecret())) {
            log.warn("未配置 cos.wechat.appid/secret，登录使用固定开发 openid：{}", DEV_OPENID);
            return DEV_OPENID;
        }
        String url = CODE2SESSION_URL
                + "?appid=" + wechatProperties.getAppid()
                + "&secret=" + wechatProperties.getSecret()
                + "&js_code=" + code
                + "&grant_type=authorization_code";
        try {
            JSONObject node = JSONUtil.parseObj(HttpUtil.get(url));
            String openid = node.getStr("openid");
            if (!StringUtils.hasText(openid)) {
                log.warn("code2session 失败：{}", node);
                throw new ServiceException("微信登录失败：" + node.getStr("errmsg", "未知错误"));
            }
            return openid;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用 code2session 异常", e);
            throw new ServiceException("微信登录失败，请稍后重试");
        }
    }

    /** 真实链路：getuserphonenumber（需先取 access_token） */
    private String fetchPhoneFromWechat(String code) {
        try {
            String tokenUrl = ACCESS_TOKEN_URL
                    + "?grant_type=client_credential"
                    + "&appid=" + wechatProperties.getAppid()
                    + "&secret=" + wechatProperties.getSecret();
            JSONObject tokenNode = JSONUtil.parseObj(HttpUtil.get(tokenUrl));
            String accessToken = tokenNode.getStr("access_token");
            if (!StringUtils.hasText(accessToken)) {
                log.warn("获取微信 access_token 失败：{}", tokenNode);
                throw new ServiceException("手机号解析失败，请重试");
            }
            String body = JSONUtil.createObj().set("code", code).toString();
            JSONObject node = JSONUtil.parseObj(
                    HttpUtil.post(PHONE_URL + "?access_token=" + accessToken, body)
            );
            JSONObject phoneInfo = node.getJSONObject("phone_info");
            String phone = phoneInfo == null ? null : phoneInfo.getStr("phoneNumber");
            if (!StringUtils.hasText(phone)) {
                log.warn("getuserphonenumber 失败：{}", node);
                throw new ServiceException("手机号解析失败，请重试");
            }
            return phone;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用 getuserphonenumber 异常", e);
            throw new ServiceException("手机号解析失败，请重试");
        }
    }

    /**
     * 开发兜底：未配置小程序密钥时无法解密手机号，用 code 的哈希派生一个稳定的 11 位展示号，
     * 仅为了本地联调能跑通绑定流程（生产必须配置 cos.wechat.appid/secret）。
     */
    private String devFallbackPhone(String code) {
        String hash = DigestUtil.md5Hex(code);
        long digits = Long.parseLong(hash.substring(0, 8), 16) % 100000000L;
        String phone = "138" + String.format("%08d", digits);
        log.warn("未配置 cos.wechat.appid/secret，绑手机号走开发兜底：{}", phone);
        return phone;
    }
}
