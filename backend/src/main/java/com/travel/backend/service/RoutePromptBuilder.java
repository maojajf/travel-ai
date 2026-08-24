package com.travel.backend.service;

import com.travel.backend.domain.dto.RouteGenerateDTO;
import org.springframework.stereotype.Component;

@Component
public class RoutePromptBuilder {

    public String buildSystemPrompt() {
        return """
            你是一名资深中文旅行路线规划专家。
            你的任务是根据用户提供的省市、时间范围、出行人群和预算档位，生成一份真实、顺路、少折返、可直接执行的旅游路线。
            你只能输出 Markdown，不允许输出解释、前言、JSON、代码块或额外说明。

            你的输出必须严格遵循下面的结构：

            # 城市旅行路线
            - 景点: 城市亮点概览
            - 美食: 必吃方向概览
            - 交通: 整体出行建议
            - 建议: 本次路线的总体提醒

            ## D1 当天主题
            ### 09:00-11:30
            - 景点: ...
            - 美食: ...
            - 交通: ...
            - 建议: ...
            - 玩法: ...

            ### 12:00-13:30
            - 景点: ...
            - 美食: ...
            - 交通: ...
            - 建议: ...
            - 玩法: ...

            ## D2 当天主题
            ### 09:00-11:30
            - 景点: ...
            - 美食: ...
            - 交通: ...
            - 建议: ...
            - 玩法: ...

            ## 出行提醒
            - 建议: ...
            - 建议: ...

            生成要求：
            - 全部内容必须使用简体中文。
            - 一级标题只能出现一次。
            - 二级标题只能使用“## Dn ...”和“## 出行提醒”。
            - 三级标题必须是时间段，例如“### 09:00-11:30”。
            - 列表项必须只使用“景点 / 美食 / 交通 / 建议 / 玩法”五类标签。
            - 每个时间段都必须包含这五类标签，且每类只写一行。
            - 路线要符合用户给定的时间、人群和预算，优先同片区安排，避免明显折返。
            - 景点、美食和玩法尽量贴近当地特色。
            """;
    }

    public String buildFormatGuardPrompt() {
        return """
            这是强制格式规则，必须严格遵守：

            1. 每一个 Markdown 元素都必须从新的一行开始。
            2. 标题写法必须是“# 空格 标题”“## 空格 标题”“### 空格 标题”。
            3. 列表项写法必须是“- 空格 标签: 内容”。
            4. 严禁把标题和列表项写在同一行。
            5. 严禁把多个列表项写在同一行。
            6. 严禁输出代码块、表格、数字列表、HTML 标签。
            7. 若某一段内容尚未想好，也不能破坏 Markdown 结构。

            输出前请自检：
            - 每个 #、##、### 前面是否都有换行或位于文首。
            - 每个 - 前面是否都有换行。
            - #、##、###、- 后面是否都有一个空格。
            - 每个列表项是否都使用“标签: 内容”格式。
            - 是否只使用“景点 / 美食 / 交通 / 建议 / 玩法”标签。
            """;
    }

    public String buildUserPrompt(RouteGenerateDTO request) {
        return """
            请根据以下条件生成一份旅游路线：
            - 省份: %s
            - 城市: %s
            - 出发日期: %s
            - 出发时间: %s
            - 结束日期: %s
            - 结束时间: %s
            - 出行人群: %s
            - 预算档位: %s

            额外要求：
            - 行程安排必须合理，避免明显赶场和折返。
            - 每个时间段必须包含景点、美食、交通、建议、玩法五项。
            - 一级标题后先输出总览列表，再开始按天安排。
            - 输出必须能直接被 Markdown 渲染，不要出现缺少换行或缺少空格的写法。
            - 不要输出“以下是为你生成的路线”之类的说明。
            """.formatted(
            request.getProvince(),
            request.getCity(),
            request.getStartDate(),
            request.getStartTime(),
            request.getEndDate(),
            request.getEndTime(),
            toTravelGroupLabel(request.getTravelGroup()),
            toBudgetLabel(request.getBudgetLevel())
        );
    }

    public String buildSummary(RouteGenerateDTO request, String markdown) {
        String condensed = markdown.replace("\r", "")
            .replace("\n", " ")
            .replace("#", "")
            .replace("-", "")
            .trim();
        if (condensed.length() > 56) {
            condensed = condensed.substring(0, 56) + "...";
        }
        return request.getCity() + "行程已生成：" + condensed;
    }

    private String toTravelGroupLabel(String value) {
        return switch (value) {
            case "solo" -> "单人";
            case "couple" -> "情侣";
            case "family" -> "亲子";
            case "friends" -> "朋友";
            default -> value;
        };
    }

    private String toBudgetLabel(String value) {
        return switch (value) {
            case "low" -> "低预算";
            case "medium" -> "中预算";
            case "high" -> "高预算";
            default -> value;
        };
    }
}
