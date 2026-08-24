package com.travel.backend.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "分页入参")
public class PageDTO {

    @Schema(description = "当前页",example = "1")
    @Min(value = 1,message = "当前页数超出限制")
    private Integer page = 1;

    @Schema(description = "每页显示条数 最多填500", example = "10")
    @Max(value = 500,message = "每页显示条数超出限制")
    private Integer pageSize = 10;
}
