package com.travel.backend.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 省市区编码
 *
 * @author Sanjeev 252126641@qq.com
 * @since 1.0.0 2024-11-28
 */
@Data
@EqualsAndHashCode(callSuper=false)
@Accessors(chain=true) 
@TableName("area_code")
@JsonInclude(value = Include.NON_NULL)
public class AreaCode  {
	private static final long serialVersionUID = 1L;

	@TableId(value = "id", type = IdType.AUTO)
	private Integer id;

    /**
     * 名称
     */
	private String name;
    /**
     * 等级 1-省 2-市 3-区
     */
	private Integer level;
    /**
     * level对应唯一code
     */
	private String code;
    /**
     * 父级code
     */
	private String pcode;
    /**
     * 权重-正序
     */
	private Integer weight;
    /**
     * 状态 1-正常
     */
	private Integer status;
    /**
     * 经度
     */
	private BigDecimal lng;
    /**
     * 纬度
     */
	private BigDecimal lat;
    /**
     * 备注
     */
	private String remark;
    /**
     * 创建时间
     */
	private Date createTime;
    /**
     * 修改时间
     */
	private Date updateTime;
	/**
	 * 拼音首字母
	 */
	private String pinyinShort;
}