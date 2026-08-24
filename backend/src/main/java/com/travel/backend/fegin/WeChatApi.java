package com.travel.backend.fegin;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/*
 * @Author liuxy
 * @Description
 * @Date 2025/1/23
 * @Param 微信相关接口调用
 */

@FeignClient(name = "weChatHttp", url = "https://api.weixin.qq.com/")
public interface WeChatApi {



    /**
     * 获取小程序openid和unionid 数据
     * 官方文档：https://developers.weixin.qq.com/miniprogram/dev/OpenApiDoc/user-login/code2Session.html
     * @param paramMap
     * @return
     */
    @GetMapping(value = "/sns/jscode2session",consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    String getAppletUnionIdAndOpenid(@RequestParam Map<String,Object> paramMap);

    /**
     * 获取微信token
     * 官方文档：https://developers.weixin.qq.com/miniprogram/dev/OpenApiDoc/mp-access-token/getAccessToken.html
     * @param paramMap
     * @return
     */
    @GetMapping(value = "/cgi-bin/token")
    String getWeChatAccessToken(@RequestParam Map<String,Object> paramMap);

}
