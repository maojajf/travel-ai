package com.travel.backend.domain.vo.wechat;

import com.alibaba.fastjson2.annotation.JSONField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author zhangpeng
 * @date 2022/9/5 0005
 * @description
 */
@Schema(description = "微信获取唯一授权码参数封装")
@Data
public class WechatAccessTokenResponse extends WechatBaseResponse {

    @Schema(description = "授权码")
    @JSONField(name = "access_token")
    private String accessToken;

    @Schema(description = "授权码有效期 毫秒")
    @JSONField(name = "expires_in")
    private Integer expiresIn;

}
