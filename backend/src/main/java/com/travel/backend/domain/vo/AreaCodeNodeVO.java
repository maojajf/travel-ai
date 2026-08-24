package com.travel.backend.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author 彭盈
 * @date 2022年05月06日 18:18
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder
@Schema(description = "省市区结果")
public class AreaCodeNodeVO {

    @Schema(description = "名称")
    private String name;

    @Schema(description = "唯一编码")
    private String code;

    @Schema(description = "父级code")
    private String pcode;

    @Schema(description = "精度")
    private BigDecimal lng;

    @Schema(description = "纬度")
    private BigDecimal lat;

    @Schema(description = "等级 1-省 2-市 3-区")
    private Integer level;

    @Schema(description = "权重-正序")
    private Integer weight;

    @Schema(description = "子集")
    private List<AreaCodeNodeVO> children;
}
