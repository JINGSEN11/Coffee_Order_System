package com.vincent.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vincent.common.exception.ServiceException;
import com.vincent.entity.Orders;
import com.vincent.entity.PayRecord;
import com.vincent.mapper.OrderMapper;
import com.vincent.mapper.PayRecordMapper;
import com.vincent.service.AppOrderService;
import com.vincent.service.AppPayService;
import com.vincent.vo.AppPayPrepayVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * C 端支付服务实现。
 * 沙箱直通模式：sys_config 未配置微信商户号/API 密钥时，预下单直接完成等价于「回调验签通过」的全部处理。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppPayServiceImpl implements AppPayService {

    /** 未配置的判定占位值（sql/02_init_data.sql 的种子值） */
    private static final String PLACEHOLDER_API_KEY = "your_api_key_here";
    private static final String CONFIG_MERCHANT_ID = "pay_merchant_id";
    private static final String CONFIG_API_KEY = "pay_api_key";

    private final OrderMapper orderMapper;
    private final PayRecordMapper payRecordMapper;
    private final AppOrderService appOrderService;
    private final AppConfigHelper appConfigHelper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppPayPrepayVO prepay(Long userId, Long orderId) {
        Orders order = orderId == null ? null : orderMapper.selectById(orderId);
        if (order == null || !Objects.equals(order.getUserId(), userId)) {
            throw new ServiceException("订单不存在");
        }
        // 已过支付截止时间的待支付订单先自动关单，再按状态拒绝支付
        appOrderService.closeIfExpired(order);
        if (order.getStatus() == null || order.getStatus() != 0) {
            throw new ServiceException("订单状态不可支付");
        }

        PayRecord payRecord = payRecordMapper.selectOne(
                new LambdaQueryWrapper<PayRecord>().eq(PayRecord::getOrderId, order.getId())
                        .orderByDesc(PayRecord::getId).last("LIMIT 1")
        );
        if (payRecord == null) {
            // out_trade_no 为幂等键，首次预下单生成后不再变更
            payRecord = new PayRecord();
            payRecord.setOrderId(order.getId());
            payRecord.setOutTradeNo(AppCalc.outTradeNo(order.getId()));
            payRecord.setAmount(AppCalc.money(order.getAmount() == null ? BigDecimal.ZERO : order.getAmount()
                    .subtract(order.getDiscountAmount() == null ? BigDecimal.ZERO : order.getDiscountAmount())
                    .max(BigDecimal.ZERO)));
            payRecord.setChannel(1);
            payRecord.setStatus(0);
            payRecord.setCreatedAt(LocalDateTime.now());
            payRecordMapper.insert(payRecord);
        }

        boolean sandbox = isSandbox();
        AppPayPrepayVO vo = new AppPayPrepayVO();
        vo.setOutTradeNo(payRecord.getOutTradeNo());
        vo.setSandbox(sandbox);

        if (!sandbox) {
            // 真实微信 JSAPI 下单需要商户证书（私钥/证书序列号/APIv3 密钥）做 V3 签名，
            // 当前 sys_config 只提供 pay_merchant_id / pay_api_key，无法完成签名，故不伪造 prepay_id。
            log.error("检测到微信商户参数，但缺少证书/签名配置，无法发起真实预支付：orderId={}", order.getId());
            throw new ServiceException("微信支付未配置完整，暂无法发起真实支付");
        }

        String transactionNo = sandboxTransactionNo();
        appOrderService.markPaid(order.getId(), transactionNo);
        vo.setPaid(true);
        vo.setPayload(sandboxPayload(payRecord.getOutTradeNo()));
        log.info("订单 {} 沙箱直通支付完成，流水号 {}", order.getOrderNo(), transactionNo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, String> notify(String body) {
        JSONObject payload = parseNotifyBody(body);
        if (payload == null) {
            return fail("无法解析回调报文");
        }
        String outTradeNo = payload.getStr("out_trade_no");
        String transactionId = payload.getStr("transaction_id");
        String tradeState = payload.getStr("trade_state");
        if (!StringUtils.hasText(outTradeNo)) {
            return fail("缺少 out_trade_no");
        }
        if (StringUtils.hasText(tradeState) && !"SUCCESS".equals(tradeState)) {
            log.warn("支付回调非成功状态：outTradeNo={}, tradeState={}", outTradeNo, tradeState);
            return success();
        }

        PayRecord payRecord = payRecordMapper.selectOne(
                new LambdaQueryWrapper<PayRecord>().eq(PayRecord::getOutTradeNo, outTradeNo).last("LIMIT 1")
        );
        if (payRecord == null) {
            log.warn("支付回调未匹配到支付流水：outTradeNo={}", outTradeNo);
            return fail("支付流水不存在");
        }
        // 幂等由 markPaid 内部的「非待支付直接返回」保证
        appOrderService.markPaid(payRecord.getOrderId(), transactionId);
        return success();
    }

    /* ---------------- 内部方法 ---------------- */

    /** sys_config 未配置商户号 / API 密钥（或仍是种子占位值）时走沙箱直通 */
    private boolean isSandbox() {
        String merchantId = appConfigHelper.stringValue(CONFIG_MERCHANT_ID, "");
        String apiKey = appConfigHelper.stringValue(CONFIG_API_KEY, "");
        return !StringUtils.hasText(merchantId)
                || !StringUtils.hasText(apiKey)
                || PLACEHOLDER_API_KEY.equals(apiKey);
    }

    /**
     * 解析回调报文。
     * 支持两种形态：明文 JSON（含 out_trade_no / transaction_id / trade_state），
     * 以及微信 V3 的 resource.ciphertext 密文（配置了 APIv3 密钥时用 AES-GCM 解密）。
     * 注意：未实现 Wechatpay-Signature 验签（需要平台证书），仅做解密与幂等处理。
     */
    private JSONObject parseNotifyBody(String body) {
        if (!StringUtils.hasText(body)) {
            return null;
        }
        try {
            JSONObject root = JSONUtil.parseObj(body);
            if (StringUtils.hasText(root.getStr("out_trade_no"))) {
                return root;
            }
            JSONObject resource = root.getJSONObject("resource");
            String ciphertext = resource == null ? null : resource.getStr("ciphertext");
            if (!StringUtils.hasText(ciphertext)) {
                return null;
            }
            String apiKey = appConfigHelper.stringValue(CONFIG_API_KEY, "");
            if (!StringUtils.hasText(apiKey) || PLACEHOLDER_API_KEY.equals(apiKey)) {
                log.error("收到加密回调但未配置 APIv3 密钥，无法解密");
                return null;
            }
            return JSONUtil.parseObj(decryptResource(ciphertext,
                    resource.getStr("nonce", ""),
                    resource.getStr("associated_data", ""),
                    apiKey));
        } catch (Exception e) {
            log.error("解析支付回调失败", e);
            return null;
        }
    }

    private String decryptResource(String ciphertext, String nonce, String associatedData, String apiKey)
            throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec key = new SecretKeySpec(apiKey.getBytes(StandardCharsets.UTF_8), "AES");
        cipher.init(Cipher.DECRYPT_MODE, key,
                new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8)));
        if (StringUtils.hasText(associatedData)) {
            cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
        }
        byte[] plain = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
        return new String(plain, StandardCharsets.UTF_8);
    }

    private String sandboxTransactionNo() {
        return "42000" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%07d", ThreadLocalRandom.current().nextInt(10000000));
    }

    private Map<String, Object> sandboxPayload(String outTradeNo) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("timeStamp", String.valueOf(System.currentTimeMillis() / 1000));
        payload.put("nonceStr", Long.toHexString(ThreadLocalRandom.current().nextLong()));
        payload.put("package", "prepay_id=sandbox_" + outTradeNo);
        payload.put("signType", "RSA");
        payload.put("paySign", "SANDBOX");
        return payload;
    }

    private Map<String, String> success() {
        Map<String, String> result = new HashMap<>();
        result.put("code", "SUCCESS");
        result.put("message", "成功");
        return result;
    }

    private Map<String, String> fail(String message) {
        Map<String, String> result = new HashMap<>();
        result.put("code", "FAIL");
        result.put("message", message);
        return result;
    }
}
