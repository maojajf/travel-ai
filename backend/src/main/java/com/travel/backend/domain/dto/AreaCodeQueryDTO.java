package com.travel.backend.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;

/**
 * @author 彭盈
 * @date 2022年05月06日 18:17
 */
@Data
@Schema(description = "城市查询实体")
@ToString
public class AreaCodeQueryDTO {

    @Schema(description = "等级 1-省 2-市 3-区")
    private Integer level;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "父级code")
    private String pcode;
}


