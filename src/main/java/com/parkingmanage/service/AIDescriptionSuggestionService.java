package com.parkingmanage.service;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * AI智能违规描述建议服务
 * 根据违规类型名称，智能生成相关的违规描述建议
 *
 * @author MLH
 * @since 2025-10-08
 */
@Service
public class AIDescriptionSuggestionService {

    /**
     * 违规类型关键词库
     */
    private static final Map<String, List<String>> KEYWORD_TEMPLATES = new HashMap<>();

    /**
     * 违规场景模板
     */
    private static final Map<String, List<String>> SCENARIO_TEMPLATES = new HashMap<>();

    /**
     * 违规后果模板
     */
    private static final Map<String, List<String>> CONSEQUENCE_TEMPLATES = new HashMap<>();

    static {
        // 初始化关键词库
        initKeywordTemplates();
        // 初始化场景模板
        initScenarioTemplates();
        // 初始化后果模板
        initConsequenceTemplates();
    }

    /**
     * 根据违规类型名称生成智能建议
     *
     * @param typeName 违规类型名称
     * @return 建议列表
     */
    public List<String> generateSuggestions(String typeName) {
        if (typeName == null || typeName.trim().isEmpty()) {
            return getDefaultSuggestions();
        }

        List<String> suggestions = new ArrayList<>();
        String normalizedTypeName = typeName.trim();

        // 1. 精确匹配：直接匹配预设的违规类型
        List<String> exactMatch = getExactMatchSuggestions(normalizedTypeName);
        if (!exactMatch.isEmpty()) {
            suggestions.addAll(exactMatch);
        }

        // 2. 关键词匹配：根据关键词生成建议
        List<String> keywordMatch = getKeywordMatchSuggestions(normalizedTypeName);
        suggestions.addAll(keywordMatch);

        // 3. AI组合生成：根据类型名称智能组合场景和后果
        List<String> aiGenerated = generateAISuggestions(normalizedTypeName);
        suggestions.addAll(aiGenerated);

        // 4. 去重并限制数量
        Set<String> uniqueSuggestions = new LinkedHashSet<>(suggestions);
        List<String> result = new ArrayList<>(uniqueSuggestions);

        // 最多返回8条建议
        return result.size() > 8 ? result.subList(0, 8) : result;
    }

    /**
     * 获取精确匹配的建议
     */
    private List<String> getExactMatchSuggestions(String typeName) {
        Map<String, List<String>> exactMatches = new HashMap<>();
        
        // 违规停车
        exactMatches.put("违规停车", Arrays.asList(
            "在禁停区域停放车辆",
            "未在指定停车位内停车",
            "车辆停放影响交通通行",
            "在人行道上停放车辆",
            "在出入口附近违规停车"
        ));

        // 占用消防通道
        exactMatches.put("占用消防通道", Arrays.asList(
            "车辆停放在消防通道内",
            "堵塞消防车通行路线",
            "在消防设施周围违规停车",
            "影响消防安全通道畅通"
        ));

        // 占用绿化带
        exactMatches.put("占用绿化带", Arrays.asList(
            "车辆驶入绿化带停放",
            "压占草坪或绿化区域",
            "破坏绿化设施",
            "在花坛周围违规停车"
        ));

        // 占用盲道
        exactMatches.put("占用盲道", Arrays.asList(
            "车辆停放在盲道上",
            "阻碍视障人士通行",
            "占用无障碍通道",
            "影响盲道正常使用"
        ));

        // 超时停车
        exactMatches.put("超时停车", Arrays.asList(
            "超过规定时间停放车辆",
            "在限时停车区域超时",
            "未按时驶离停车位",
            "长时间占用临时停车位"
        ));

        // 未按位停车
        exactMatches.put("未按位停车", Arrays.asList(
            "车辆未停放在划定车位内",
            "跨越停车线停放",
            "占用多个停车位",
            "车辆停放不规范"
        ));

        // 占用他人车位
        exactMatches.put("占用他人车位", Arrays.asList(
            "占用他人专属停车位",
            "停放在已分配的车位",
            "未经许可使用他人车位",
            "侵占固定车位"
        ));

        // 逆向停车
        exactMatches.put("逆向停车", Arrays.asList(
            "车辆逆向停放",
            "未按规定方向停车",
            "车头方向与规定相反"
        ));

        // 占用充电车位
        exactMatches.put("占用充电车位", Arrays.asList(
            "非充电车辆占用充电桩车位",
            "充电完成后未及时驶离",
            "长时间占用充电车位"
        ));

        // 精确匹配
        for (Map.Entry<String, List<String>> entry : exactMatches.entrySet()) {
            if (typeName.equals(entry.getKey())) {
                return entry.getValue();
            }
        }

        return new ArrayList<>();
    }

    /**
     * 根据关键词匹配生成建议
     */
    private List<String> getKeywordMatchSuggestions(String typeName) {
        List<String> suggestions = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : KEYWORD_TEMPLATES.entrySet()) {
            String keyword = entry.getKey();
            if (typeName.contains(keyword)) {
                suggestions.addAll(entry.getValue());
            }
        }

