package com.travel.backend.service;


import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSONObject;
import com.travel.backend.domain.vo.wechat.WechatAccessTokenResponse;
import com.travel.backend.exception.ServiceException;
import com.travel.backend.fegin.WeChatApi;
import com.travel.backend.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/*
 * @Author liuxy
 * @Description
 * @Date 2022/5/12
 * @Param 微信相关接口调用
 */
@Service
@Slf4j
public class WeChatService {

    @Autowired
    private WeChatApi wxApi;

    // 微信accessToken redis Key
    private final static String WECHAT_APPLET_ACCESS_TOKEN = "wechat:applet:access_token";

    private final static String DELIMITER = ":";

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 获取小程序openid和unionid 数据
     */
    public String getAppletUnionIdAndOpenid(Map<String,Object> paramMap){
        return wxApi.getAppletUnionIdAndOpenid(paramMap);
    }

    /**
     * 通过appId和appSecret获取accessToken
     */
    public String getAccessToken(String appId, String appSecret){
        StringBuffer redisKey = new StringBuffer();
        redisKey.append(WECHAT_APPLET_ACCESS_TOKEN);
        redisKey.append(DELIMITER);
        redisKey.append(appId);

        if(redisUtil.hasKey(redisKey.toString())){
            Object o = redisUtil.get(redisKey.toString());
            if(ObjectUtil.isNotEmpty(o)){
                return o.toString();
            }
        }

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("appid", appId);
        paramMap.put("secret", appSecret);
        paramMap.put("grant_type","client_credential");

        String weChatAccessToken = wxApi.getWeChatAccessToken(paramMap);
        if(StringUtils.isBlank(weChatAccessToken)){
            throw new ServiceException("获取微信accessToken失败");
        }
        WechatAccessTokenResponse wechatAccessTokenResponse = JSONObject.parseObject(weChatAccessToken, WechatAccessTokenResponse.class);
        //存入 redis 中, 多环境处理token兼容问题
        if(wechatAccessTokenResponse.vaild()){
            redisUtil.set(redisKey.toString(), wechatAccessTokenResponse.getAccessToken(), wechatAccessTokenResponse.getExpiresIn()- 60 , TimeUnit.SECONDS);
            return wechatAccessTokenResponse.getAccessToken();
        }

        return null;
    }

    /**
     * 删除缓存中的 redisToken
     */
    public void removeRedisToken(String appId) {
        StringBuffer redisKey = new StringBuffer();
        redisKey.append(WECHAT_APPLET_ACCESS_TOKEN);
        redisKey.append(DELIMITER);
        redisKey.append(appId);
        redisUtil.del(redisKey.toString());

    }

}