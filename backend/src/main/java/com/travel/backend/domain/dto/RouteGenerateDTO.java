package com.travel.backend.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RouteGenerateDTO {

    @Schema(description = "省")
    @NotBlank
    private String province;

    @Schema(description = "市")
    @NotBlank
    private String city;

    @Schema(description = "开始日期")
    @NotBlank
    private String startDate;

    @Schema(description = "开始时间")
    @NotBlank
    private String startTime;

    @Schema(description = "结束日期")
    @NotBlank
    private String endDate;

    @Schema(description = "结束时间")
    @NotBlank
    private String endTime;

    @Schema(description = "出游人群")
    @NotBlank
    private String travelGroup;

    @Schema(description = "预算档位")
    @NotBlank
    private String budgetLevel;

}
