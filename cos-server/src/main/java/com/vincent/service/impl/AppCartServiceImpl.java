package com.vincent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vincent.common.exception.ServiceException;
import com.vincent.dto.AppAddonDTO;
import com.vincent.dto.AppCartAddDTO;
import com.vincent.dto.AppCartUpdateDTO;
import com.vincent.entity.CartItem;
import com.vincent.entity.Category;
import com.vincent.entity.OrderItem;
import com.vincent.entity.Product;
import com.vincent.entity.ProductAddon;
import com.vincent.entity.Sku;
import com.vincent.mapper.CartItemMapper;
import com.vincent.mapper.CategoryMapper;
import com.vincent.mapper.ProductAddonMapper;
import com.vincent.mapper.ProductMapper;
import com.vincent.mapper.SkuMapper;
import com.vincent.service.AppCartService;
import com.vincent.vo.AppCartAddonVO;
import com.vincent.vo.AppCartItemVO;
import com.vincent.vo.AppCartVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppCartServiceImpl implements AppCartService {

    /**
     * cart_item 表没有 checked 列（sql/04_migrate_app_fields.sql 只新增了 addons / addons_hash），
     * 而契约 PUT /api/app/cart/{id} 支持 { "checked": false }，故勾选态落在 Redis 的「未勾选行 ID 集合」，
     * 默认全部勾选；Redis 不可用时降级为「全部勾选」，不影响结算。
     */
    private static final String UNCHECKED_KEY_PREFIX = "cos:app:cart:unchecked:";

    private final CartItemMapper cartItemMapper;
    private final SkuMapper skuMapper;
    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final ProductAddonMapper productAddonMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final AppShopResolver shopResolver;

    @Override
    public AppCartVO buildCart(Long userId) {
        if (userId == null) {
            return emptyCart();
        }
        List<CartItem> rows = cartItemMapper.selectList(
                new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, userId).orderByAsc(CartItem::getId)
        );
        return assemble(userId, toVOs(rows));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppCartVO add(Long userId, AppCartAddDTO dto) {
        if (dto == null || dto.getSkuId() == null) {
            throw new ServiceException("规格不存在");
        }
        Sku sku = skuMapper.selectById(dto.getSkuId());
        if (sku == null) {
            throw new ServiceException("规格不存在");
        }
        if (sku.getStock() == null || sku.getStock() <= 0) {
            throw new ServiceException("该规格已售罄");
        }
        Long shopId = shopResolver.resolveId(dto.getShopId());
        if (shopId == null) {
            throw new ServiceException("门店不存在，请联系门店");
        }

        List<AppAddonDTO> addons = normalizeAddons(sku, dto.getAddons());
        String addonsHash = AppCalc.addonsHash(addons);
        Map<String, Object> options = dto.getOptions() == null ? new LinkedHashMap<>() : dto.getOptions();
        String optionsHash = AppCalc.optionsHash(options);
        int maxQty = Math.max(1, sku.getStock());
        int qty = Math.max(1, Math.min(dto.getQty() == null ? 1 : dto.getQty(), maxQty));

        CartItem exist = cartItemMapper.selectOne(
                new LambdaQueryWrapper<CartItem>()
                        .eq(CartItem::getUserId, userId)
                        .eq(CartItem::getSkuId, sku.getId())
                        .eq(CartItem::getShopId, shopId)
                        .eq(CartItem::getOptionsHash, optionsHash)
                        .eq(CartItem::getAddonsHash, addonsHash)
                        .last("LIMIT 1")
        );
        if (exist != null) {
            exist.setQty(Math.min(exist.getQty() + qty, maxQty));
            exist.setUpdatedAt(LocalDateTime.now());
            cartItemMapper.updateById(exist);
        } else {
            CartItem row = new CartItem();
            row.setUserId(userId);
            row.setSkuId(sku.getId());
            row.setQty(qty);
            row.setOptions(AppCalc.writeOptions(options));
            row.setOptionsHash(optionsHash);
            row.setAddons(AppCalc.writeAddons(addons));
            row.setAddonsHash(addonsHash);
            row.setShopId(shopId);
            row.setCreatedAt(LocalDateTime.now());
            row.setUpdatedAt(LocalDateTime.now());
            cartItemMapper.insert(row);
        }
        return buildCart(userId);
    }

    /**
     * 加料规整：只接受「该商品确实可加、且加料本身有库存」的加料 SKU。
     * 名称与价格一律服务端按加料 SKU 取，绝不信任前端传值（防价格欺骗）。
     * 份数按 product_addon.max_qty 与加料库存双重夹取，同一加料合并为一项。
     */
    private List<AppAddonDTO> normalizeAddons(Sku mainSku, List<AppAddonDTO> raw) {
        if (raw == null || raw.isEmpty()) {
            return new ArrayList<>();
        }
        List<ProductAddon> links = productAddonMapper.selectList(
                new LambdaQueryWrapper<ProductAddon>()
                        .eq(ProductAddon::getProductId, mainSku.getProductId())
                        .eq(ProductAddon::getStatus, 1)
        );
        if (links.isEmpty()) {
            throw new ServiceException("该商品不支持加料");
        }
        Map<Long, ProductAddon> linkByAddonProduct = links.stream()
                .filter(l -> l.getAddonProductId() != null)
                .collect(Collectors.toMap(ProductAddon::getAddonProductId, l -> l, (a, b) -> a));

        Map<Long, Integer> merged = new LinkedHashMap<>();
        for (AppAddonDTO item : raw) {
            if (item == null || item.getSkuId() == null) {
                continue;
            }
            Sku addonSku = skuMapper.selectById(item.getSkuId());
            if (addonSku == null) {
                throw new ServiceException("所选加料不存在");
            }
            ProductAddon link = linkByAddonProduct.get(addonSku.getProductId());
            if (link == null) {
                throw new ServiceException("该商品不支持此加料");
            }
            int stock = addonSku.getStock() == null ? 0 : addonSku.getStock();
            if (stock <= 0) {
                throw new ServiceException("加料已售罄");
            }
            int max = link.getMaxQty() == null ? 1 : Math.max(1, link.getMaxQty());
            int qty = Math.min(AppCalc.qtyOf(item.getQty()), Math.min(max, stock));
            merged.merge(addonSku.getId(), qty, (a, b) -> Math.min(a + b, Math.min(max, stock)));
        }
        List<AppAddonDTO> result = new ArrayList<>();
        merged.forEach((skuId, qty) -> {
            AppAddonDTO dto = new AppAddonDTO();
            dto.setSkuId(skuId);
            dto.setQty(qty);
            result.add(dto);
        });
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppCartVO update(Long userId, Long lineId, AppCartUpdateDTO dto) {
        CartItem row = requireOwnedLine(userId, lineId);
        if (dto != null && dto.getQty() != null) {
            Sku sku = skuMapper.selectById(row.getSkuId());
            int max = sku == null || sku.getStock() == null ? 99 : Math.max(1, sku.getStock());
            row.setQty(Math.max(1, Math.min(dto.getQty(), max)));
        }
        if (dto != null && dto.getChecked() != null) {
            markChecked(userId, row.getId(), dto.getChecked());
        }
        row.setUpdatedAt(LocalDateTime.now());
        cartItemMapper.updateById(row);
        return buildCart(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppCartVO delete(Long userId, Long lineId) {
        if (lineId == null) {
            // 不传 id 表示清空当前用户购物车（切门店时使用）
            cartItemMapper.delete(new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, userId));
            clearUnchecked(userId);
        } else {
            CartItem row = requireOwnedLine(userId, lineId);
            cartItemMapper.deleteById(row.getId());
            markChecked(userId, row.getId(), true);
        }
        return buildCart(userId);
    }

    @Override
    public List<AppCartItemVO> settleLines(Long userId, List<Long> lineIds) {
        if (userId == null) {
            return new ArrayList<>();
        }
        List<CartItem> rows;
        if (lineIds != null) {
            if (lineIds.isEmpty()) {
                return new ArrayList<>();
            }
            rows = cartItemMapper.selectList(
                    new LambdaQueryWrapper<CartItem>()
                            .eq(CartItem::getUserId, userId)
                            .in(CartItem::getId, lineIds)
            );
        } else {
            Set<Long> unchecked = uncheckedIds(userId);
            rows = cartItemMapper.selectList(
                    new LambdaQueryWrapper<CartItem>()
                            .eq(CartItem::getUserId, userId)
                            .orderByAsc(CartItem::getId)
            ).stream().filter(r -> !unchecked.contains(r.getId())).collect(Collectors.toList());
        }
        return toVOs(rows);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int addBack(Long userId, Long shopId, List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        // 加料行（item_type=2）不是独立的购物车行，按 parent_item_id 归回主商品行的 addons
        Map<Long, List<OrderItem>> addonsByParent = new HashMap<>();
        List<OrderItem> mains = new ArrayList<>();
        for (OrderItem item : items) {
            if (isAddonItem(item)) {
                if (item.getParentItemId() != null) {
                    addonsByParent.computeIfAbsent(item.getParentItemId(), k -> new ArrayList<>()).add(item);
                }
            } else {
                mains.add(item);
            }
        }

        Long targetShopId = shopId;
        int added = 0;
        for (OrderItem item : mains) {
            Sku sku = item.getSkuId() == null ? null : skuMapper.selectById(item.getSkuId());
            if (sku == null || sku.getStock() == null || sku.getStock() <= 0) {
                continue;
            }
            Product product = sku.getProductId() == null ? null : productMapper.selectById(sku.getProductId());
            if (product == null || product.getStatus() == null || product.getStatus() != 1) {
                // 已下架或售罄的 SKU 跳过
                continue;
            }
            // 加料还原：只带回「该商品仍支持、且加料当前有库存」的部分；
            // 注：温度/糖度选项未在订单明细里留存快照，回滚后置空（下单时可重新选）。
            List<AppAddonDTO> addons = restoreAddons(sku, addonsByParent.get(item.getId()));
            String addonsHash = AppCalc.addonsHash(addons);
            int maxQty = Math.max(1, sku.getStock());
            int qty = Math.max(1, Math.min(item.getQty() == null ? 1 : item.getQty(), maxQty));
            CartItem exist = cartItemMapper.selectOne(
                    new LambdaQueryWrapper<CartItem>()
                            .eq(CartItem::getUserId, userId)
                            .eq(CartItem::getSkuId, sku.getId())
                            .eq(CartItem::getShopId, targetShopId)
                            .eq(CartItem::getOptionsHash, "")
                            .eq(CartItem::getAddonsHash, addonsHash)
                            .last("LIMIT 1")
            );
            if (exist != null) {
                exist.setQty(Math.min(exist.getQty() + qty, maxQty));
                exist.setUpdatedAt(LocalDateTime.now());
                cartItemMapper.updateById(exist);
            } else {
                CartItem row = new CartItem();
                row.setUserId(userId);
                row.setSkuId(sku.getId());
                row.setQty(qty);
                row.setOptionsHash("");
                row.setAddons(AppCalc.writeAddons(addons));
                row.setAddonsHash(addonsHash);
                row.setShopId(targetShopId);
                row.setCreatedAt(LocalDateTime.now());
                row.setUpdatedAt(LocalDateTime.now());
                cartItemMapper.insert(row);
            }
            added++;
        }
        return added;
    }

    /** 是否为加料明细行 */
    private boolean isAddonItem(OrderItem item) {
        return item != null && item.getItemType() != null && item.getItemType() == 2;
    }

    /** 取消订单回购物车时，把加料行还原成该商品允许的加料项 */
    private List<AppAddonDTO> restoreAddons(Sku mainSku, List<OrderItem> addonItems) {
        if (addonItems == null || addonItems.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> skuIds = addonItems.stream().map(OrderItem::getSkuId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        if (skuIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<AppAddonDTO> raw = new ArrayList<>();
        for (OrderItem item : addonItems) {
            AppAddonDTO dto = new AppAddonDTO();
            dto.setSkuId(item.getSkuId());
            dto.setQty(item.getQty());
            raw.add(dto);
        }
        try {
            return normalizeAddons(mainSku, raw);
        } catch (ServiceException e) {
            // 商品已不再支持该加料 / 加料已售罄：静默丢弃，不能让回滚整体失败
            log.info("回滚加料失败，已忽略：{}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeOrdered(Long userId, Long shopId, List<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<CartItem> wrapper = new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .in(CartItem::getSkuId, skuIds);
        if (shopId != null) {
            wrapper.eq(CartItem::getShopId, shopId);
        }
        cartItemMapper.delete(wrapper);
    }

    /* ---------------- 内部方法 ---------------- */

    /** 行装配：拼接 SKU / 商品 / 分类信息，勾选态取自 Redis */
    private List<AppCartItemVO> toVOs(List<CartItem> rows) {
        if (rows == null || rows.isEmpty()) {
            return new ArrayList<>();
        }
        Long userId = rows.get(0).getUserId();
        Set<Long> unchecked = uncheckedIds(userId);

        List<Long> skuIds = rows.stream().map(CartItem::getSkuId).filter(Objects::nonNull).distinct()
                .collect(Collectors.toList());
        if (skuIds.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, Sku> skus = skuMapper.selectList(new LambdaQueryWrapper<Sku>().in(Sku::getId, skuIds))
                .stream().collect(Collectors.toMap(Sku::getId, s -> s, (a, b) -> a));
        List<Long> productIds = skus.values().stream().map(Sku::getProductId).filter(Objects::nonNull).distinct()
                .collect(Collectors.toList());
        if (productIds.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, Product> products = productMapper.selectList(
                        new LambdaQueryWrapper<Product>().in(Product::getId, productIds)
                ).stream().collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));
        Map<Long, String> categoryNames = new HashMap<>();
        List<Long> categoryIds = products.values().stream().map(Product::getCategoryId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        if (!categoryIds.isEmpty()) {
            categoryNames = categoryMapper.selectList(
                            new LambdaQueryWrapper<Category>().in(Category::getId, categoryIds)
                    ).stream().collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
        }

        // 加料是独立分装商品：先把所有行引用到的加料 SKU / 商品 / 关联覆盖价一次查出来
        Map<Long, List<AppAddonDTO>> addonsByRow = new HashMap<>();
        Set<Long> addonSkuIds = new HashSet<>();
        for (CartItem row : rows) {
            List<AppAddonDTO> addons = AppCalc.parseAddons(row.getAddons());
            addonsByRow.put(row.getId(), addons);
            addons.forEach(a -> addonSkuIds.add(a.getSkuId()));
        }
        Map<Long, Sku> addonSkus = addonSkuIds.isEmpty() ? new HashMap<>()
                : skuMapper.selectList(new LambdaQueryWrapper<Sku>().in(Sku::getId, addonSkuIds))
                .stream().collect(Collectors.toMap(Sku::getId, s -> s, (a, b) -> a));
        List<Long> addonProductIds = addonSkus.values().stream().map(Sku::getProductId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, Product> addonProducts = addonProductIds.isEmpty() ? new HashMap<>()
                : productMapper.selectList(new LambdaQueryWrapper<Product>().in(Product::getId, addonProductIds))
                .stream().collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));
        Map<String, BigDecimal> addonOverridePrices = new HashMap<>();
        if (!addonProductIds.isEmpty() && !productIds.isEmpty()) {
            for (ProductAddon link : productAddonMapper.selectList(new LambdaQueryWrapper<ProductAddon>()
                    .in(ProductAddon::getProductId, productIds)
                    .in(ProductAddon::getAddonProductId, addonProductIds))) {
                addonOverridePrices.put(link.getProductId() + ":" + link.getAddonProductId(), link.getPrice());
            }
        }

        List<AppCartItemVO> list = new ArrayList<>();
        for (CartItem row : rows) {
            Sku sku = skus.get(row.getSkuId());
            if (sku == null) {
                continue;
            }
            Product product = products.get(sku.getProductId());
            if (product == null) {
                continue;
            }
            BigDecimal basePrice = AppCalc.money(sku.getPrice());
            Map<String, Object> specs = AppCalc.parseSpecs(sku.getSpecsJson());
            Map<String, Object> options = AppCalc.parseOptions(row.getOptions());

            List<AppCartAddonVO> addonVOs = new ArrayList<>();
            BigDecimal addonAmount = BigDecimal.ZERO;
            for (AppAddonDTO addon : addonsByRow.getOrDefault(row.getId(), new ArrayList<>())) {
                Sku addonSku = addonSkus.get(addon.getSkuId());
                if (addonSku == null) {
                    continue;
                }
                Product addonProduct = addonProducts.get(addonSku.getProductId());
                BigDecimal override = addonOverridePrices.get(product.getId() + ":" + addonSku.getProductId());
                BigDecimal addonPrice = AppCalc.money(override != null ? override : addonSku.getPrice());
                AppCartAddonVO addonVO = new AppCartAddonVO();
                addonVO.setSkuId(addonSku.getId());
                addonVO.setProductId(addonSku.getProductId());
                addonVO.setName(addonProduct == null ? "加料" : addonProduct.getName());
                addonVO.setPrice(addonPrice);
                addonVO.setQty(AppCalc.qtyOf(addon.getQty()));
                addonVOs.add(addonVO);
                addonAmount = addonAmount.add(addonPrice.multiply(BigDecimal.valueOf(addonVO.getQty())));
            }
            BigDecimal unitPrice = AppCalc.money(basePrice.add(addonAmount));

            String baseText = AppCalc.combineText(specs, options);
            String addonText = addonVOs.stream()
                    .map(a -> a.getQty() != null && a.getQty() > 1 ? a.getName() + "×" + a.getQty() : a.getName())
                    .collect(Collectors.joining("+"));

            AppCartItemVO vo = new AppCartItemVO();
            vo.setId(row.getId());
            vo.setSkuId(sku.getId());
            vo.setProductId(product.getId());
            vo.setProductName(product.getName());
            vo.setCategoryId(product.getCategoryId());
            vo.setSpecs(specs);
            vo.setOptions(options);
            vo.setSpecsText(addonText.isEmpty() ? baseText
                    : (baseText.isEmpty() ? addonText : baseText + " + " + addonText));
            vo.setAddons(addonVOs);
            vo.setBasePrice(basePrice);
            vo.setUnitPrice(unitPrice);
            vo.setPrice(unitPrice);
            vo.setStock(sku.getStock());
            vo.setQty(row.getQty());
            vo.setChecked(!unchecked.contains(row.getId()));
            vo.setShopId(row.getShopId());
            list.add(vo);
        }
        return list;
    }

    private AppCartVO assemble(Long userId, List<AppCartItemVO> lines) {
        BigDecimal goodsAmount = BigDecimal.ZERO;
        int totalQty = 0;
        for (AppCartItemVO line : lines) {
            int qty = line.getQty() == null ? 0 : line.getQty();
            totalQty += qty;
            if (Boolean.TRUE.equals(line.getChecked())) {
                goodsAmount = goodsAmount.add(line.getPrice().multiply(BigDecimal.valueOf(qty)));
            }
        }
        goodsAmount = AppCalc.money(goodsAmount);

        AppCartVO vo = new AppCartVO();
        vo.setList(lines);
        vo.setGoodsAmount(goodsAmount);
        // 券与积分在结算页试算，购物车只给商品合计
        vo.setCouponAmount(BigDecimal.ZERO);
        vo.setPointsUsed(0);
        vo.setPointsAmount(BigDecimal.ZERO);
        vo.setPayable(goodsAmount);
        vo.setTotalQty(totalQty);
        return vo;
    }

    private AppCartVO emptyCart() {
        return assemble(null, new ArrayList<>());
    }

    private CartItem requireOwnedLine(Long userId, Long lineId) {
        CartItem row = lineId == null ? null : cartItemMapper.selectById(lineId);
        if (row == null || !Objects.equals(row.getUserId(), userId)) {
            throw new ServiceException("购物车记录不存在");
        }
        return row;
    }

    private Set<Long> uncheckedIds(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        try {
            Set<String> members = stringRedisTemplate.opsForSet().members(UNCHECKED_KEY_PREFIX + userId);
            if (members == null || members.isEmpty()) {
                return Collections.emptySet();
            }
            Set<Long> ids = new HashSet<>();
            for (String member : members) {
                try {
                    ids.add(Long.valueOf(member));
                } catch (NumberFormatException ignored) {
                    // 脏数据忽略
                }
            }
            return ids;
        } catch (Exception e) {
            log.warn("读取购物车勾选态失败，降级为全部勾选：{}", e.getMessage());
            return Collections.emptySet();
        }
    }

    private void markChecked(Long userId, Long lineId, boolean checked) {
        if (userId == null || lineId == null) {
            return;
        }
        String key = UNCHECKED_KEY_PREFIX + userId;
        try {
            if (checked) {
                stringRedisTemplate.opsForSet().remove(key, String.valueOf(lineId));
            } else {
                stringRedisTemplate.opsForSet().add(key, String.valueOf(lineId));
            }
        } catch (Exception e) {
            log.warn("写入购物车勾选态失败：{}", e.getMessage());
        }
    }

    private void clearUnchecked(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            stringRedisTemplate.delete(UNCHECKED_KEY_PREFIX + userId);
        } catch (Exception e) {
            log.warn("清空购物车勾选态失败：{}", e.getMessage());
        }
    }
}
