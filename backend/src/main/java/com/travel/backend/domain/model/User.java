package com.travel.backend.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * 用户
 *
 * @author Sanjeev 252126641@qq.com
 * @since 1.0.0 2024-11-28
 */
@Data
@EqualsAndHashCode(callSuper=false)
@Accessors(chain=true) 
@TableName("user")
@JsonInclude(value = Include.NON_NULL)
public class User {
	private static final long serialVersionUID = 1L;

	@TableId(value = "id", type = IdType.AUTO)
	private Integer id;

    /**
     * 用户openid
     */
	private String uopenid;
    /**
     * 用户唯一unionid
     */
	private String unionid;
    /**
     * 用户sessionkey
     */
	private String sessionKey;
    /**
     * 用户静默登录标识，1 可静默登录，如用户手动退出，需要改为0
     */
	private Integer silentLogin;
    /**
     * 1.正常 2.冻结 3.其他
     */
	private Integer status;
    /**
     * 最后登录时间
     */
	private Date lastLoginDate;
    /**
     * 创建时间
     */
	private Date createTime;
}