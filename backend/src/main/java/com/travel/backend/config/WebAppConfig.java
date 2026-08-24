package com.travel.backend.config;

import com.travel.backend.interceptor.LogMDCInterceptor;
import com.travel.backend.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * url拦截控制
 * @author clancy
 *
 */
@Configuration
public class WebAppConfig implements WebMvcConfigurer {

    @Autowired
    private LogMDCInterceptor logMDCInterceptor;

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**");

        registry.addInterceptor(logMDCInterceptor)
                .addPathPatterns("/**").
                order(1);
    }
}
