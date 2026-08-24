package com.travel.backend.constants;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;


/**
 * 类描述：当前登录人信息
 *
 * @author luokun
 * @data 2019-07-23
 */
@Data
@ToString
public class CurrentUser {

    public static final int CURRENT_USER_MODEL = 1;

    @Schema(description = "用户id")
    private Integer userId;

    @Schema(description = "用户openid")
    private String openid;

    @Schema(description = "用户唯一unionid")
    private String unionid;

    @Schema(description = "sessionKey")
    private String sessionKey;
}
