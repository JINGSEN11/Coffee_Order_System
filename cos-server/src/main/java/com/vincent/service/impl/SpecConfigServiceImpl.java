package com.vincent.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vincent.common.exception.ServiceException;
import com.vincent.dto.SpecConfigDTO;
import com.vincent.entity.OrderItem;
import com.vincent.entity.Product;
import com.vincent.entity.ProductAddon;
import com.vincent.entity.ProductSpecGroup;
import com.vincent.entity.Sku;
import com.vincent.entity.SpecGroup;
import com.vincent.entity.SpecOption;
import com.vincent.mapper.OrderItemMapper;
import com.vincent.mapper.ProductAddonMapper;
import com.vincent.mapper.ProductMapper;
import com.vincent.mapper.ProductSpecGroupMapper;
import com.vincent.mapper.SkuMapper;
import com.vincent.mapper.SpecGroupMapper;
import com.vincent.mapper.SpecOptionMapper;
import com.vincent.service.SpecConfigService;
import com.vincent.vo.SpecGroupAdminVO;
import com.vincent.vo.SpecOptionAdminVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpecConfigServiceImpl implements SpecConfigService {

    private final SpecGroupMapper specGroupMapper;
    private final SpecOptionMapper specOptionMapper;
    private final ProductSpecGroupMapper productSpecGroupMapper;
    private final ProductAddonMapper productAddonMapper;
    private final ProductMapper productMapper;
    private final SkuMapper skuMapper;
    private final OrderItemMapper orderItemMapper;

    @Override
    public List<SpecGroupAdminVO> groups() {
        List<SpecGroup> groups = specGroupMapper.selectList(
                new LambdaQueryWrapper<SpecGroup>().orderByAsc(SpecGroup::getSort));
        if (groups.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, List<SpecOption>> optionsByGroup = specOptionMapper.selectList(
                        new LambdaQueryWrapper<SpecOption>().orderByAsc(SpecOption::getSort))
                .stream().collect(Collectors.groupingBy(SpecOption::getGroupId, LinkedHashMap::new, Collectors.toList()));

        List<SpecGroupAdminVO> list = new ArrayList<>();
        for (SpecGroup group : groups) {
            SpecGroupAdminVO vo = new SpecGroupAdminVO();
            vo.setId(group.getId());
            vo.setGroupKey(group.getGroupKey());
            vo.setName(group.getName());
            vo.setRequired(group.getRequired() != null && group.getRequired() == 1);
            vo.setMulti(group.getMulti() != null && group.getMulti() == 1);
            vo.setIsPriceDim(group.getIsPriceDim() != null && group.getIsPriceDim() == 1);
            vo.setSort(group.getSort());
            vo.setStatus(group.getStatus());
            List<SpecOptionAdminVO> options = new ArrayList<>();
            for (SpecOption option : optionsByGroup.getOrDefault(group.getId(), new ArrayList<>())) {
                SpecOptionAdminVO optionVO = new SpecOptionAdminVO();
                optionVO.setId(option.getId());
                optionVO.setName(option.getName());
                optionVO.setExtra(option.getExtra());
                optionVO.setSort(option.getSort());
                optionVO.setStatus(option.getStatus());
                options.add(optionVO);
            }
            vo.setOptions(options);
            list.add(vo);
        }
        return list;
    }

    @Override
    public SpecConfigDTO configOf(Long productId) {
        SpecConfigDTO dto = new SpecConfigDTO();
        Map<Long, List<Long>> optionIds = new LinkedHashMap<>();
        List<ProductSpecGroup> links = productSpecGroupMapper.selectList(
                new LambdaQueryWrapper<ProductSpecGroup>().eq(ProductSpecGroup::getProductId, productId));
        List<Long> groupIds = new ArrayList<>();
        for (ProductSpecGroup link : links) {
            groupIds.add(link.getGroupId());
            // 落库为 NULL 表示全组可用，这里展开成全部选项 ID 便于前端回显
            List<Long> allowed = parseIds(link.getOptionIds());
            if (allowed == null) {
                allowed = specOptionMapper.selectList(new LambdaQueryWrapper<SpecOption>()
                                .eq(SpecOption::getGroupId, link.getGroupId()))
                        .stream().map(SpecOption::getId).collect(Collectors.toList());
            }
            optionIds.put(link.getGroupId(), allowed);
        }
        dto.setGroupIds(groupIds);
        dto.setOptionIds(optionIds);
        dto.setAddonProductIds(productAddonMapper.selectList(
                        new LambdaQueryWrapper<ProductAddon>().eq(ProductAddon::getProductId, productId)
                                .orderByAsc(ProductAddon::getSort))
                .stream().map(ProductAddon::getAddonProductId).collect(Collectors.toList()));
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveConfig(Long productId, SpecConfigDTO dto) {
        Product product = productId == null ? null : productMapper.selectById(productId);
        if (product == null) {
            throw new ServiceException("商品不存在");
        }
        List<Long> groupIds = dto == null || dto.getGroupIds() == null
                ? new ArrayList<>() : dto.getGroupIds().stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        List<SpecGroup> groups = groupIds.isEmpty() ? new ArrayList<>() : specGroupMapper.selectBatchIds(groupIds);
        Map<Long, List<SpecOption>> optionsByGroup = groups.isEmpty() ? new HashMap<>()
                : specOptionMapper.selectList(new LambdaQueryWrapper<SpecOption>()
                        .in(SpecOption::getGroupId, groupIds))
                .stream().collect(Collectors.groupingBy(SpecOption::getGroupId));

        // 1. 重建「商品 ⇄ 规格组」
        productSpecGroupMapper.delete(new LambdaQueryWrapper<ProductSpecGroup>()
                .eq(ProductSpecGroup::getProductId, productId));
        Map<Long, List<Long>> reqOptions = dto == null || dto.getOptionIds() == null
                ? new HashMap<>() : dto.getOptionIds();
        for (SpecGroup group : groups) {
            List<Long> all = optionsByGroup.getOrDefault(group.getId(), new ArrayList<>())
                    .stream().map(SpecOption::getId).collect(Collectors.toList());
            List<Long> allowed = reqOptions.get(group.getId());
            List<Long> effective = (allowed == null || allowed.isEmpty()) ? all
                    : allowed.stream().filter(all::contains).distinct().collect(Collectors.toList());
            if (effective.isEmpty()) {
                continue;
            }
            ProductSpecGroup row = new ProductSpecGroup();
            row.setProductId(productId);
            row.setGroupId(group.getId());
            // 全选存 NULL，语义更清晰也省空间
            row.setOptionIds(effective.size() == all.size() ? null : JSONUtil.toJsonStr(effective));
            row.setSort(group.getSort());
            productSpecGroupMapper.insert(row);
        }

        // 2. 重建「商品 ⇄ 加料」
        productAddonMapper.delete(new LambdaQueryWrapper<ProductAddon>()
                .eq(ProductAddon::getProductId, productId));
        List<Long> addonProductIds = dto == null || dto.getAddonProductIds() == null
                ? new ArrayList<>() : dto.getAddonProductIds().stream().filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        int sort = 0;
        for (Long addonProductId : addonProductIds) {
            if (productMapper.selectById(addonProductId) == null) {
                continue;
            }
            ProductAddon row = new ProductAddon();
            row.setProductId(productId);
            row.setAddonProductId(addonProductId);
            row.setMaxQty(3);
            row.setSort(sort++);
            row.setStatus(1);
            productAddonMapper.insert(row);
        }

        // 3. 价格维度组：补齐缺失的 SKU
        ensurePriceDimSkus(product, groups, optionsByGroup, reqOptions);

        // 4. 反向清理：配置撤掉后留下的孤儿 SKU
        //    （挂错规格组要能撤干净，否则商品会出现「有 2 个 SKU 却不挂任何规格组」的脏状态）
        reclaimOrphanSkus(product, groups, optionsByGroup, reqOptions);
    }

    /**
     * 回收因为「不再属于任何价格维度选项」而变成孤儿的 SKU。
     * 安全前提：**只删没有任何 order_item 引用的 SKU**，有历史订单引用的原样保留。
     */
    private void reclaimOrphanSkus(Product product, List<SpecGroup> groups,
                                   Map<Long, List<SpecOption>> optionsByGroup,
                                   Map<Long, List<Long>> reqOptions) {
        List<Sku> skus = skuMapper.selectList(
                new LambdaQueryWrapper<Sku>().eq(Sku::getProductId, product.getId()).orderByAsc(Sku::getId));
        if (skus.isEmpty()) {
            return;
        }

        // 商品不再挂任何价格维度组：应当回到「单规格」形态
        Map<String, java.util.Set<String>> allowedValues = new LinkedHashMap<>();
        for (SpecGroup group : groups) {
            if (group.getIsPriceDim() == null || group.getIsPriceDim() != 1) {
                continue;
            }
            List<Long> allowed = reqOptions.get(group.getId());
            java.util.Set<String> names = optionsByGroup.getOrDefault(group.getId(), new ArrayList<>())
                    .stream()
                    .filter(o -> allowed == null || allowed.isEmpty() || allowed.contains(o.getId()))
                    .map(SpecOption::getName).collect(Collectors.toSet());
            allowedValues.put(group.getName(), names);
        }

        if (allowedValues.isEmpty()) {
            // 本来就是单规格商品（烘焙/周边）：不动
            if (skus.stream().allMatch(this::isStandardOnly)) {
                return;
            }
            Sku keep = skus.get(0);
            if (!isStandardOnly(keep)) {
                Map<String, Object> specs = new LinkedHashMap<>();
                specs.put(AppSpec.STANDARD_SPEC_KEY, AppSpec.STANDARD_SPEC_VALUE);
                keep.setSpecsJson(JSONUtil.toJsonStr(specs));
                skuMapper.updateById(keep);
            }
            for (int i = 1; i < skus.size(); i++) {
                deleteIfUnreferenced(skus.get(i), "商品已取消规格");
            }
            return;
        }

        for (Sku sku : skus) {
            Map<String, Object> specs = AppCalc.parseSpecs(sku.getSpecsJson());
            boolean belongs = false;
            for (Map.Entry<String, java.util.Set<String>> entry : allowedValues.entrySet()) {
                Object value = specs.get(entry.getKey());
                if (value != null && entry.getValue().contains(String.valueOf(value))) {
                    belongs = true;
                    break;
                }
            }
            if (!belongs) {
                deleteIfUnreferenced(sku, "该规格已从商品配置中移除");
            }
        }
    }

    /** 没有任何订单明细引用时才删除 */
    private void deleteIfUnreferenced(Sku sku, String why) {
        Long refs = orderItemMapper.selectCount(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getSkuId, sku.getId()));
        if (refs != null && refs > 0) {
            log.info("SKU {} 被 {} 条订单明细引用，保留不删（{}）", sku.getId(), refs, why);
            return;
        }
        skuMapper.deleteById(sku.getId());
        log.info("回收 SKU {}（{}）", sku.getId(), why);
    }

    /**
     * 为价格维度组补齐 SKU：以现有最低价为基准 + 选项加价。
     * 只补不删 —— 已有 SKU 可能被历史订单明细引用，删掉会让 sku_id 失效。
     */
    private void ensurePriceDimSkus(Product product, List<SpecGroup> groups,
                                    Map<Long, List<SpecOption>> optionsByGroup,
                                    Map<Long, List<Long>> reqOptions) {
        List<SpecGroup> priceGroups = groups.stream()
                .filter(g -> g.getIsPriceDim() != null && g.getIsPriceDim() == 1)
                .collect(Collectors.toList());
        if (priceGroups.isEmpty()) {
            return;
        }
        List<Sku> skus = skuMapper.selectList(
                new LambdaQueryWrapper<Sku>().eq(Sku::getProductId, product.getId()));
        if (skus.isEmpty()) {
            // 商品还没有任何 SKU：由商品编辑页先落一个基础 SKU，这里不越权造
            return;
        }
        BigDecimal base = skus.stream().map(Sku::getPrice).filter(Objects::nonNull)
                .min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

        for (SpecGroup group : priceGroups) {
            List<Long> allowed = reqOptions.get(group.getId());
            List<SpecOption> options = optionsByGroup.getOrDefault(group.getId(), new ArrayList<>())
                    .stream()
                    .filter(o -> allowed == null || allowed.isEmpty() || allowed.contains(o.getId()))
                    .sorted(Comparator.comparing(o -> o.getExtra() == null ? BigDecimal.ZERO : o.getExtra()))
                    .collect(Collectors.toList());
            if (options.isEmpty()) {
                continue;
            }

            Map<String, Sku> byValue = new LinkedHashMap<>();
            for (Sku sku : skus) {
                Object value = AppCalc.parseSpecs(sku.getSpecsJson()).get(group.getName());
                if (value != null) {
                    byValue.put(String.valueOf(value), sku);
                }
            }

            // 商品此前是单规格（specs_json = {"规格":"标准"}）：把这个 SKU 直接当成加价最低的
            // 那个选项（通常是「中杯」），而不是另建一个 —— 否则会变成
            // 「标准 + 中杯 + 大杯」三个 SKU，其中「标准」谁也不是，还多占一份库存。
            if (byValue.isEmpty() && skus.size() == 1 && isStandardOnly(skus.get(0))) {
                SpecOption baseOption = options.get(0);
                Sku only = skus.get(0);
                Map<String, Object> specs = new LinkedHashMap<>();
                specs.put(group.getName(), baseOption.getName());
                only.setSpecsJson(JSONUtil.toJsonStr(specs));
                only.setPrice(AppCalc.money(base.add(
                        baseOption.getExtra() == null ? BigDecimal.ZERO : baseOption.getExtra())));
                skuMapper.updateById(only);
                byValue.put(baseOption.getName(), only);
                log.info("商品 {} 的单规格 SKU 已改为 {}={}", product.getName(), group.getName(),
                        baseOption.getName());
            }

            for (SpecOption option : options) {
                if (byValue.containsKey(option.getName())) {
                    continue;
                }
                Sku sku = new Sku();
                sku.setProductId(product.getId());
                Map<String, Object> specs = new LinkedHashMap<>();
                specs.put(group.getName(), option.getName());
                sku.setSpecsJson(JSONUtil.toJsonStr(specs));
                BigDecimal extra = option.getExtra() == null ? BigDecimal.ZERO : option.getExtra();
                sku.setPrice(AppCalc.money(base.add(extra)));
                // 新规格还没入库，库存给 0，由库存管理页入库
                sku.setStock(0);
                sku.setWarnStock(10);
                skuMapper.insert(sku);
                byValue.put(option.getName(), sku);
                log.info("为商品 {} 补齐规格 SKU：{}={}，价格 {}", product.getName(), group.getName(),
                        option.getName(), sku.getPrice());
            }
        }
    }

    /** 是否只是「标准」单规格（没有参与价格矩阵的规格） */
    private boolean isStandardOnly(Sku sku) {
        Map<String, Object> specs = AppCalc.parseSpecs(sku.getSpecsJson());
        return specs.size() == 1
                && AppSpec.STANDARD_SPEC_KEY.equals(String.valueOf(specs.keySet().iterator().next()))
                && AppSpec.STANDARD_SPEC_VALUE.equals(String.valueOf(specs.get(AppSpec.STANDARD_SPEC_KEY)));
    }

    /** 解析 option_ids JSON；null / 空表示全组可用 */
    private List<Long> parseIds(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        List<Long> ids = new ArrayList<>();
        try {
            for (Object item : JSONUtil.parseArray(json)) {
                if (item != null) {
                    ids.add(Long.valueOf(String.valueOf(item)));
                }
            }
        } catch (Exception e) {
            log.warn("解析 product_spec_group.option_ids 失败：{}", json, e);
            return null;
        }
        return ids.isEmpty() ? null : ids;
    }
}
