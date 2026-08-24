package com.travel.backend.domain.vo.wechat;

import com.alibaba.fastjson2.annotation.JSONField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author: Mao
 * @CreateTime: 2024/9/9 22:25
 * @Description:
 */
@Data
@Schema(description = "微信openIdDTO")
public class WechatOpenIdVO implements Serializable {

    @Schema(description = "会话密钥")
    @JSONField(name = "session_key")
    private String sessionKey;

    @Schema(description = "用户唯一标识")
    @JSONField(name = "openid")
    private String openid;

    @Schema(description = "用户在开放平台的唯一标识符")
    @JSONField(name = "unionid")
    private String unionid;

    @Schema(description = "错误码")
    private Integer errcode = 0;

    @Schema(description = "错误信息")
    private String errmsg;
}
