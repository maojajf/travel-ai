package com.travel.backend.config;

import com.travel.backend.constants.HeaderNames;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Swagger2的接口配置
 *
 * @author wecard
 */
@Configuration
public class SwaggerConfig
{

    private static final List<String> list = new ArrayList<>();

    static {
        list.add("/login");
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                // 配置接口文档基本信息
                .info(this.getApiInfo())
                .components(new Components()
                                .addSecuritySchemes(HeaderNames.X_AUTH_TOKEN,
                                        new SecurityScheme()
                                                .name(HeaderNames.X_AUTH_TOKEN)
                                                .in(SecurityScheme.In.HEADER)
                                                .description("用户凭证")
                                ).addSecuritySchemes(HeaderNames.X_DEVICE_ID,
                                        new SecurityScheme()
                                                .name(HeaderNames.X_DEVICE_ID)
                                                .in(SecurityScheme.In.HEADER)
                                                .description("设备id")
                                )
                );
    }

    @Bean
    public GlobalOpenApiCustomizer globalOpenApiCustomizer() {
        return openApi -> {
            // 全局添加鉴权参数
            if (openApi.getPaths() != null) {
                openApi.getPaths().forEach((s, pathItem) -> {
                    // 登录接口/验证码不需要添加鉴权参数
                    if (list.contains(s)) {
                        return;
                    }
                    // 接口添加鉴权参数
                    pathItem.readOperations()
                            .forEach(operation ->
                                    operation.addSecurityItem(new SecurityRequirement().addList(HeaderNames.X_AUTH_TOKEN,HeaderNames.X_DEVICE_ID))
                            );
                });
            }
        };
    }

    private Info getApiInfo() {
        return new Info()
                // 配置文档标题
                .title("SpringBoot3集成Knife4j")
                // 配置文档描述
                .description("SpringBoot3集成Knife4j")
                // 配置作者信息
                .contact(new Contact().name("travel").url("xx"))
                // 配置License许可证信息
                .license(new License().name("Apache 2.0").url("xx"))
                // 概述信息
                .summary("SpringBoot3集成Knife4j")
                .termsOfService("xx")
                // 配置版本号
                .version("2.0");
    }
}
