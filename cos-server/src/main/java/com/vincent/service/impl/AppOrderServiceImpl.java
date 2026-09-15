package com.vincent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.vincent.common.exception.ServiceException;
import com.vincent.dto.AppOrderCreateDTO;
import com.vincent.dto.AppReviewCreateDTO;
import com.vincent.entity.Coupon;
import com.vincent.entity.Member;
import com.vincent.entity.OrderItem;
import com.vincent.entity.Orders;
import com.vincent.entity.PayRecord;
import com.vincent.entity.PointsRecord;
import com.vincent.entity.Product;
import com.vincent.entity.RefundRecord;
import com.vincent.entity.Review;
import com.vincent.entity.Shop;
import com.vincent.entity.Sku;
import com.vincent.entity.UserCoupon;
import com.vincent.mapper.CouponMapper;
import com.vincent.mapper.MemberMapper;
import com.vincent.mapper.OrderItemMapper;
import com.vincent.mapper.OrderMapper;
import com.vincent.mapper.PayRecordMapper;
import com.vincent.mapper.PickupSeqMapper;
import com.vincent.mapper.PointsRecordMapper;
import com.vincent.mapper.ProductMapper;
import com.vincent.mapper.RefundRecordMapper;
import com.vincent.mapper.ReviewMapper;
import com.vincent.mapper.ShopMapper;
import com.vincent.mapper.SkuMapper;
import com.vincent.mapper.UserCouponMapper;
import com.vincent.service.AppCartService;
import com.vincent.service.AppOrderService;
import com.vincent.vo.AppCartAddonVO;
import com.vincent.vo.AppCartItemVO;
import com.vincent.vo.AppOrderItemVO;
import com.vincent.vo.AppOrderListVO;
import com.vincent.vo.AppOrderVO;
import com.vincent.vo.AppPayRecordVO;
import com.vincent.vo.AppRefundRecordVO;
import com.vincent.vo.AppReviewVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppOrderServiceImpl implements AppOrderService {

    /** 取餐码发号重试次数（uk_pickup 唯一键冲突时） */
    private static final int PICKUP_RETRY = 5;
    /** 评价奖励积分 */
    private static final int REVIEW_REWARD_POINTS = 10;
    private static final String CANCEL_REASON_DEFAULT = "顾客主动取消";
    private static final String CANCEL_REASON_TIMEOUT = "支付超时自动关单";
    private static final DateTimeFormatter ORDER_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final SkuMapper skuMapper;
    private final ProductMapper productMapper;
    private final ShopMapper shopMapper;
    private final MemberMapper memberMapper;
    private final PointsRecordMapper pointsRecordMapper;
    private final PayRecordMapper payRecordMapper;
    private final RefundRecordMapper refundRecordMapper;
    private final ReviewMapper reviewMapper;
    private final UserCouponMapper userCouponMapper;
    private final CouponMapper couponMapper;
    private final PickupSeqMapper pickupSeqMapper;
    private final AppCartService appCartService;
    private final AppConfigHelper appConfigHelper;
    private final AppShopResolver shopResolver;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppOrderVO create(Long userId, AppOrderCreateDTO dto) {
        if (dto == null) {
            throw new ServiceException("请先选择要下单的商品");
        }
        int type = dto.getType() == null ? 1 : dto.getType();
        if (type == 1 && !StringUtils.hasText(dto.getTableNo())) {
            throw new ServiceException("堂食请先扫码获取桌号");
        }
        if (type == 2 && !StringUtils.hasText(dto.getExpectedPickupTime())) {
            throw new ServiceException("请选择自取时段");
        }

        List<AppCartItemVO> lines = appCartService.settleLines(userId, dto.getLines());
        if (lines.isEmpty()) {
            throw new ServiceException("请先选择要下单的商品");
        }
        for (AppCartItemVO line : lines) {
            int stock = line.getStock() == null ? 0 : line.getStock();
            if (stock < line.getQty()) {
                throw new ServiceException("「" + line.getProductName() + "」库存不足");
            }
        }
        // 加料是独立分装商品，库存要单独校验：单杯份数 × 杯数
        Map<Long, Integer> addonNeed = new LinkedHashMap<>();
        Map<Long, String> addonNames = new HashMap<>();
        for (AppCartItemVO line : lines) {
            for (AppCartAddonVO addon : safeAddons(line)) {
                int need = AppCalc.qtyOf(addon.getQty()) * Math.max(1, line.getQty() == null ? 1 : line.getQty());
                addonNeed.merge(addon.getSkuId(), need, Integer::sum);
                addonNames.put(addon.getSkuId(), addon.getName());
            }
        }
        for (Map.Entry<Long, Integer> entry : addonNeed.entrySet()) {
            Sku addonSku = skuMapper.selectById(entry.getKey());
            int stock = addonSku == null || addonSku.getStock() == null ? 0 : addonSku.getStock();
            if (stock < entry.getValue()) {
                throw new ServiceException("「" + addonNames.get(entry.getKey()) + "」库存不足");
            }
        }

        Long shopId = lines.get(0).getShopId() != null ? lines.get(0).getShopId() : shopResolver.resolveId(null);
        if (shopId == null) {
            throw new ServiceException("门店不存在，请联系门店");
        }

        // 金额一律服务端重算，与小程序试算同规则
        BigDecimal goodsAmount = BigDecimal.ZERO;
        for (AppCartItemVO line : lines) {
            goodsAmount = goodsAmount.add(line.getPrice().multiply(BigDecimal.valueOf(line.getQty())));
        }
        goodsAmount = AppCalc.money(goodsAmount);

        UserCoupon userCoupon = null;
        Coupon coupon = null;
        if (dto.getUserCouponId() != null) {
            userCoupon = userCouponMapper.selectById(dto.getUserCouponId());
            if (userCoupon == null || !Objects.equals(userCoupon.getUserId(), userId)
                    || userCoupon.getStatus() == null || userCoupon.getStatus() != 0) {
                throw new ServiceException("该优惠券不可用");
            }
            coupon = couponMapper.selectById(userCoupon.getCouponId());
            LocalDateTime now = LocalDateTime.now();
            if (coupon == null || coupon.getStatus() == null || coupon.getStatus() != 1
                    || (coupon.getValidStartTime() != null && coupon.getValidStartTime().isAfter(now))
                    || (coupon.getValidEndTime() != null && coupon.getValidEndTime().isBefore(now))) {
                throw new ServiceException("该优惠券不可用");
            }
        }
        BigDecimal couponAmount = AppCalc.couponAmountOf(coupon, goodsAmount);
        if (userCoupon != null && couponAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("该优惠券未达到使用门槛");
        }

        int deductRate = appConfigHelper.intValue("points_deduct_rate", 100);
        Member member = memberMapper.selectById(userId);
        int userPoints = member == null || member.getPoints() == null ? 0 : member.getPoints();
        BigDecimal afterCoupon = goodsAmount.subtract(couponAmount).max(BigDecimal.ZERO);
        int pointsUsed = AppCalc.clampPoints(dto.getPointsUsed(), userPoints, afterCoupon, deductRate);
        BigDecimal pointsAmount = AppCalc.pointsToAmount(pointsUsed, deductRate);
        BigDecimal discountAmount = AppCalc.money(couponAmount.add(pointsAmount));
        BigDecimal payAmount = AppCalc.money(goodsAmount.subtract(discountAmount).max(BigDecimal.ZERO));

        LocalDateTime now = LocalDateTime.now();
        int timeoutMinutes = appConfigHelper.intValue("order_timeout_minutes", 15);

        Orders order = new Orders();
        order.setOrderNo(generateOrderNo(now));
        order.setUserId(userId);
        order.setShopId(shopId);
        order.setTableNo(type == 1 ? dto.getTableNo() : null);
        order.setExpectedPickupTime(type == 2 ? dto.getExpectedPickupTime() : null);
        order.setType(type);
        order.setSource(dto.getSource() == null ? 2 : dto.getSource());
        order.setAmount(goodsAmount);
        order.setDiscountAmount(discountAmount);
        order.setUserCouponId(userCoupon == null ? null : userCoupon.getId());
        order.setCouponAmount(couponAmount);
        order.setPointsUsed(pointsUsed);
        order.setPointsAmount(pointsAmount);
        order.setStatus(0);
        order.setPickupStatus(0);
        order.setRemark(StringUtils.hasText(dto.getRemark()) ? dto.getRemark() : null);
        order.setPayDeadline(now.plusMinutes(timeoutMinutes));
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        orderMapper.insert(order);

        for (AppCartItemVO line : lines) {
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setSkuId(line.getSkuId());
            item.setItemType(1);
            // 规格快照不带加料：加料是独立明细行，避免展示时重复
            item.setProductName(line.getProductName());
            item.setSpecs(AppCalc.combineText(line.getSpecs(), line.getOptions()));
            // 主行只记 SKU 基础价：加料另有独立行，用含加料单价会把加料算两次
            item.setPrice(line.getBasePrice() != null ? line.getBasePrice() : line.getPrice());
            item.setQty(line.getQty());
            orderItemMapper.insert(item);

            // 加料展开为独立明细行：自己扣库存、自己算销量，parent_item_id 指回主商品行。
            // 份数随杯数放大（买 2 杯加 1 份燕麦奶 = 2 份）。
            for (AppCartAddonVO addon : safeAddons(line)) {
                OrderItem addonItem = new OrderItem();
                addonItem.setOrderId(order.getId());
                addonItem.setSkuId(addon.getSkuId());
                addonItem.setItemType(2);
                addonItem.setParentItemId(item.getId());
                addonItem.setProductName(addon.getName());
                addonItem.setSpecs("加料");
                addonItem.setPrice(addon.getPrice());
                addonItem.setQty(AppCalc.qtyOf(addon.getQty()) * Math.max(1, line.getQty() == null ? 1 : line.getQty()));
                orderItemMapper.insert(addonItem);
            }
        }

        // 下单即锁定券；不扣库存、不发取餐码。
        // 积分这里只做上限校验并落 order.points_used，实际扣减与 points_record(type=2)
        // 在支付成功时完成，取消/超时关单时按是否已扣减决定是否退回。
        if (userCoupon != null) {
            userCouponMapper.update(null, new LambdaUpdateWrapper<UserCoupon>()
                    .eq(UserCoupon::getId, userCoupon.getId())
                    .set(UserCoupon::getStatus, 1)
                    .set(UserCoupon::getOrderId, order.getId())
                    .set(UserCoupon::getUsedAt, now));
        }
        log.info("会员 {} 下单成功：订单 {}，金额 {}，实付 {}", userId, order.getOrderNo(), goodsAmount, payAmount);
        return buildOrderVO(orderMapper.selectById(order.getId()));
    }

    @Override
    public AppOrderListVO list(Long userId, String status) {
        List<Orders> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Orders>()
                        .eq(Orders::getUserId, userId)
                        .orderByDesc(Orders::getCreatedAt)
        );
        Map<String, Long> counts = new LinkedHashMap<>();
        for (int i = 0; i <= 7; i++) {
            counts.put(String.valueOf(i), 0L);
        }
        for (Orders order : orders) {
            if (order.getStatus() != null) {
                counts.merge(String.valueOf(order.getStatus()), 1L, Long::sum);
            }
        }

        List<Orders> filtered = orders;
        if (StringUtils.hasText(status) && !"all".equalsIgnoreCase(status.trim())) {
            List<Integer> wanted = new ArrayList<>();
            for (String part : status.split(",")) {
                String trimmed = part.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                try {
                    wanted.add(Integer.valueOf(trimmed));
                } catch (NumberFormatException e) {
                    throw new ServiceException("订单状态参数不合法");
                }
            }
            filtered = orders.stream()
                    .filter(o -> o.getStatus() != null && wanted.contains(o.getStatus()))
                    .collect(Collectors.toList());
        }

        AppOrderListVO vo = new AppOrderListVO();
        vo.setTotal((long) filtered.size());
        vo.setCounts(counts);
        vo.setList(filtered.stream().map(this::buildOrderVO).collect(Collectors.toList()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppOrderVO detail(Long userId, String idOrNo) {
        Orders order = resolveOwned(userId, idOrNo);
        closeIfExpired(order);
        return buildOrderVO(orderMapper.selectById(order.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppOrderVO cancel(Long userId, String idOrNo, String reason) {
        Orders order = resolveOwned(userId, idOrNo);
        Integer currentStatus = order.getStatus();
        if (currentStatus == null || (currentStatus != 0 && currentStatus != 1)) {
            throw new ServiceException("该状态下不可取消，请联系门店");
        }
        boolean wasPaid = currentStatus == 1;
        LocalDateTime now = LocalDateTime.now();
        String finalReason = StringUtils.hasText(reason) ? reason : CANCEL_REASON_DEFAULT;

        order.setStatus(wasPaid ? 7 : 4);
        order.setCancelTime(now);
        order.setCancelReason(finalReason);
        // 已发号的订单作废取餐码，号不回收
        order.setPickupStatus(order.getPickupNo() != null ? 5 : 0);
        order.setUpdatedAt(now);
        orderMapper.updateById(order);

        if (wasPaid) {
            RefundRecord refund = new RefundRecord();
            refund.setOrderId(order.getId());
            refund.setRefundNo("RF" + now.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                    + String.format("%05d", order.getId()));
            refund.setAmount(payAmountOf(order));
            refund.setReason(finalReason);
            refund.setStatus(1);
            refund.setOperator("系统自动");
            refund.setCallbackTime(now);
            refund.setCreatedAt(now);
            refundRecordMapper.insert(refund);
        }

        releaseCoupon(order);
        if (wasPaid) {
            refundPoints(order, now);
            restoreStock(order);
        }
        log.info("会员 {} 取消订单 {}，原因：{}", userId, order.getOrderNo(), finalReason);
        return buildOrderVO(orderMapper.selectById(order.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> again(Long userId, String idOrNo) {
        Orders order = resolveOwned(userId, idOrNo);
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId())
        );
        int added = appCartService.addBack(userId, order.getShopId(), items);

        Map<String, Object> result = new HashMap<>();
        result.put("added", added);
        result.put("cart", appCartService.buildCart(userId));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> review(Long userId, String idOrNo, AppReviewCreateDTO dto) {
        Orders order = resolveOwned(userId, idOrNo);
        if (order.getStatus() == null || order.getStatus() != 3) {
            throw new ServiceException("订单完成后才可评价");
        }
        if (reviewMapper.selectCount(
                new LambdaQueryWrapper<Review>().eq(Review::getOrderId, order.getId())) > 0) {
            throw new ServiceException("该订单已评价过啦");
        }

        int score = dto == null || dto.getScore() == null ? 5 : Math.max(1, Math.min(5, dto.getScore()));
        String content = joinReviewContent(dto);
        LocalDateTime now = LocalDateTime.now();

        Review review = new Review();
        review.setOrderId(order.getId());
        review.setUserId(userId);
        review.setScore(score);
        review.setContent(content);
        review.setImages("[]");
        // review 表无 tags / anonymous 列：tags 以「、」前缀并入 content，anonymous 仅记录日志
        review.setStatus(1);
        review.setCreatedAt(now);
        review.setUpdatedAt(now);
        reviewMapper.insert(review);
        if (dto != null && Boolean.TRUE.equals(dto.getAnonymous())) {
            log.info("订单 {} 为匿名评价", order.getId());
        }

        Member member = memberMapper.selectById(userId);
        if (member != null) {
            Member update = new Member();
            update.setId(member.getId());
            update.setPoints((member.getPoints() == null ? 0 : member.getPoints()) + REVIEW_REWARD_POINTS);
            update.setUpdatedAt(now);
            memberMapper.updateById(update);
        }
        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setChangeValue(REVIEW_REWARD_POINTS);
        record.setType(1);
        record.setOrderId(order.getId());
        record.setCreatedAt(now);
        pointsRecordMapper.insert(record);

        Map<String, Object> result = new HashMap<>();
        result.put("reviewed", true);
        result.put("points_gained", REVIEW_REWARD_POINTS);
        return result;
    }

    @Override
    public AppOrderVO buildOrderVO(Orders order) {
        if (order == null) {
            return null;
        }
        AppOrderVO vo = new AppOrderVO();
        BeanUtils.copyProperties(order, vo);
        vo.setPayAmount(payAmountOf(order));
        if (vo.getPickupCode() == null && order.getPickupNo() != null) {
            vo.setPickupCode(AppSpec.pickupCodeOf(order.getPickupNo()));
        }

        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId())
        );
        Map<Long, Sku> skus = skusById(items.stream().map(OrderItem::getSkuId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList()));
        Map<Long, Product> products = productsById(skus.values().stream().map(Sku::getProductId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList()));

        // 加料在库里是独立明细行（item_type=2），展示时归并到主商品行的 addons，
        // 并且**不计入 totalQty**（杯数只算主商品，否则买 1 杯加 1 份料会显示 2 件）
        int totalQty = 0;
        List<AppOrderItemVO> itemVOs = new ArrayList<>();
        Map<Long, AppOrderItemVO> voById = new HashMap<>();
        for (OrderItem item : items) {
            if (isAddonItem(item)) {
                continue;
            }
            totalQty += item.getQty() == null ? 0 : item.getQty();
            AppOrderItemVO itemVO = new AppOrderItemVO();
            BeanUtils.copyProperties(item, itemVO);
            itemVO.setAddons(new ArrayList<>());
            Sku sku = skus.get(item.getSkuId());
            if (sku != null) {
                itemVO.setProductId(sku.getProductId());
                Product product = products.get(sku.getProductId());
                if (product != null) {
                    itemVO.setCategoryId(product.getCategoryId());
                }
            }
            itemVOs.add(itemVO);
            voById.put(item.getId(), itemVO);
        }
        for (OrderItem item : items) {
            if (!isAddonItem(item)) {
                continue;
            }
            AppCartAddonVO addonVO = new AppCartAddonVO();
            addonVO.setSkuId(item.getSkuId());
            addonVO.setName(item.getProductName());
            addonVO.setPrice(item.getPrice());
            addonVO.setQty(item.getQty());
            Sku addonSku = skus.get(item.getSkuId());
            if (addonSku != null) {
                addonVO.setProductId(addonSku.getProductId());
            }
            AppOrderItemVO parent = voById.get(item.getParentItemId());
            if (parent != null) {
                parent.getAddons().add(addonVO);
            } else {
                // 找不到主行（理论上不会发生）时，退化成独立行展示，避免丢信息
                AppOrderItemVO orphan = new AppOrderItemVO();
                orphan.setAddons(List.of(addonVO));
                itemVOs.add(orphan);
            }
        }
        vo.setItems(itemVOs);
        vo.setTotalQty(totalQty);

        Review review = reviewMapper.selectOne(
                new LambdaQueryWrapper<Review>().eq(Review::getOrderId, order.getId())
                        .orderByDesc(Review::getId).last("LIMIT 1")
        );
        if (review != null) {
            AppReviewVO reviewVO = new AppReviewVO();
            reviewVO.setId(review.getId());
            reviewVO.setScore(review.getScore());
            reviewVO.setContent(review.getContent());
            reviewVO.setImages(AppCalc.parseImages(review.getImages()));
            reviewVO.setCreatedAt(review.getCreatedAt());
            vo.setReview(reviewVO);
        }

        PayRecord payRecord = payRecordMapper.selectOne(
                new LambdaQueryWrapper<PayRecord>().eq(PayRecord::getOrderId, order.getId())
                        .orderByDesc(PayRecord::getId).last("LIMIT 1")
        );
        if (payRecord != null) {
            AppPayRecordVO payVO = new AppPayRecordVO();
            BeanUtils.copyProperties(payRecord, payVO);
            vo.setPayRecord(payVO);
        }

        RefundRecord refundRecord = refundRecordMapper.selectOne(
                new LambdaQueryWrapper<RefundRecord>().eq(RefundRecord::getOrderId, order.getId())
                        .orderByDesc(RefundRecord::getId).last("LIMIT 1")
        );
        if (refundRecord != null) {
            AppRefundRecordVO refundVO = new AppRefundRecordVO();
            BeanUtils.copyProperties(refundRecord, refundVO);
            vo.setRefundRecord(refundVO);
        }

        Shop shop = order.getShopId() == null ? null : shopMapper.selectById(order.getShopId());
        if (shop != null) {
            Map<String, Object> shopMap = new LinkedHashMap<>();
            shopMap.put("id", shop.getId());
            shopMap.put("name", shop.getName());
            shopMap.put("address", shop.getAddress());
            vo.setShop(shopMap);
        }
        return vo;
    }

    @Override
    public List<AppOrderVO> recentOngoing(Long userId, int limit) {
        if (userId == null) {
            return new ArrayList<>();
        }
        List<Orders> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Orders>()
                        .eq(Orders::getUserId, userId)
                        .in(Orders::getStatus, List.of(0, 1, 2, 3))
                        .orderByDesc(Orders::getCreatedAt)
                        .last("LIMIT " + limit)
        );
        return orders.stream().map(this::buildOrderVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Orders closeIfExpired(Orders order) {
        if (order == null || order.getStatus() == null || order.getStatus() != 0) {
            return order;
        }
        if (order.getPayDeadline() == null || !order.getPayDeadline().isBefore(LocalDateTime.now())) {
            return order;
        }
        order.setStatus(4);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason(CANCEL_REASON_TIMEOUT);
        order.setPickupStatus(0);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        // 超时关单同时释放锁定的券，避免券被永久占用
        releaseCoupon(order);
        log.info("订单 {} 支付超时自动关单", order.getOrderNo());
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markPaid(Long orderId, String transactionNo) {
        Orders order = orderId == null ? null : orderMapper.selectById(orderId);
        if (order == null) {
            log.warn("支付成功处理时订单不存在：orderId={}", orderId);
            return;
        }
        if (order.getStatus() == null || order.getStatus() != 0) {
            // 幂等：重复回调直接返回，不重复发号/扣库存
            log.info("订单 {} 当前状态 {} 非待支付，忽略本次支付成功处理", order.getOrderNo(), order.getStatus());
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        BigDecimal payAmount = payAmountOf(order);

        PayRecord payRecord = payRecordMapper.selectOne(
                new LambdaQueryWrapper<PayRecord>().eq(PayRecord::getOrderId, order.getId())
                        .orderByDesc(PayRecord::getId).last("LIMIT 1")
        );
        if (payRecord == null) {
            PayRecord created = new PayRecord();
            created.setOrderId(order.getId());
            created.setOutTradeNo(AppCalc.outTradeNo(order.getId()));
            created.setTransactionNo(transactionNo);
            created.setAmount(payAmount);
            created.setChannel(1);
            created.setStatus(1);
            created.setCallbackTime(now);
            created.setCreatedAt(now);
            payRecordMapper.insert(created);
        } else {
            payRecordMapper.update(null, new LambdaUpdateWrapper<PayRecord>()
                    .eq(PayRecord::getId, payRecord.getId())
                    .set(PayRecord::getStatus, 1)
                    .set(PayRecord::getTransactionNo, transactionNo)
                    .set(PayRecord::getCallbackTime, now));
        }

        // 发取餐码：门店 + 自然日从 1 递增，配合 pickup_seq 水位与 uk_pickup 唯一键重试
        LocalDate bizDate = LocalDate.now();
        for (int attempt = 0; attempt < PICKUP_RETRY; attempt++) {
            int pickupNo = nextPickupNo(order.getShopId(), bizDate);
            order.setStatus(1);
            order.setPayTime(now);
            order.setPickupNo(pickupNo);
            order.setPickupDate(bizDate);
            order.setPickupCode(AppSpec.pickupCodeOf(pickupNo));
            order.setPickupStatus(1);
            order.setUpdatedAt(now);
            try {
                orderMapper.updateById(order);
                break;
            } catch (DuplicateKeyException e) {
                log.warn("取餐码发号冲突，重试第 {} 次：shopId={}, bizDate={}, pickupNo={}",
                        attempt + 1, order.getShopId(), bizDate, pickupNo);
                if (attempt == PICKUP_RETRY - 1) {
                    throw e;
                }
            }
        }

        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId())
        );
        deductStock(items);
        appCartService.removeOrdered(order.getUserId(), order.getShopId(),
                items.stream().map(OrderItem::getSkuId).filter(Objects::nonNull).distinct()
                        .collect(Collectors.toList()));
        consumePoints(order, now);
        log.info("订单 {} 支付成功，取餐码 {}，实付 {}", order.getOrderNo(), order.getPickupCode(), payAmount);
    }

    /* ---------------- 内部方法 ---------------- */

    /** 明细是否为加料行（独立分装小料） */
    private boolean isAddonItem(OrderItem item) {
        return item != null && item.getItemType() != null && item.getItemType() == 2;
    }

    /** 购物车行的加料列表，null 安全 */
    private List<AppCartAddonVO> safeAddons(AppCartItemVO line) {
        return line.getAddons() == null ? new ArrayList<>() : line.getAddons();
    }

    private int nextPickupNo(Long shopId, LocalDate bizDate) {
        pickupSeqMapper.upsertIncrement(shopId, bizDate);
        Integer current = pickupSeqMapper.selectCurrentNo(shopId, bizDate);
        return current == null ? 1 : current;
    }

    private void deductStock(List<OrderItem> items) {
        for (OrderItem item : items) {
            Sku sku = item.getSkuId() == null ? null : skuMapper.selectById(item.getSkuId());
            if (sku == null) {
                continue;
            }
            int stock = sku.getStock() == null ? 0 : sku.getStock();
            int qty = item.getQty() == null ? 0 : item.getQty();
            Sku update = new Sku();
            update.setId(sku.getId());
            update.setStock(Math.max(0, stock - qty));
            skuMapper.updateById(update);
        }
    }

    private void restoreStock(Orders order) {
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId())
        );
        for (OrderItem item : items) {
            Sku sku = item.getSkuId() == null ? null : skuMapper.selectById(item.getSkuId());
            if (sku == null) {
                continue;
            }
            Sku update = new Sku();
            update.setId(sku.getId());
            update.setStock((sku.getStock() == null ? 0 : sku.getStock())
                    + (item.getQty() == null ? 0 : item.getQty()));
            skuMapper.updateById(update);
        }
    }

    /** 支付成功时扣减积分并记流水 type=2 抵扣消耗 */
    private void consumePoints(Orders order, LocalDateTime now) {
        int used = order.getPointsUsed() == null ? 0 : order.getPointsUsed();
        if (used <= 0) {
            return;
        }
        Member member = memberMapper.selectById(order.getUserId());
        if (member != null) {
            Member update = new Member();
            update.setId(member.getId());
            update.setPoints(Math.max(0, (member.getPoints() == null ? 0 : member.getPoints()) - used));
            update.setUpdatedAt(now);
            memberMapper.updateById(update);
        }
        PointsRecord record = new PointsRecord();
        record.setUserId(order.getUserId());
        record.setChangeValue(-used);
        record.setType(2);
        record.setOrderId(order.getId());
        record.setCreatedAt(now);
        pointsRecordMapper.insert(record);
    }

    /** 退款/取消时退回积分并记流水 type=3 退款退回 */
    private void refundPoints(Orders order, LocalDateTime now) {
        int used = order.getPointsUsed() == null ? 0 : order.getPointsUsed();
        if (used <= 0) {
            return;
        }
        Member member = memberMapper.selectById(order.getUserId());
        if (member != null) {
            Member update = new Member();
            update.setId(member.getId());
            update.setPoints((member.getPoints() == null ? 0 : member.getPoints()) + used);
            update.setUpdatedAt(now);
            memberMapper.updateById(update);
        }
        PointsRecord record = new PointsRecord();
        record.setUserId(order.getUserId());
        record.setChangeValue(used);
        record.setType(3);
        record.setOrderId(order.getId());
        record.setCreatedAt(now);
        pointsRecordMapper.insert(record);
    }

    /** 释放订单锁定的持券 */
    private void releaseCoupon(Orders order) {
        if (order.getUserCouponId() == null) {
            return;
        }
        // order_id / used_at 需要显式置 NULL，updateById 无法写空值，故用 setSql
        userCouponMapper.update(null, new LambdaUpdateWrapper<UserCoupon>()
                .eq(UserCoupon::getId, order.getUserCouponId())
                .set(UserCoupon::getStatus, 0)
                .setSql("order_id = NULL, used_at = NULL"));
    }

    private Orders resolveOwned(Long userId, String idOrNo) {
        if (!StringUtils.hasText(idOrNo)) {
            throw new ServiceException("订单不存在");
        }
        String key = idOrNo.trim();
        Orders order = null;
        try {
            order = orderMapper.selectById(Long.valueOf(key));
        } catch (NumberFormatException ignored) {
            // 非数字则按订单号查询
        }
        if (order == null) {
            order = orderMapper.selectOne(
                    new LambdaQueryWrapper<Orders>().eq(Orders::getOrderNo, key).last("LIMIT 1")
            );
        }
        if (order == null || !Objects.equals(order.getUserId(), userId)) {
            throw new ServiceException("订单不存在");
        }
        return order;
    }

    private BigDecimal payAmountOf(Orders order) {
        BigDecimal amount = order.getAmount() == null ? BigDecimal.ZERO : order.getAmount();
        BigDecimal discount = order.getDiscountAmount() == null ? BigDecimal.ZERO : order.getDiscountAmount();
        return AppCalc.money(amount.subtract(discount).max(BigDecimal.ZERO));
    }

    /** 评价内容：tags 以「、」拼接后与正文用全角空格分隔（对齐既有小程序实现） */
    private String joinReviewContent(AppReviewCreateDTO dto) {
        if (dto == null) {
            return null;
        }
        String tags = dto.getTags() == null ? "" : dto.getTags().stream()
                .filter(StringUtils::hasText).collect(Collectors.joining("、"));
        String content = dto.getContent() == null ? "" : dto.getContent().trim();
        if (tags.isEmpty()) {
            return content.isEmpty() ? null : content;
        }
        return content.isEmpty() ? tags : tags + "　" + content;
    }

    private String generateOrderNo(LocalDateTime now) {
        return "CO" + now.format(ORDER_NO_FORMAT)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private Map<Long, Sku> skusById(List<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return new HashMap<>();
        }
        return skuMapper.selectList(new LambdaQueryWrapper<Sku>().in(Sku::getId, skuIds))
                .stream().collect(Collectors.toMap(Sku::getId, s -> s, (a, b) -> a));
    }

    private Map<Long, Product> productsById(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return new HashMap<>();
        }
        return productMapper.selectList(new LambdaQueryWrapper<Product>().in(Product::getId, productIds))
                .stream().collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));
    }
}
