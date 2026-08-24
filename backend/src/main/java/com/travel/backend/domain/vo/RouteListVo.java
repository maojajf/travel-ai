package com.travel.backend.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "路线列表项")
public class RouteListVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "id")
    private String routeId;

    @Schema(description = "省")
    private String province;

    @Schema(description = "市")
    private String city;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "出行人群")
    private String travelGroup;

    @Schema(description = "预算档位")
    private String budgetLevel;

    @Schema(description = "开始日期")
    private String startAt;

    @Schema(description = "结束日期")
    private String endAt;

    @Schema(description = "是否收藏")
    private boolean favorite = false;

    @Schema(description = "创建时间")
    private String createdAt;

    @Schema(description = "Markdown正文")
    private String contentMarkdown;
}
