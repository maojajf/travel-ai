package com.travel.backend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @Author liuxy
 * @Description 可以拿到多线程执行时候的异常日志，
 * @Date 2021/4/2
 * @Param
 **/

@Component
public class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    @Override
    public void uncaughtException(Thread t, Throwable e) {

        log.error("{}:run Exception :{}",t.getName(),e);

    }
}
