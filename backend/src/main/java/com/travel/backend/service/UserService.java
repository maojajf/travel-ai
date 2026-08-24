package com.travel.backend.service;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.travel.backend.domain.SessionBean;
import com.travel.backend.domain.SessionHolder;
import com.travel.backend.domain.dto.UserLoginDTO;
import com.travel.backend.domain.model.User;
import com.travel.backend.domain.vo.wechat.WechatOpenIdVO;
import com.travel.backend.exception.ServiceException;
import com.travel.backend.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Mao
 * @time 2022-05-20
 */
@Service
@Slf4j
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    @Lazy
    private WeChatService weChatApi;

    @Value("${wx.appid:wxdb0ea2c800e9e1d6}")
    private String appId;

    @Value("${wx.secret:694210da254b71f14d70d6147e6870ba}")
    private String secret;

    @Autowired
    @Lazy
    private SessionHolder sessionHolder;

    /**
     * 根据userid查询openid
     * @param userId
     * @return
     */
    public String getOpenidByUserId(Integer userId) {
        Wrapper<User> queryUser = new QueryWrapper<User>().lambda()
                .eq(User::getStatus, 1)
                .eq(User::getId, userId);
        User user = userMapper.selectOne(queryUser,false);
        if(ObjectUtil.isEmpty( user)){
            return  null;
        }
        return user.getUopenid();
    }

    /**
     * 根据userid查询sessionKey
     * @param userId
     * @return
     */
    public String getSessionkeyByUserId(Integer userId) {
        Wrapper<User> queryUser = new QueryWrapper<User>().lambda()
                .eq(User::getStatus, 1)
                .eq(User::getId, userId);
        User user = userMapper.selectOne(queryUser,false);
        if(ObjectUtil.isEmpty( user)){
            return  null;
        }
        return user.getSessionKey();
    }

    /**
     * 登录、注册微信端用户
     * @param dto
     * @param deviceId
     * @return
     */
    public String regOrLogin(UserLoginDTO dto, String deviceId) {
        // 获取openid和unionid
        String weChatAppletInfo = this.getWeChatAppletInfo(appId, secret,
                dto.getJsCode());
        WechatOpenIdVO wechatOpenIdvo = JSONObject.parseObject(weChatAppletInfo, WechatOpenIdVO.class);
        Integer errCode = wechatOpenIdvo.getErrcode();
        if (ObjectUtil.notEqual(0, errCode.intValue())) {
            log.error("getWeChatAppletInfo error, code: {}; errmsg: {}", wechatOpenIdvo.getErrcode(),
                    wechatOpenIdvo.getErrmsg());
            throw new ServiceException("获取应用openId失败,请稍后重试");
        }

        var user = userMapper.getByUopenid(wechatOpenIdvo.getOpenid());
        if (ObjectUtil.isEmpty( user)) {
            // 注册
            user = initNewUser(wechatOpenIdvo, dto.getExtendJson());

        }
        user.setSessionKey(wechatOpenIdvo.getSessionKey());
        user.setLastLoginDate(new Date());

        String token = userLoginPost( deviceId, user);
        return token;
    }

    private User initNewUser(WechatOpenIdVO wechatOpenIdvo, UserLoginDTO.UserExtendInfoVO extendJson) {
        User userEntity = new User();
        // 注册本地用户, 填充微信unionId等数据
        userEntity.setUnionid(wechatOpenIdvo.getUnionid())
                .setSessionKey(wechatOpenIdvo.getSessionKey())
                .setUopenid(wechatOpenIdvo.getOpenid()).setLastLoginDate(new Date());

        userMapper.insert(userEntity);
        return userEntity;
    }

    private String getWeChatAppletInfo(String appid, String appSecret, String jsCode) {
        Map<String, Object> reqMap = new HashMap<>();
        reqMap.put("appid", appid);
        reqMap.put("secret", appSecret);
        reqMap.put("js_code", jsCode);
        reqMap.put("grant_type", "authorization_code");

        String resJson = weChatApi.getAppletUnionIdAndOpenid(reqMap);
        return resJson;
    }

    private String userLoginPost(String deviceId, User user) {
        SessionBean sessionBean = new SessionBean();
        sessionBean.setCode(deviceId);
        sessionBean.setUserId(user.getId());
        sessionBean.setUnionid(user.getUnionid());
        sessionBean.setOpenid(user.getUopenid());
        sessionBean.setSessionKey(user.getSessionKey());

        var token = sessionHolder.createSession(deviceId, user.getId(), sessionBean);
        log.info("userLogin done : userId = {}, deviceId = {}, token = {}", user.getId(), deviceId, token);
        return token;
    }
}
