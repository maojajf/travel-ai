package com.travel.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;


@SpringBootApplication
@EnableFeignClients(basePackages = "com.travel")
@EnableAsync
public class TravelBackendApplication {

    private static final Logger logger = LoggerFactory.getLogger(TravelBackendApplication.class);


    public static void main(String[] args) {
        SpringApplication.run(TravelBackendApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx, @Value("${spring.application.name}") String projectName) {
        return args -> {
            logger.info("项目: {} 启动成功", projectName);
        };
    }
}
