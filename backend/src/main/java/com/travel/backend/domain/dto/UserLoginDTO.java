package com.travel.backend.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author: Mao
 * @CreateTime: 2024/10/9 17:48
 */
@Data
@Schema(description = "用户登录DTO")
public class UserLoginDTO  implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "微信授权获取用户openid的code")
    @NotBlank
    private String jsCode;

    @Schema(description = "扩展业务json,可传用户信息")
    private UserExtendInfoVO extendJson ;

    @Data
    public static class UserExtendInfoVO{

        @Schema(description ="用户微信名")
        private String uname;

        @Schema(description ="用户性别  0未知 1男 2女")
        private int ugender = 0;

        @Schema(description = "用户地址")
        private String uaddress;

        @Schema(description = "头像")
        private String uavatar;
    }
}