        return suggestions;
    }

    /**
     * AI智能组合生成建议
     */
    private List<String> generateAISuggestions(String typeName) {
        List<String> suggestions = new ArrayList<>();

        // 分析类型名称中的关键信息
        String action = extractAction(typeName);
        String location = extractLocation(typeName);
        String object = extractObject(typeName);

        // 根据提取的信息生成建议
        if (action != null && location != null) {
            suggestions.add(String.format("%s在%s", action, location));
        }

        if (action != null && object != null) {
            suggestions.add(String.format("%s%s", action, object));
        }

        // 添加通用后果描述
        List<String> consequences = CONSEQUENCE_TEMPLATES.getOrDefault("通用", new ArrayList<>());
        if (!consequences.isEmpty()) {
            // 随机选择1-2个后果
            Collections.shuffle(consequences);
            suggestions.addAll(consequences.subList(0, Math.min(2, consequences.size())));
        }

        return suggestions;
    }

    /**
     * 提取动作关键词
     */
    private String extractAction(String typeName) {
        if (typeName.contains("占用")) return "占用";
        if (typeName.contains("停放") || typeName.contains("停车")) return "车辆停放";
        if (typeName.contains("堵塞")) return "堵塞";
        if (typeName.contains("阻碍")) return "阻碍";
        if (typeName.contains("超时")) return "超过规定时间";
        if (typeName.contains("逆向")) return "逆向停放";
        if (typeName.contains("违规")) return "违规停放";
        return null;
    }

    /**
     * 提取位置关键词
     */
    private String extractLocation(String typeName) {
        if (typeName.contains("消防")) return "消防通道";
        if (typeName.contains("绿化")) return "绿化带";
        if (typeName.contains("盲道")) return "盲道";
        if (typeName.contains("人行道")) return "人行道";
        if (typeName.contains("通道")) return "通道";
        if (typeName.contains("车位")) return "车位";
        if (typeName.contains("充电")) return "充电桩车位";
        if (typeName.contains("出入口")) return "出入口";
        return null;
    }

    /**
     * 提取对象关键词
     */
    private String extractObject(String typeName) {
        if (typeName.contains("消防")) return "消防设施";
        if (typeName.contains("绿化")) return "绿化区域";
        if (typeName.contains("通道")) return "通行路线";
        if (typeName.contains("车位")) return "停车位";
        return null;
    }

    /**
     * 获取默认建议
     */
    private List<String> getDefaultSuggestions() {
        return Arrays.asList(
            "车辆停放不符合规定",
            "违反停车管理规定",
            "影响其他车辆或行人通行",
            "未按照规定要求停放车辆"
        );
    }

    /**
     * 初始化关键词模板
     */
    private static void initKeywordTemplates() {
        KEYWORD_TEMPLATES.put("停车", Arrays.asList(
            "车辆停放位置不当",
            "未按规定停放车辆",
            "停车影响正常通行"
        ));

        KEYWORD_TEMPLATES.put("占用", Arrays.asList(
            "非法占用公共区域",
            "占用影响他人使用",
            "长时间占用公共资源"
        ));

        KEYWORD_TEMPLATES.put("消防", Arrays.asList(
            "影响消防安全",
            "妨碍消防救援",
            "存在消防隐患"
        ));

        KEYWORD_TEMPLATES.put("绿化", Arrays.asList(
            "破坏绿化环境",
            "影响小区美观",
            "损坏绿化设施"
        ));

        KEYWORD_TEMPLATES.put("盲道", Arrays.asList(
            "妨碍无障碍通行",
            "影响特殊人群出行",
            "违反无障碍设施管理规定"
        ));

        KEYWORD_TEMPLATES.put("超时", Arrays.asList(
            "超过允许停放时长",
            "违反时间限制规定",
            "影响车位周转率"
        ));

        KEYWORD_TEMPLATES.put("充电", Arrays.asList(
            "影响充电车辆使用",
            "降低充电设施利用率",
            "妨碍新能源车辆充电"
        ));
    }

    /**
     * 初始化场景模板
     */
    private static void initScenarioTemplates() {
        SCENARIO_TEMPLATES.put("禁停区域", Arrays.asList(
            "在禁停标志区域停放",
            "在禁止停车路段停放",
            "在限制停车区域违规停放"
        ));

        SCENARIO_TEMPLATES.put("通道区域", Arrays.asList(
            "堵塞通行路线",
            "影响车辆进出",
            "阻碍正常通行"
        ));

        SCENARIO_TEMPLATES.put("特殊区域", Arrays.asList(
            "在特殊保护区域停放",
            "占用专用区域",
            "侵占公共设施"
        ));
    }

    /**
     * 初始化后果模板
     */
    private static void initConsequenceTemplates() {
        CONSEQUENCE_TEMPLATES.put("通用", Arrays.asList(
            "影响小区环境秩序",
            "妨碍其他业主正常使用",
            "存在安全隐患",
            "违反物业管理规定",
            "影响小区整体形象",
            "造成交通不便"
        ));
    }
}
