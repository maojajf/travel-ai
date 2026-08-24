package com.travel.backend.domain;


import cn.hutool.core.util.ObjectUtil;
import com.travel.backend.util.JWTUtil;
import com.travel.backend.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;


/**
 * @author samlin
 * @date 2019/07/22
 * in the future this show put to cache like redis
 */
@Component("sessionHolder")
@Slf4j
public class SessionHolder {

    @Autowired
    private RedisUtil redisUtil;

    // 15天,秒为单位
    private final static long FIFTEEN_DAY = 3600 * 24 * 15;

    // 1天,秒为单位
    private final static long ONE_DAY = 3600 * 24;

    private final String SESSION_TOKEN = "session:token:";

    private final String SESSION_UID = "session:uid:";

    public boolean hasSession(String token, String code) {
        var sessionTokenKey = createSessionTokenKey(token, code);
        return redisUtil.hasKey(sessionTokenKey);
    }

    /**
     * 获取session信息
     * @param token
     * @param code
     * @return
     */
    public SessionBean getSession(String token, String code) {
        var sessionTokenKey = createSessionTokenKey(token, code);
        SessionBean sessionBean = (SessionBean)redisUtil.get(sessionTokenKey);
        log.info("sessionBean:{}", sessionBean);
        return sessionBean;
    }

    public String createSession(String code, Integer userId, SessionBean sessionBean) {
    	return createSession(code, userId, sessionBean, null);
    }
    public String createSession(String code, Integer userId, SessionBean sessionBean, String token) {
        // jwt 生成token
        token = StringUtils.isBlank(token)? JWTUtil.createDefaultAccessToken(userId.toString(), code) : token;
      //  String token = UUID.randomUUID().toString();
        String sessionKey = createSessionTokenKey(token, code);
        redisUtil.set(sessionKey, sessionBean, FIFTEEN_DAY, TimeUnit.SECONDS);

        var sessionUid = createSessionUid(userId, code);
        redisUtil.sSet(sessionUid, token,FIFTEEN_DAY, TimeUnit.SECONDS);

        return token;
    }

    /**
     * 删除token信息
     * @param token
     * @param code
     */
    public void destroySession(String token, String code) {
        if (StringUtils.isNotBlank(token) && StringUtils.isNotBlank(code)) {
            String sessionTokenKey = createSessionTokenKey(token, code);
            SessionBean sessionBean = (SessionBean)redisUtil.get(sessionTokenKey);
            if (ObjectUtil.isNotNull(sessionBean)) {
                var sessionUid = createSessionUid(sessionBean.getUserId(), code);
                redisUtil.del(sessionUid);
            }
            redisUtil.del(sessionTokenKey);
            log.info("destroy session for token:{}", token);
        }
    }

    /**
     * 更新 token 信息
     */
    public void updateSession(String token, String code, SessionBean sessionBean) {
        String sessionKey = createSessionTokenKey(sessionBean.getToken(), sessionBean.getCode());
        redisUtil.set(sessionKey, sessionBean, FIFTEEN_DAY, TimeUnit.SECONDS);
    }


    public String createSessionTokenKey(String token, String code) {
        logTokenAndCode(token, code);
        String sessionKey = SESSION_TOKEN + code + ":" + token;
        return sessionKey;
    }

    public String createSessionUid(Integer userId, String code) {
        String sessionUid = SESSION_UID + code + ":" + userId;
        return sessionUid;
    }

    private static void logTokenAndCode(String token, String code) {
        log.info("token:{}, code:{}", token, code);
    }
}

