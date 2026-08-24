package com.travel.backend.config;

import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Mao
 * @time 2022-05-20
 * @decr 配置 分页插件
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public PaginationInnerInterceptor paginationInterceptor() {
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor();
        //mybatis plus默认了最大limit数为500条, 修改成1000
        paginationInterceptor.setMaxLimit(1000L);
        return paginationInterceptor;
    }
}
