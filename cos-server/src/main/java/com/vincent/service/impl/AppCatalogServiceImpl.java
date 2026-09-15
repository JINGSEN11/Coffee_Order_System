package com.vincent.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vincent.common.BaseContext;
import com.vincent.common.exception.ServiceException;
import com.vincent.entity.Category;
import com.vincent.entity.OrderItem;
import com.vincent.entity.Product;
import com.vincent.entity.ProductAddon;
import com.vincent.entity.ProductSpecGroup;
import com.vincent.entity.Review;
import com.vincent.entity.Shop;
import com.vincent.entity.Sku;
import com.vincent.entity.SpecGroup;
import com.vincent.entity.SpecOption;
import com.vincent.mapper.CategoryMapper;
import com.vincent.mapper.OrderItemMapper;
import com.vincent.mapper.ProductAddonMapper;
import com.vincent.mapper.ProductMapper;
import com.vincent.mapper.ProductSpecGroupMapper;
import com.vincent.mapper.ReviewMapper;
import com.vincent.mapper.SkuMapper;
import com.vincent.mapper.SpecGroupMapper;
import com.vincent.mapper.SpecOptionMapper;
import com.vincent.service.AppCartService;
import com.vincent.service.AppCatalogService;
import com.vincent.vo.AppAddonGroupVO;
import com.vincent.vo.AppAddonOptionVO;
import com.vincent.vo.AppMenuCategoryVO;
import com.vincent.vo.AppMenuVO;
import com.vincent.vo.AppProductDetailVO;
import com.vincent.vo.AppProductVO;
import com.vincent.vo.AppReviewVO;
import com.vincent.vo.AppSearchVO;
import com.vincent.vo.AppSkuVO;
import com.vincent.vo.AppSpecGroupVO;
import com.vincent.vo.AppSpecOptionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppCatalogServiceImpl implements AppCatalogService {

    /** 无评价时的默认评分 */
    private static final BigDecimal DEFAULT_REVIEW_SCORE = new BigDecimal("4.9");

    private final ProductMapper productMapper;
    private final SkuMapper skuMapper;
    private final CategoryMapper categoryMapper;
    private final OrderItemMapper orderItemMapper;
    private final ReviewMapper reviewMapper;
    private final SpecGroupMapper specGroupMapper;
    private final SpecOptionMapper specOptionMapper;
    private final ProductSpecGroupMapper productSpecGroupMapper;
    private final ProductAddonMapper productAddonMapper;
    private final AppCartService appCartService;
    private final AppShopResolver shopResolver;

    @Override
    public AppMenuVO menu(Long shopId) {
        Shop shop = shopResolver.resolve(shopId);

        List<Category> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getStatus, 1)
                        // 加料分类（show_in_app=0）只在饮品的加料栏出现，不在点餐菜单单独成栏
                        .eq(Category::getShowInApp, 1)
                        .orderByAsc(Category::getSort)
        );
        List<Product> products = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, 1)
                        .orderByDesc(Product::getSales)
        );
        Map<Long, List<Product>> productsByCategory = products.stream()
                .filter(p -> p.getCategoryId() != null)
                .collect(Collectors.groupingBy(Product::getCategoryId, LinkedHashMap::new, Collectors.toList()));

        List<AppProductVO> allVos = buildProductVOs(products);
        Map<Long, AppProductVO> voById = allVos.stream()
                .collect(Collectors.toMap(AppProductVO::getId, v -> v, (a, b) -> a));

        List<AppMenuCategoryVO> menuCategories = new ArrayList<>();
        for (Category category : categories) {
            List<AppProductVO> categoryProducts = productsByCategory
                    .getOrDefault(category.getId(), Collections.emptyList())
                    .stream()
                    .map(p -> voById.get(p.getId()))
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
            if (categoryProducts.isEmpty()) {
                // 只返回含上架商品的分类
                continue;
            }
            AppMenuCategoryVO vo = new AppMenuCategoryVO();
            vo.setId(category.getId());
            vo.setName(category.getName());
            vo.setSort(category.getSort());
            vo.setStatus(category.getStatus());
            vo.setCount(categoryProducts.size());
            vo.setProducts(categoryProducts);
            menuCategories.add(vo);
        }

        AppMenuVO menu = new AppMenuVO();
        menu.setShop(shopBrief(shop));
        menu.setCategories(menuCategories);
        menu.setCart(appCartService.buildCart(BaseContext.getCurrentId()));
        return menu;
    }

    @Override
    public AppProductDetailVO productDetail(Long productId) {
        Product product = productId == null ? null : productMapper.selectById(productId);
        if (product == null || product.getStatus() == null || product.getStatus() != 1) {
            throw new ServiceException("商品不存在或已下架");
        }

        List<Sku> skus = skuMapper.selectList(
                new LambdaQueryWrapper<Sku>().eq(Sku::getProductId, product.getId()).orderByAsc(Sku::getId)
        );
        // 是否「可定制」：由商品是否挂了规格组决定（温度/糖度不再进 SKU，故不能再用 specs_json 判断）
        boolean hasMatrix = productSpecGroupMapper.selectCount(
                new LambdaQueryWrapper<ProductSpecGroup>().eq(ProductSpecGroup::getProductId, product.getId())
        ) > 0;

        AppProductDetailVO vo = new AppProductDetailVO();
        fill(vo, product, skus, categoryName(product.getCategoryId()), reviewScores(List.of(product.getId())));
        vo.setSkus(skus.stream().map(this::toSkuVO).collect(Collectors.toList()));
        vo.setHasMatrix(hasMatrix);
        vo.setSpecGroups(hasMatrix ? buildSpecGroups(product, skus) : new ArrayList<>());
        // 加料是独立分装商品，由 product_addon 声明「这个饮品能加哪些料」，
        // 不再靠 hasMatrix 猜（烘焙/周边没有关联记录，自然返回 null）。
        vo.setAddonGroup(buildAddonGroup(product));
        vo.setDetails(AppSpec.details(hasMatrix));
        vo.setReviews(productReviews(product.getId(), skus, 3));
        return vo;
    }

    @Override
    public AppSearchVO search(String kw) {
        String keyword = kw == null ? "" : kw.trim();
        AppSearchVO vo = new AppSearchVO();
        vo.setKw(keyword);
        vo.setHotKeywords(AppSpec.HOT_KEYWORDS);
        if (keyword.isEmpty()) {
            vo.setList(new ArrayList<>());
            return vo;
        }

        List<Long> matchedCategoryIds = categoryMapper.selectList(
                        new LambdaQueryWrapper<Category>().like(Category::getName, keyword)
                ).stream().map(Category::getId).collect(Collectors.toList());

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>().eq(Product::getStatus, 1);
        wrapper.and(w -> {
            w.like(Product::getName, keyword)
                    .or().like(Product::getTags, keyword)
                    .or().like(Product::getDescription, keyword);
            if (!matchedCategoryIds.isEmpty()) {
                w.or().in(Product::getCategoryId, matchedCategoryIds);
            }
        });
        wrapper.orderByDesc(Product::getSales);
        // 加料商品是饮品的可选项，不在搜索结果里单独出现
        List<Long> hiddenCategoryIds = categoryMapper.selectList(
                        new LambdaQueryWrapper<Category>().eq(Category::getShowInApp, 0)
                ).stream().map(Category::getId).collect(Collectors.toList());
        if (!hiddenCategoryIds.isEmpty()) {
            wrapper.notIn(Product::getCategoryId, hiddenCategoryIds);
        }
        vo.setList(buildProductVOs(productMapper.selectList(wrapper)));
        return vo;
    }

    @Override
    public List<AppProductVO> buildProductVOs(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> productIds = products.stream().map(Product::getId).collect(Collectors.toList());
        List<Sku> skus = skuMapper.selectList(
                new LambdaQueryWrapper<Sku>().in(Sku::getProductId, productIds).orderByAsc(Sku::getId)
        );
        Map<Long, List<Sku>> skusByProduct = skus.stream()
                .filter(s -> s.getProductId() != null)
                .collect(Collectors.groupingBy(Sku::getProductId));

        Map<Long, String> categoryNames = categoryNames(
                products.stream().map(Product::getCategoryId).filter(java.util.Objects::nonNull)
                        .distinct().collect(Collectors.toList())
        );
        Map<Long, BigDecimal> scores = reviewScores(productIds);

        List<AppProductVO> list = new ArrayList<>();
        for (Product product : products) {
            AppProductVO vo = new AppProductVO();
            fill(vo, product, skusByProduct.getOrDefault(product.getId(), Collections.emptyList()),
                    categoryNames.get(product.getCategoryId()), scores);
            list.add(vo);
        }
        return list;
    }

    /* ---------------- 内部方法 ---------------- */

    private void fill(AppProductVO vo, Product product, List<Sku> skus, String categoryName,
                      Map<Long, BigDecimal> scores) {
        vo.setId(product.getId());
        vo.setCategoryId(product.getCategoryId());
        vo.setCategoryName(categoryName == null ? "" : categoryName);
        vo.setName(product.getName());
        vo.setDescription(product.getDescription());
        vo.setImage(product.getImage());
        vo.setTags(product.getTags());
        vo.setTagList(StringUtils.hasText(product.getTags())
                ? java.util.Arrays.stream(product.getTags().split(",")).filter(StringUtils::hasText)
                        .collect(Collectors.toList())
                : new ArrayList<>());
        vo.setSales(product.getSales());
        vo.setStatus(product.getStatus());

        BigDecimal min = null;
        BigDecimal max = null;
        int inStock = 0;
        boolean allBelowWarn = true;
        for (Sku sku : skus) {
            BigDecimal price = sku.getPrice() == null ? BigDecimal.ZERO : sku.getPrice();
            min = min == null || price.compareTo(min) < 0 ? price : min;
            max = max == null || price.compareTo(max) > 0 ? price : max;
            int stock = sku.getStock() == null ? 0 : sku.getStock();
            if (stock > 0) {
                inStock++;
                int warn = sku.getWarnStock() == null ? 0 : sku.getWarnStock();
                if (stock > warn) {
                    allBelowWarn = false;
                }
            }
        }
        vo.setMinPrice(AppCalc.money(min));
        vo.setMaxPrice(AppCalc.money(max));
        vo.setSkuCount(skus.size());
        vo.setSoldOut(inStock == 0);
        vo.setLowStock(inStock > 0 && allBelowWarn);
        vo.setReviewScore(scores.getOrDefault(product.getId(), DEFAULT_REVIEW_SCORE));
    }

    /**
     * 规格组：来自 spec_group / spec_option / product_spec_group 三张表。
     * 价格维度组（杯型）的 disabled = 该选项下不存在有库存的 SKU；
     * 非价格维度组（温度/糖度）不影响库存，只受「该商品可用选项白名单」限制。
     */
    private List<AppSpecGroupVO> buildSpecGroups(Product product, List<Sku> skus) {
        List<ProductSpecGroup> links = productSpecGroupMapper.selectList(
                new LambdaQueryWrapper<ProductSpecGroup>()
                        .eq(ProductSpecGroup::getProductId, product.getId())
                        .orderByAsc(ProductSpecGroup::getSort)
        );
        if (links.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> groupIds = links.stream().map(ProductSpecGroup::getGroupId)
                .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, SpecGroup> groupById = specGroupMapper.selectBatchIds(groupIds).stream()
                .filter(g -> g.getStatus() != null && g.getStatus() == 1)
                .collect(Collectors.toMap(SpecGroup::getId, g -> g, (a, b) -> a));
        Map<Long, List<SpecOption>> optionsByGroup = specOptionMapper.selectList(
                        new LambdaQueryWrapper<SpecOption>()
                                .in(SpecOption::getGroupId, groupIds)
                                .eq(SpecOption::getStatus, 1)
                                .orderByAsc(SpecOption::getSort))
                .stream().collect(Collectors.groupingBy(SpecOption::getGroupId));

        // 价格维度组的「有货选项值」：由有库存的 SKU 反查
        Set<String> inStockValues = new HashSet<>();
        for (Sku sku : skus) {
            if (sku.getStock() != null && sku.getStock() > 0) {
                AppCalc.parseSpecs(sku.getSpecsJson()).values()
                        .forEach(v -> inStockValues.add(String.valueOf(v)));
            }
        }

        List<AppSpecGroupVO> groups = new ArrayList<>();
        for (ProductSpecGroup link : links) {
            SpecGroup group = groupById.get(link.getGroupId());
            if (group == null) {
                continue;
            }
            boolean priceDim = group.getIsPriceDim() != null && group.getIsPriceDim() == 1;
            Set<Long> allowed = parseOptionIds(link.getOptionIds());
            List<AppSpecOptionVO> optionVOs = new ArrayList<>();
            for (SpecOption option : optionsByGroup.getOrDefault(group.getId(), Collections.emptyList())) {
                if (allowed != null && !allowed.contains(option.getId())) {
                    continue;
                }
                AppSpecOptionVO optionVO = new AppSpecOptionVO();
                optionVO.setName(option.getName());
                optionVO.setExtra(priceDim ? AppCalc.money(option.getExtra()) : BigDecimal.ZERO);
                optionVO.setDisabled(priceDim && !inStockValues.contains(option.getName()));
                optionVOs.add(optionVO);
            }
            if (optionVOs.isEmpty()) {
                continue;
            }
            AppSpecGroupVO groupVO = new AppSpecGroupVO();
            groupVO.setKey(group.getGroupKey());
            groupVO.setLabel(group.getName());
            groupVO.setRequired(group.getRequired() == null || group.getRequired() == 1);
            groupVO.setMulti(group.getMulti() != null && group.getMulti() == 1);
            groupVO.setPriceDim(priceDim);
            groupVO.setOptions(optionVOs);
            groups.add(groupVO);
        }
        return groups;
    }

    /** product_spec_group.option_ids → 可用选项 ID 集合；null / 空表示全组可用 */
    private Set<Long> parseOptionIds(String optionIds) {
        if (!StringUtils.hasText(optionIds)) {
            return null;
        }
        Set<Long> ids = new HashSet<>();
        try {
            for (Object item : JSONUtil.parseArray(optionIds)) {
                if (item != null) {
                    ids.add(Long.valueOf(String.valueOf(item)));
                }
            }
        } catch (Exception e) {
            log.warn("解析 product_spec_group.option_ids 失败：{}", optionIds, e);
            return null;
        }
        return ids.isEmpty() ? null : ids;
    }

    /**
     * 加料组：由 product_addon 关联的加料商品装配。
     * 加料是独立的分装商品（有自己的 SKU / 价格 / 库存），价格取关联覆盖价或加料商品售价。
     */
    private AppAddonGroupVO buildAddonGroup(Product product) {
        List<ProductAddon> links = productAddonMapper.selectList(
                new LambdaQueryWrapper<ProductAddon>()
                        .eq(ProductAddon::getProductId, product.getId())
                        .eq(ProductAddon::getStatus, 1)
                        .orderByAsc(ProductAddon::getSort)
        );
        if (links.isEmpty()) {
            return null;
        }
        List<Long> addonProductIds = links.stream().map(ProductAddon::getAddonProductId)
                .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        if (addonProductIds.isEmpty()) {
            return null;
        }
        Map<Long, Product> addonProductById = productMapper.selectBatchIds(addonProductIds).stream()
                .filter(p -> p.getStatus() != null && p.getStatus() == 1)
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));
        // 加料商品是标准品（单 SKU），取其 SKU 带出价格与库存
        Map<Long, Sku> skuByProduct = new HashMap<>();
        for (Sku sku : skuMapper.selectList(new LambdaQueryWrapper<Sku>()
                .in(Sku::getProductId, addonProductIds).orderByAsc(Sku::getId))) {
            skuByProduct.putIfAbsent(sku.getProductId(), sku);
        }

        List<AppAddonOptionVO> options = new ArrayList<>();
        for (ProductAddon link : links) {
            Product addonProduct = addonProductById.get(link.getAddonProductId());
            Sku sku = skuByProduct.get(link.getAddonProductId());
            if (addonProduct == null || sku == null) {
                continue;
            }
            int stock = sku.getStock() == null ? 0 : sku.getStock();
            AppAddonOptionVO option = new AppAddonOptionVO();
            option.setSkuId(sku.getId());
            option.setProductId(addonProduct.getId());
            option.setName(addonProduct.getName());
            option.setPrice(AppCalc.money(link.getPrice() != null ? link.getPrice() : sku.getPrice()));
            option.setStock(stock);
            option.setMaxQty(link.getMaxQty() == null ? 1 : link.getMaxQty());
            option.setDisabled(stock <= 0);
            options.add(option);
        }
        if (options.isEmpty()) {
            return null;
        }
        AppAddonGroupVO group = new AppAddonGroupVO();
        group.setKey(AppSpec.ADDON_GROUP_KEY);
        group.setLabel(AppSpec.ADDON_GROUP_LABEL);
        group.setRequired(false);
        group.setMulti(true);
        group.setOptions(options);
        return group;
    }

    private AppSkuVO toSkuVO(Sku sku) {
        AppSkuVO vo = new AppSkuVO();
        vo.setId(sku.getId());
        vo.setProductId(sku.getProductId());
        vo.setSpecsJson(AppCalc.parseSpecs(sku.getSpecsJson()));
        vo.setPrice(AppCalc.money(sku.getPrice()));
        vo.setStock(sku.getStock());
        vo.setWarnStock(sku.getWarnStock());
        return vo;
    }

    /** 商品评价摘要：取该商品历史订单中审核通过的评价，按时间倒序 */
    private List<AppReviewVO> productReviews(Long productId, List<Sku> skus, int limit) {
        if (skus.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> skuIds = skus.stream().map(Sku::getId).collect(Collectors.toList());
        List<Long> orderIds = orderItemMapper.selectList(
                        new LambdaQueryWrapper<OrderItem>().in(OrderItem::getSkuId, skuIds)
                ).stream().map(OrderItem::getOrderId).filter(java.util.Objects::nonNull).distinct()
                .collect(Collectors.toList());
        if (orderIds.isEmpty()) {
            return new ArrayList<>();
        }
        return reviewMapper.selectList(
                        new LambdaQueryWrapper<Review>()
                                .in(Review::getOrderId, orderIds)
                                .eq(Review::getStatus, 1)
                                .orderByDesc(Review::getCreatedAt)
                                .last("LIMIT " + limit)
                ).stream().map(this::toReviewVO).collect(Collectors.toList());
    }

    private AppReviewVO toReviewVO(Review review) {
        AppReviewVO vo = new AppReviewVO();
        vo.setId(review.getId());
        vo.setScore(review.getScore());
        vo.setContent(review.getContent());
        vo.setImages(AppCalc.parseImages(review.getImages()));
        vo.setCreatedAt(review.getCreatedAt());
        return vo;
    }

    /** 商品评价均分：由 order_item 反查 SKU 所属商品，再取评价；无评价返回 4.9 */
    private Map<Long, BigDecimal> reviewScores(List<Long> productIds) {
        Map<Long, BigDecimal> result = new HashMap<>();
        if (productIds == null || productIds.isEmpty()) {
            return result;
        }
        List<Sku> skus = skuMapper.selectList(
                new LambdaQueryWrapper<Sku>().in(Sku::getProductId, productIds)
        );
        if (skus.isEmpty()) {
            return result;
        }
        Map<Long, Long> skuToProduct = skus.stream()
                .collect(Collectors.toMap(Sku::getId, Sku::getProductId, (a, b) -> a));
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().in(OrderItem::getSkuId, new ArrayList<>(skuToProduct.keySet()))
        );
        Map<Long, Set<Long>> productsByOrder = new HashMap<>();
        for (OrderItem item : items) {
            Long productId = skuToProduct.get(item.getSkuId());
            if (productId != null && item.getOrderId() != null) {
                productsByOrder.computeIfAbsent(item.getOrderId(), k -> new java.util.HashSet<>()).add(productId);
            }
        }
        if (productsByOrder.isEmpty()) {
            return result;
        }
        List<Review> reviews = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>().in(Review::getOrderId, new ArrayList<>(productsByOrder.keySet()))
        );
        Map<Long, List<Integer>> scores = new HashMap<>();
        for (Review review : reviews) {
            Set<Long> pids = productsByOrder.get(review.getOrderId());
            if (pids == null || review.getScore() == null) {
                continue;
            }
            for (Long pid : pids) {
                scores.computeIfAbsent(pid, k -> new ArrayList<>()).add(review.getScore());
            }
        }
        for (Long pid : productIds) {
            List<Integer> values = scores.get(pid);
            if (values == null || values.isEmpty()) {
                continue;
            }
            int sum = values.stream().mapToInt(Integer::intValue).sum();
            result.put(pid, BigDecimal.valueOf(sum)
                    .divide(BigDecimal.valueOf(values.size()), 1, RoundingMode.HALF_UP));
        }
        return result;
    }

    private Map<Long, String> categoryNames(List<Long> categoryIds) {
        if (categoryIds.isEmpty()) {
            return new HashMap<>();
        }
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>().in(Category::getId, categoryIds))
                .stream().collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
    }

    private String categoryName(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        Category category = categoryMapper.selectById(categoryId);
        return category == null ? null : category.getName();
    }

    /** 契约里的门店简要信息仅 3 个字段 */
    private Map<String, Object> shopBrief(Shop shop) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", shop == null ? null : shop.getId());
        map.put("name", shop == null ? "" : shop.getName());
        map.put("accept_order", shop == null ? null : shop.getAcceptOrder());
        return map;
    }
}
