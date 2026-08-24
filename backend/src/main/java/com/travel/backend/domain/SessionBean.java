package com.travel.backend.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * Created by samlin on 2018/4/12.
 *  公共SessionBean 使用类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class SessionBean {

    @Schema(description = "登录成功后token")
    private String token;

    @Schema(description = "用户openId")
    private String openid;
    @Schema(description = "登录的sessionKey")
    private String sessionKey;

    @Schema(description = "平台用户id")
    private Integer userId;

    @Schema(description = "用户唯一unionid")
    private String unionid;

    @Schema(description = "设备id")
    private String code;
}

