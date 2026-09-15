package com.vincent.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.ResponseFormat;
import org.springframework.beans.factory.ObjectProvider;
import com.vincent.config.AiProperties;
import com.vincent.dto.AppAiChatDTO;
import com.vincent.dto.AppAiMessageDTO;
import com.vincent.entity.Category;
import com.vincent.entity.Product;
import com.vincent.entity.Shop;
import com.vincent.mapper.CategoryMapper;
import com.vincent.mapper.ProductMapper;
import com.vincent.service.AppAiService;
import com.vincent.service.AppCatalogService;
import com.vincent.service.AppCouponService;
import com.vincent.vo.AppAiChatVO;
import com.vincent.vo.AppAiGreetingVO;
import com.vincent.vo.AppAiSuggestionVO;
import com.vincent.vo.AppCouponPageVO;
import com.vincent.vo.AppCouponVO;
import com.vincent.vo.AppProductVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 助手（小卡）实现。
 *
 * 两条链路，同一个出口：
 *   · 模型链路：把「在售商品清单 + 可领券清单」塞进 system 提示词，要求模型只回 JSON，
 *     商品/券一律只给 id；服务端拿 id 回表装配真实卡片，模型编不出价格和库存。
 *   · 本地兜底：未配 key / 调用失败 / 输出不合规时，用「字词命中 + 意图词典 + 销量」
 *     自己挑商品，保证没网没 key 也能给出一份像样的推荐。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppAiServiceImpl implements AppAiService {

    private static final String NAME = "小卡";
    private static final String TITLE = "Hi，我是小卡";
    private static final String SUBTITLE = "你的咖啡点单搭子";
    private static final String DISCLAIMER = "本服务为 AI 生成内容，仅供参考";

    private static final String SOURCE_AI = "ai";
    private static final String SOURCE_LOCAL = "local";

    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";

    /** 评分达到该值才算「真的对味」，否则按热销兜底并如实告诉顾客（避免硬凑推荐） */
    private static final int STRONG_SCORE = 4;

    /** 回复文本上限，防御模型话痨（前端气泡不适合长文） */
    private static final int REPLY_LIMIT = 110;

    /** 意图词典：key 为触发词（逗号分隔），value 为落到商品文本检索的候选词 */
    private static final Map<String, List<String>> INTENTS = new LinkedHashMap<>();

    static {
        INTENTS.put("提神,醒,困,熬夜,加班,累,美式,浓,苦,咖啡因",
                List.of("美式", "浓缩", "冷萃", "咖啡", "提神", "醒脑", "深烘"));
        INTENTS.put("不苦,顺口,好入口,奶味,奶香,甜一点,柔和",
                List.of("拿铁", "澳白", "卡布", "玛奇朵", "摩卡", "厚乳", "燕麦", "奶"));
        INTENTS.put("清爽,解腻,不腻,果茶,水果,冰,无糖,低卡,轻,减脂,夏天",
                List.of("冰", "冷", "果", "茶", "柠檬", "青提", "气泡", "无糖", "低卡"));
        INTENTS.put("热,暖,冬天,暖胃,姜",
                List.of("热", "暖", "姜", "燕麦"));
        INTENTS.put("点心,甜点,面包,烘焙,小吃,配,饿,早餐,下午茶",
                List.of("可颂", "贝果", "吐司", "面包", "蛋糕", "曲奇", "甜点", "派", "司康", "布丁"));
        INTENTS.put("招牌,推荐,必点,好喝,人气,热销,新品,经典",
                List.of("招牌", "推荐", "热销", "人气", "新品"));
        INTENTS.put("甜,焦糖,可可,巧克力",
                List.of("焦糖", "可可", "芝士", "奶油", "厚乳", "巧克力"));
    }

    /** 领券意图触发词 */
    private static final List<String> COUPON_WORDS =
            List.of("券", "优惠", "折扣", "打折", "省钱", "减免", "免单", "便宜");

    private final AiProperties aiProperties;
    /**
     * DeepSeek 模型（由 spring-ai-starter-model-deepseek 自动装配）。
     * 用 ObjectProvider 而不是直接注入：没配 key 时（spring.ai.model.chat != deepseek）
     * 自动装配会整体跳过，这里拿到 null 就走本地兜底 —— 服务照常启动，不会因为没有 key 就起不来。
     */
    private final ObjectProvider<DeepSeekChatModel> chatModelProvider;
    private final AppCatalogService appCatalogService;
    private final AppCouponService appCouponService;
    private final AppShopResolver appShopResolver;
    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;

    @Override
    public AppAiGreetingVO greeting(Long userId, Long shopId) {
        Snapshot snap = snapshot(userId, shopId);
        AppAiGreetingVO vo = new AppAiGreetingVO();
        vo.setName(NAME);
        vo.setTitle(TITLE);
        vo.setSubtitle(SUBTITLE);
        vo.setDisclaimer(DISCLAIMER);
        vo.setSuggestions(suggestions(snap));
        return vo;
    }

    @Override
    public AppAiChatVO chat(Long userId, AppAiChatDTO dto) {
        String question = dto == null || dto.getMessage() == null ? "" : dto.getMessage().trim();
        Snapshot snap = snapshot(userId, dto == null ? null : dto.getShopId());

        // 1) 模型链路：只有「模型可用 + 开关打开 + 有实际提问」才走，避免拿空问题烧额度
        DeepSeekChatModel model = aiProperties.isEnabled() && !question.isEmpty()
                ? chatModelProvider.getIfAvailable()
                : null;
        if (model == null) {
            log.debug("AI 助手走本地兜底（总开关 enabled={}，DeepSeekChatModel 是否装配={}）",
                    aiProperties.isEnabled(), chatModelProvider.getIfAvailable() != null);
        } else {
            String content = callModel(model, question, dto.getHistory(), snap);
            if (content != null) {
                AppAiChatVO byModel = parseModelReply(content, snap);
                if (byModel != null) {
                    fillSuggestions(byModel, snap);
                    log.info("AI 助手（模型链路）回复：{}，商品 {} 个，券 {} 张",
                            abbreviate(byModel.getReply(), 40),
                            size(byModel.getProducts()), size(byModel.getCoupons()));
                    return byModel;
                }
            }
        }

        // 2) 本地兜底
        AppAiChatVO vo = localReply(question, snap);
        fillSuggestions(vo, snap);
        return vo;
    }

    /* ==================== 数据快照 ==================== */

    /**
     * 一次对话所需的全部真实数据：门店、在售商品、当前用户视角的券。
     * 提示词与卡片都从这份快照里取，保证「模型眼里看到的」和「顾客看到的」是同一份。
     */
    private Snapshot snapshot(Long userId, Long shopId) {
        Snapshot snap = new Snapshot();
        snap.shop = appShopResolver.resolve(shopId);

        // 商品：上架 + 排除「加料」这类不单独售卖的隐藏分类，按销量倒序截断
        List<Long> hiddenCategoryIds = categoryMapper.selectList(
                        new LambdaQueryWrapper<Category>().eq(Category::getShowInApp, 0)
                ).stream().map(Category::getId).collect(Collectors.toList());
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>().eq(Product::getStatus, 1);
        if (!hiddenCategoryIds.isEmpty()) {
            wrapper.notIn(Product::getCategoryId, hiddenCategoryIds);
        }
        wrapper.orderByDesc(Product::getSales).orderByAsc(Product::getId);
        wrapper.last("LIMIT " + Math.max(1, aiProperties.getCatalogSize()));
        snap.products = appCatalogService.buildProductVOs(productMapper.selectList(wrapper));

        // 券：直接复用领券中心，can_claim / remaining 与领券页口径完全一致
        AppCouponPageVO center = appCouponService.list(userId, "center");
        snap.coupons = center == null || center.getList() == null ? new ArrayList<>() : center.getList();
        return snap;
    }

    /* ==================== 建议问法 ==================== */

    private List<AppAiSuggestionVO> suggestions(Snapshot snap) {
        List<AppAiSuggestionVO> list = new ArrayList<>();
        AppProductVO hot = firstSellable(snap.products);
        if (hot != null) {
            list.add(new AppAiSuggestionVO("☕", "「" + hot.getName() + "」怎么样？"));
        } else {
            list.add(new AppAiSuggestionVO("☕", "推荐一杯提神的"));
        }
        list.add(new AppAiSuggestionVO("🧊", "来杯清爽不腻的"));
        list.add(new AppAiSuggestionVO("🥐", "配咖啡的点心推荐一个"));
        if (!claimableCoupons(snap.coupons, 1).isEmpty()) {
            list.add(new AppAiSuggestionVO("🎟️", "现在有哪些券能领？"));
        } else {
            list.add(new AppAiSuggestionVO("⭐", "招牌和新品有哪些？"));
        }
        return list;
    }

    /** 模型没给追问时补上，保证每轮都有可点的下一步 */
    private void fillSuggestions(AppAiChatVO vo, Snapshot snap) {
        if (vo.getSuggestions() != null && !vo.getSuggestions().isEmpty()) {
            return;
        }
        List<AppAiSuggestionVO> list = new ArrayList<>();
        if (hasText(vo.getCoupons())) {
            list.add(new AppAiSuggestionVO("🍰", "配个点心来吃"));
            list.add(new AppAiSuggestionVO("☕", "有券的话推荐点什么？"));
        } else if (hasText(vo.getProducts())) {
            list.add(new AppAiSuggestionVO("🧊", "有没有清爽一点的？"));
            list.add(new AppAiSuggestionVO("🥐", "配个点心"));
        } else {
            list.add(new AppAiSuggestionVO("☕", "推荐一杯提神的"));
            list.add(new AppAiSuggestionVO("🎟️", "现在有哪些券能领？"));
        }
        vo.setSuggestions(list);
    }

    /* ==================== 模型链路 ==================== */

    /**
     * 调用 DeepSeek（Spring AI 的 DeepSeekChatModel），返回模型输出的原始文本。
     * 返回 null 表示「这轮模型没给出可用结果」，调用方据此降级为本地兜底。
     */
    private String callModel(DeepSeekChatModel model, String question, List<AppAiMessageDTO> history, Snapshot snap) {
        try {
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(systemPrompt(snap)));
            for (AppAiMessageDTO item : trimHistory(history)) {
                String text = abbreviate(item.getContent(), 400);
                messages.add(ROLE_ASSISTANT.equalsIgnoreCase(item.getRole())
                        ? new AssistantMessage(text)
                        : new UserMessage(text));
            }
            messages.add(new UserMessage(question));

            // 模型/温度/最大 token 全部沿用 spring.ai.deepseek.chat.options 的既有配置，
            // 这里只补一项：JSON 输出模式（省掉「从散文里抠 JSON」的解析地狱）
            ResponseFormat jsonMode = ResponseFormat.builder().type(ResponseFormat.Type.JSON_OBJECT).build();
            DeepSeekChatOptions defaults = model.getOptions();
            DeepSeekChatOptions options = defaults == null
                    ? DeepSeekChatOptions.builder().responseFormat(jsonMode).build()
                    : defaults.mutate().responseFormat(jsonMode).build();

            ChatResponse response = model.call(new Prompt(messages, options));
            String content = response == null || response.getResult() == null || response.getResult().getOutput() == null
                    ? null
                    : response.getResult().getOutput().getText();
            if (!StringUtils.hasText(content)) {
                log.warn("DeepSeek 返回空内容，降级为本地兜底");
                return null;
            }
            return content;
        } catch (Exception e) {
            // 网络抖动/超时/额度用尽都走这里：对顾客来说「小卡回答慢一点」远好过「没有回答」
            log.warn("调用 DeepSeek 失败，降级为本地兜底：{}: {}", e.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }

    private List<AppAiMessageDTO> trimHistory(List<AppAiMessageDTO> history) {
        if (history == null || history.isEmpty()) {
            return new ArrayList<>();
        }
        List<AppAiMessageDTO> valid = history.stream()
                .filter(Objects::nonNull)
                .filter(m -> StringUtils.hasText(m.getContent()))
                .collect(Collectors.toList());
        int max = Math.max(0, aiProperties.getMaxHistory());
        return valid.size() <= max ? valid : new ArrayList<>(valid.subList(valid.size() - max, valid.size()));
    }

    private String systemPrompt(Snapshot snap) {
        int maxProducts = Math.max(1, aiProperties.getMaxProducts());
        int maxCoupons = Math.max(1, aiProperties.getMaxCoupons());
        StringBuilder sb = new StringBuilder(2048);
        sb.append("你是咖啡店「").append(shopName(snap)).append("」的 AI 点单助手，名叫「小卡」。\n");
        sb.append("你只干两件事：① 按顾客的口味/场景从下面的商品清单里推荐饮品或点心；② 告诉顾客有哪些券能领。\n");
        sb.append("\n【硬性规则】\n");
        sb.append("1. 只能推荐清单里出现过的商品 id，绝对不许编造商品名、价格、库存、销量；清单里没有就直说没有。\n");
        sb.append("2. 优惠一律以清单里的券为准，不要自己算折扣、不要承诺清单外的优惠。\n");
        sb.append("3. 不要向顾客索要订单号、手机号、验证码等敏感信息。\n");
        sb.append("4. 语气像咖啡店店员，亲切、口语、简短，回复不超过 60 字，最多一个 emoji。\n");
        sb.append("5. 只输出 JSON 对象，不要输出解释文字，不要用 markdown 代码块包裹。\n");
        sb.append("\n【输出格式】\n");
        sb.append("{\"reply\":\"给顾客的回复\",\"products\":[商品id],\"coupons\":[券id],\"suggestions\":[\"追问1\",\"追问2\"]}\n");
        sb.append("products 最多 ").append(maxProducts).append(" 个、coupons 最多 ").append(maxCoupons)
                .append(" 个，没有就给空数组；suggestions 给 2~3 条中文追问、每条不超过 12 个字。\n");
        sb.append("顾客只是闲聊或与点单无关时，products 与 coupons 都给空数组。\n");

        sb.append("\n【在售商品】格式：id|名称|分类|价格|标签|月售|状态\n");
        if (snap.products.isEmpty()) {
            sb.append("（暂无在售商品）\n");
        }
        for (AppProductVO p : snap.products) {
            sb.append(p.getId()).append('|').append(nz(p.getName())).append('|').append(nz(p.getCategoryName()))
                    .append('|').append(priceText(p)).append('|').append(nz(p.getTags()))
                    .append('|').append(p.getSales() == null ? 0 : p.getSales())
                    .append('|');
            if (Boolean.TRUE.equals(p.getSoldOut())) {
                sb.append("已售罄");
            } else if (Boolean.TRUE.equals(p.getLowStock())) {
                sb.append("库存紧张");
            } else {
                sb.append("在售");
            }
            sb.append('\n');
        }

        // 只把「这位顾客现在真的能领」的券放进提示词。
        // 试过把不可领的也列上并标注「已领完」，模型照样会跟顾客说「可以领」——
        // 结果就是页面上按钮写着「已领取」，顾客以为自己在被耍。事实不该由模型决定，
        // 那就从源头让它选不到。
        List<AppCouponVO> claimable = claimableCoupons(snap.coupons, Integer.MAX_VALUE);
        sb.append("\n【这位顾客现在能领的优惠券】格式：id|名称|门槛|优惠|库存\n");
        if (claimable.isEmpty()) {
            sb.append("（这位顾客当前没有可领取的优惠券，不要编造券，也不要承诺优惠）\n");
        }
        for (AppCouponVO c : claimable) {
            sb.append(c.getId()).append('|').append(nz(c.getName())).append('|')
                    .append("满 ").append(money(c.getThresholdAmount())).append(" 元可用").append('|')
                    .append(couponBenefit(c)).append('|')
                    .append(c.getRemaining() == null || c.getRemaining() < 0 ? "不限量" : ("剩 " + c.getRemaining() + " 张"))
                    .append('\n');
        }

        if (snap.shop != null) {
            sb.append("\n【门店信息】");
            sb.append(shopName(snap));
            if (StringUtils.hasText(snap.shop.getBusinessHours())) {
                sb.append("，营业时间 ").append(snap.shop.getBusinessHours());
            }
            if (StringUtils.hasText(snap.shop.getAddress())) {
                sb.append("，地址 ").append(snap.shop.getAddress());
            }
            sb.append("。\n");
        }
        sb.append("\n注意：顾客要「领券」时，你只需在 coupons 里给出券 id，"
                + "小程序的券卡上带「领取」按钮，顾客点一下就领到了，不用你去执行领取。\n");
        return sb.toString();
    }

    /* ==================== 模型输出解析 ==================== */

    /**
     * 解析模型输出。任何一步不合规（不是 JSON、没有 reply）都返回 null → 走兜底，
     * 宁可回复平庸一点，也不要给顾客一句半截话或空白气泡。
     */
    private AppAiChatVO parseModelReply(String content, Snapshot snap) {
        JSONObject node;
        try {
            node = JSONUtil.parseObj(extractJson(content));
        } catch (Exception e) {
            log.warn("模型输出不是合法 JSON，降级为本地兜底：{}", abbreviate(content, 200));
            return null;
        }
        String reply = node.getStr("reply");
        if (!StringUtils.hasText(reply)) {
            log.warn("模型输出缺少 reply 字段，降级为本地兜底：{}", abbreviate(content, 200));
            return null;
        }

        AppAiChatVO vo = new AppAiChatVO();
        vo.setReply(abbreviate(reply.trim(), REPLY_LIMIT));
        vo.setSource(SOURCE_AI);
        vo.setProducts(pickProducts(node, snap));
        vo.setCoupons(pickCoupons(node, snap));
        vo.setSuggestions(pickSuggestions(node));
        return vo;
    }

    /** JSON 模式下正常不带围栏，但模型偶尔仍会裹一层，兜一手 */
    private String extractJson(String content) {
        String text = content == null ? "" : content.trim();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        return start >= 0 && end > start ? text.substring(start, end + 1) : text;
    }

    private List<AppProductVO> pickProducts(JSONObject node, Snapshot snap) {
        Map<Long, AppProductVO> index = snap.products.stream()
                .collect(Collectors.toMap(AppProductVO::getId, p -> p, (a, b) -> a, LinkedHashMap::new));
        List<AppProductVO> list = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (Long id : readIds(node.get("products"))) {
            AppProductVO hit = index.get(id);
            // 模型编出来的 id 直接丢弃：卡片的每个字段都必须是库里的真实值
            if (hit == null || !seen.add(id)) {
                continue;
            }
            list.add(hit);
            if (list.size() >= Math.max(1, aiProperties.getMaxProducts())) {
                break;
            }
        }
        return list;
    }

    private List<AppCouponVO> pickCoupons(JSONObject node, Snapshot snap) {
        Map<Long, AppCouponVO> index = snap.coupons.stream()
                .collect(Collectors.toMap(AppCouponVO::getId, c -> c, (a, b) -> a, LinkedHashMap::new));
        List<AppCouponVO> list = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (Long id : readIds(node.get("coupons"))) {
            AppCouponVO hit = index.get(id);
            // 双保险：提示词已经只列可领的券，这里再拦一次不可领的。
            // 一旦把「已领完」的券发出去，页面按钮是「已领取」，等于小卡当面说谎。
            if (hit == null || !Boolean.TRUE.equals(hit.getCanClaim()) || !seen.add(id)) {
                continue;
            }
            list.add(hit);
            if (list.size() >= Math.max(1, aiProperties.getMaxCoupons())) {
                break;
            }
        }
        return list;
    }

    private List<AppAiSuggestionVO> pickSuggestions(JSONObject node) {
        List<AppAiSuggestionVO> list = new ArrayList<>();
        Object raw = node.get("suggestions");
        if (!(raw instanceof JSONArray)) {
            return list;
        }
        JSONArray array = (JSONArray) raw;
        for (Object item : array) {
            String text = item == null ? "" : String.valueOf(item).trim();
            if (!text.isEmpty() && text.length() <= 20 && !listContains(list, text)) {
                list.add(new AppAiSuggestionVO(iconOf(text), text));
            }
            if (list.size() >= 3) {
                break;
            }
        }
        return list;
    }

    private boolean listContains(List<AppAiSuggestionVO> list, String text) {
        return list.stream().anyMatch(s -> text.equals(s.getText()));
    }

    /** 把 JSON 里可能是 [1,2] / ["1","2"] / null 的字段统一读成 id 列表 */
    private List<Long> readIds(Object raw) {
        List<Long> ids = new ArrayList<>();
        if (!(raw instanceof JSONArray)) {
            return ids;
        }
        for (Object item : (JSONArray) raw) {
            if (item == null) {
                continue;
            }
            try {
                ids.add(Long.valueOf(String.valueOf(item).trim()));
            } catch (NumberFormatException e) {
                // 模型有时会回商品名而不是 id，这里静默丢掉（名字->id 的反查成本高且易错）
                log.debug("模型返回了非数字 id，已忽略：{}", item);
            }
        }
        return ids;
    }

    /* ==================== 本地兜底链路 ==================== */

    private AppAiChatVO localReply(String question, Snapshot snap) {
        AppAiChatVO vo = new AppAiChatVO();
        vo.setSource(SOURCE_LOCAL);

        if (question.isEmpty()) {
            // 空输入不是真实场景（前端也不会发），当作 ping 处理：
            // 只给引导语，不塞卡片 —— 页面此刻正处在空态，本来就有一排建议问法
            vo.setReply("我在～想喝点什么？说说口味或者现在的状态，我给你挑。");
            return vo;
        }

        List<AppCouponVO> claimable = claimableCoupons(snap.coupons, Math.max(1, aiProperties.getMaxCoupons()));

        // 1) 领券意图
        if (isCouponIntent(question)) {
            if (!claimable.isEmpty()) {
                StringBuilder sb = new StringBuilder("现在能领 ");
                sb.append(claimable.size()).append(" 张：");
                sb.append(claimable.stream().map(AppCouponVO::getName).collect(Collectors.joining("、")));
                sb.append("。点卡片上的「领取」，下单时自动抵扣。");
                vo.setReply(sb.toString());
                vo.setCoupons(claimable);
                return vo;
            }
            vo.setReply("这波券你都领过啦。下单时在结算页会自动挑最划算的那张。");
            vo.setProducts(topSellable(snap.products, 1));
            return vo;
        }

        // 2) 商品推荐：先按字词打分命中，命中不了再如实说 + 给热销
        List<AppProductVO> hit = matchProducts(question, snap.products)
                .stream().limit(Math.max(1, aiProperties.getMaxProducts())).collect(Collectors.toList());
        boolean fuzzy = hit.isEmpty();
        vo.setProducts(fuzzy ? topSellable(snap.products, Math.max(1, aiProperties.getMaxProducts())) : hit);

        StringBuilder sb = new StringBuilder();
        if (fuzzy) {
            sb.append("我按热销给你排了几款，看看有没有对味的：");
            sb.append(vo.getProducts().stream().map(AppProductVO::getName).collect(Collectors.joining("、")));
            sb.append("。");
        } else {
            sb.append("按你说的挑了 ").append(vo.getProducts().size()).append(" 款：");
            sb.append(hit.stream().map(AppProductVO::getName).collect(Collectors.joining("、")));
            sb.append("。").append(reasonOf(hit.get(0)));
        }
        // 顺手带一张能领的券，把「推荐」和「领券」串成一步
        List<AppCouponVO> crossSell = claimableCoupons(snap.coupons, 1);
        if (!crossSell.isEmpty()) {
            sb.append("另外「").append(crossSell.get(0).getName()).append("」现在还能领，一起用更划算。");
            vo.setCoupons(crossSell);
        }
        vo.setReply(abbreviate(sb.toString(), REPLY_LIMIT));
        return vo;
    }

    /**
     * 字词命中打分：
     *   查询切成 2-gram + 意图词典候选词，分别按「名称/标签/分类/描述」加权，
     *   名称命中权重最高（顾客说「拿铁」，命中的就是名字里带「拿铁」的）。
     *   分数不足 STRONG_SCORE 视为没把握，交给调用方兜底热销，不硬凑。
     */
    private List<AppProductVO> matchProducts(String question, List<AppProductVO> products) {
        Set<String> terms = queryTerms(question);
        terms.addAll(intentKeywords(question));
        if (terms.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, Integer> scores = new HashMap<>();
        for (AppProductVO p : products) {
            String name = nz(p.getName());
            String tags = nz(p.getTags());
            String category = nz(p.getCategoryName());
            String description = nz(p.getDescription());
            int score = 0;
            for (String term : terms) {
                int weight = Math.max(2, term.length());
                if (name.contains(term)) {
                    score += weight * 3;
                }
                if (tags.contains(term)) {
                    score += weight * 2;
                }
                if (category.contains(term)) {
                    score += weight * 2;
                }
                if (description.contains(term)) {
                    score += weight;
                }
            }
            if (score > 0) {
                scores.put(p.getId(), score);
            }
        }
        if (scores.isEmpty()) {
            return new ArrayList<>();
        }
        return products.stream()
                .filter(p -> scores.getOrDefault(p.getId(), 0) >= STRONG_SCORE)
                .sorted(Comparator
                        .comparingInt((AppProductVO p) -> scores.getOrDefault(p.getId(), 0)).reversed()
                        .thenComparing(Comparator.comparingInt(
                                (AppProductVO p) -> p.getSales() == null ? 0 : p.getSales()).reversed()))
                .collect(Collectors.toList());
    }

    private Set<String> queryTerms(String question) {
        Set<String> terms = new LinkedHashSet<>();
        String text = question == null ? "" : question.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", "");
        for (int i = 0; i + 2 <= text.length(); i++) {
            terms.add(text.substring(i, i + 2));
        }
        return terms;
    }

    private Set<String> intentKeywords(String question) {
        Set<String> keywords = new LinkedHashSet<>();
        String text = question == null ? "" : question;
        // 「不苦」里含「苦」，先识别否定再逐条判断，否则会把忌苦的顾客推去喝美式
        boolean avoidBitter = text.contains("不苦") || text.contains("别苦") || text.contains("不要太苦")
                || text.contains("怕苦") || text.contains("有点苦");
        for (Map.Entry<String, List<String>> entry : INTENTS.entrySet()) {
            for (String trigger : entry.getKey().split(",")) {
                if (trigger.isEmpty() || ("苦".equals(trigger) && avoidBitter)) {
                    continue;
                }
                if (text.contains(trigger)) {
                    keywords.addAll(entry.getValue());
                    break;
                }
            }
        }
        return keywords;
    }

    private boolean isCouponIntent(String question) {
        String text = question == null ? "" : question;
        return COUPON_WORDS.stream().anyMatch(text::contains);
    }

    private String reasonOf(AppProductVO product) {
        if (product.getTagList() != null && !product.getTagList().isEmpty()) {
            return "「" + product.getName() + "」是" + product.getTagList().get(0) + "，"
                    + priceText(product) + "。点卡片看详情、选规格下单。";
        }
        return "「" + product.getName() + "」在" + nz(product.getCategoryName()) + "里卖得不错，"
                + priceText(product) + "。点卡片看详情。";
    }

    /* ==================== 小工具 ==================== */

    private List<AppCouponVO> claimableCoupons(List<AppCouponVO> coupons, int limit) {
        return coupons.stream()
                .filter(c -> Boolean.TRUE.equals(c.getCanClaim()))
                .limit(Math.max(1, limit))
                .collect(Collectors.toList());
    }

    /** 可售商品按销量倒序取前 n（售罄的排最后，避免把「已售罄」推给顾客） */
    private List<AppProductVO> topSellable(List<AppProductVO> products, int limit) {
        return products.stream()
                .sorted(Comparator
                        .comparing((AppProductVO p) -> Boolean.TRUE.equals(p.getSoldOut()))
                        .thenComparing(Comparator.comparingInt(
                                (AppProductVO p) -> p.getSales() == null ? 0 : p.getSales()).reversed()))
                .limit(Math.max(1, limit))
                .collect(Collectors.toList());
    }

    private AppProductVO firstSellable(List<AppProductVO> products) {
        List<AppProductVO> list = topSellable(products, 1);
        return list.isEmpty() ? null : list.get(0);
    }

    private String couponBenefit(AppCouponVO coupon) {
        if (coupon.getType() != null && coupon.getType() == 2 && coupon.getDiscountRate() != null) {
            return "打 " + money(coupon.getDiscountRate()) + " 折";
        }
        return "减 " + money(coupon.getDiscountAmount()) + " 元";
    }

    private String priceText(AppProductVO product) {
        String min = money(product.getMinPrice());
        String max = money(product.getMaxPrice());
        if (min.equals(max)) {
            return "¥" + min;
        }
        return "¥" + min + "~" + max;
    }

    private String shopName(Snapshot snap) {
        return snap.shop == null || !StringUtils.hasText(snap.shop.getName()) ? "本店" : snap.shop.getName();
    }

    private String money(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String iconOf(String text) {
        if (text.contains("券") || text.contains("优惠") || text.contains("折扣") || text.contains("省")) {
            return "🎟️";
        }
        if (text.contains("点心") || text.contains("甜点") || text.contains("面包")
                || text.contains("烘焙") || text.contains("可颂") || text.contains("蛋糕")) {
            return "🥐";
        }
        if (text.contains("清爽") || text.contains("解腻") || text.contains("冰") || text.contains("果")) {
            return "🧊";
        }
        if (text.contains("热") || text.contains("暖")) {
            return "🍵";
        }
        if (text.contains("提神") || text.contains("苦") || text.contains("美式") || text.contains("浓")) {
            return "☕";
        }
        if (text.contains("甜") || text.contains("奶")) {
            return "🍰";
        }
        if (text.contains("招牌") || text.contains("新品") || text.contains("推荐")) {
            return "⭐";
        }
        return "💬";
    }

    private String abbreviate(String text, int limit) {
        if (text == null) {
            return "";
        }
        String value = text.trim();
        return value.length() <= limit ? value : value.substring(0, limit - 1) + "…";
    }

    private String nz(String value) {
        return value == null ? "" : value;
    }

    private boolean hasText(List<?> list) {
        return list != null && !list.isEmpty();
    }

    private int size(List<?> list) {
        return list == null ? 0 : list.size();
    }

    /** 一次对话的只读数据快照 */
    private static class Snapshot {
        private Shop shop;
        private List<AppProductVO> products = new ArrayList<>();
        private List<AppCouponVO> coupons = new ArrayList<>();
    }
}
