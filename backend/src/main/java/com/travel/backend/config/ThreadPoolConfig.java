package com.travel.backend.config;

import com.travel.backend.exception.UncaughtExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置
 *
 * @author sxh
 **/
@Slf4j
@Configuration
public class ThreadPoolConfig {

    @Autowired
    private UncaughtExceptionHandler uncaughtExceptionHandler;

    public static final int DEFAULT_QUARTZ_QUEUE_CAPACITY = 2000;

    private int keepAliveTime = 60;

    @Bean(name = "threadPoolTaskExecutor")
    public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 设置核心线程数
        int cpuNum = Runtime.getRuntime().availableProcessors();// 获取处理器数量
        executor.setCorePoolSize(cpuNum * 2 );
        // 设置最大线程数
        executor.setMaxPoolSize(cpuNum * 5);
        // 默认的队列容量
        executor.setQueueCapacity(DEFAULT_QUARTZ_QUEUE_CAPACITY);
        // 设置活跃时间-/秒
        executor.setKeepAliveSeconds(keepAliveTime);
        // 线程名
        //executor.setThreadNamePrefix("serviceIoExecutor-");
        // 设置拒绝策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);

        //设置线程名字和异常栈进日志，默认是不会写入日志的
        ThreadFactory executorThreadFactory = new BasicThreadFactory.Builder()
                .namingPattern("threadPoolTaskExecutor-pool-%d")
                .uncaughtExceptionHandler(uncaughtExceptionHandler)
                .build();
        executor.setThreadFactory(executorThreadFactory);

        executor.initialize();
        log.info("threadPoolTaskExecutor start success ");
        return executor;
    }

}
