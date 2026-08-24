package com.travel.backend.interceptor;

import com.travel.backend.constants.Authority;
import com.travel.backend.constants.CurrentUser;
import com.travel.backend.constants.HeaderNames;
import com.travel.backend.constants.InvokerContext;
import com.travel.backend.domain.ResultCode;
import com.travel.backend.domain.SessionBean;
import com.travel.backend.domain.SessionHolder;
import com.travel.backend.exception.ServiceException;
import com.travel.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;


/**
 * 类描述：拦截器
 *
 * @author luokun
 * @data 2019-07-24
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(LoginInterceptor.class);

    @Autowired
    private SessionHolder sessionHolder;

    @Autowired
    private UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HandlerMethod method;
        try {
            method = (HandlerMethod) handler;
        } catch (Exception e) {
            return true;
        }
        Authority auth = method.getMethodAnnotation(Authority.class);
        // 不配，默认就是不需要鉴权
        if (auth == null) {
            return true;
        }

        Authority.NeedLogon needLogon = auth.needLogon();

        logger.info("auth:{}, method:{}", auth, method);

        var token = request.getHeader(HeaderNames.X_AUTH_TOKEN);
        var code = request.getHeader(HeaderNames.X_DEVICE_ID);

        logger.debug("The api request comes.needLogon={} token={}", needLogon, token);

        // 获取请求数据
        SessionBean session = sessionHolder.getSession(token, code);
        if (needLogon == Authority.NeedLogon.YES && session == null) {
            var rc = ResultCode.UNAUTHORIZED;
            throw new ServiceException(rc.getMessage(), rc.getCode());
        }

        if (session != null) {
        	
        	if (StringUtils.isBlank(session.getOpenid())) {
        		
        		session.setOpenid(userService.getOpenidByUserId(session.getUserId()));
        		sessionHolder.createSession(code, session.getUserId(), session, token);
        	}
            if (StringUtils.isBlank(session.getSessionKey())) {

                session.setSessionKey(userService.getSessionkeyByUserId(session.getUserId()));
                sessionHolder.createSession(code, session.getUserId(), session, token);
            }
            CurrentUser user = this.analysisToken(session);
            //保存user信息到当前线程
            InvokerContext.setUserCtx(user);
        }

        return true;
    }

    private CurrentUser analysisToken(SessionBean sess) {
        CurrentUser currentUser = new CurrentUser();
        currentUser.setUserId(sess.getUserId());
        currentUser.setOpenid(sess.getOpenid());
        currentUser.setUnionid(sess.getUnionid());
        currentUser.setSessionKey(sess.getSessionKey());
        return currentUser;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) throws Exception {
        // TODO Auto-generated method stub

    }


    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        //在DispatcherServlet完全处理完请求后被调用，可用于清理资源等。返回处理（已经渲染了页面）
        InvokerContext.clear();
    }

}