package com.travel.backend.interceptor;

import cn.hutool.core.util.ObjectUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartRequest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Auther: Mao
 * @Date: 2024/07/22/11:52
 * @Description:
 */
@Aspect
@Component
@Slf4j
public class OperLogAspect {

    private static ThreadLocal<Long> startTime = new ThreadLocal<>();

    @Autowired
    private ObjectMapper mapper;

    @Pointcut("execution(public * com.travel.backend.controller..*.*(..))")
    public void requestLogPointCut() {
    }

    @Before(value = "requestLogPointCut() ")
    public void requestLog(JoinPoint joinPoint) {
        try {
            startTime.set(System.currentTimeMillis());
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = requestAttributes.getRequest();
            // 从 attribution中获取对应的信息
            String method = request.getMethod();
            Object[] args = joinPoint.getArgs();
            List argList = new ArrayList();
            if(!ObjectUtil.isEmpty(args)) {
                // 排除 HttpServletRequest、HttpServletResponse、MultipartFile 不然下面的writeValueAsString会报错
                // 获取请求参数集合并进行遍历拼接
                for (int i = 0; i < args.length; i++) {
                    if (args[i] instanceof HttpServletRequest ||
                            args[i] instanceof HttpServletResponse ||
                            args[i] instanceof MultipartFile ||
                            args[i] instanceof MultipartFile[] ||
                            args[i] instanceof MultipartRequest ||
                            args[i] instanceof MultipartHttpServletRequest) {
                        continue;
                    }

                    if (args[i] != null && args[i].getClass().isArray() &&
                            MultipartFile.class.isAssignableFrom(args[i].getClass().getComponentType())) {
                        continue;
                    }

                    argList.add(args[i]);
                }
                args = argList.toArray();
            }
            Map<String, String> headers = requestParamsMap(request);
            String header = headers.entrySet()
                    .stream()
                    .map(item -> "            " +
                            item.getKey() + ": [" +
                            String.join(";", item.getValue()) + "]")
                    .collect(Collectors.joining("\n"));
            log.info("\n\t"//
                            + "traceId : {}"//
                            + "\n\t" //
                            + "request url : {}"//
                            + "\n\t" //
                            + "HttpMethod : {}"
                            + "\n\t" //
                            + "request path : {}"//
                            + "\n\t" //
                            + "request parameter : {}"//
                            + "\n\t" //
                            + "request header : \n{}"//
                            + "\n\t" //
                            + "thread no : {}"//
                            + "\n\t" //
                            + "out ip : \n{}"//
                            + "\n\t" //
                            + "----------------------------------------------------------", //
                    UUID.randomUUID().toString(),
                    request.getRequestURI(),method, joinPoint.getTarget().getClass().getName() + "." + joinPoint.getSignature().getName(),
                    mapper.writeValueAsString(args), header,
                    Thread.currentThread().getId() + "---" + Thread.currentThread().getName());
            RequestContextHolder.getRequestAttributes().setAttribute("params",mapper.writeValueAsString(args),0);
        } catch (JsonProcessingException e) {
            log.error("{}", e);
        }catch (Exception e) {
            log.error("{}", e);
        }
    }

    @AfterReturning(returning = "response", pointcut = "requestLogPointCut()")
    public void responseLog(Object response) {
        long costTime = 0L;
        try {
            if(ObjectUtil.isNotEmpty(startTime.get())){
                costTime = System.currentTimeMillis() - startTime.get();
            }
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletResponse httpResponse = requestAttributes.getResponse();
            log.info("\n\t"//
                            + "----------------------------------------------------------"//
                            + "\n\t" //
                            + "response parameter : {}"//
                            + "\n\t" //
                            + "thread no : {}"//
                            + "\n\t" //
                            + "response_status : {}"//
                            + "\n\t" //
                            + "cost_time : {} ms"//
                            + "\n\t" //
                            + "----------------------------------------------------------",
                    mapper.writeValueAsString(response),
                    Thread.currentThread().getId() + "---" + Thread.currentThread().getName(),httpResponse.getStatus(),costTime);
            startTime.remove();
        } catch (JsonProcessingException e) {
            log.error("{}", e);
        }
    }

    @AfterThrowing(throwing = "ex", pointcut = "requestLogPointCut()")
    public void responseThrowLog(Throwable ex) {
        long costTime = 0L;
        try {
            if(ObjectUtil.isNotEmpty(startTime.get())){
                costTime = System.currentTimeMillis() - startTime.get();
            }
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletResponse httpResponse = requestAttributes.getResponse();
            log.info("\n\t"//
                            + "----------------------------------------------------------"//
                            + "\n\t" //
                            + "response throwException : {}"//
                            + "\n\t" //
                            + "thread no : {}"//
                            + "\n\t" //
                            + "response_status : {}"//
                            + "\n\t" //
                            + "cost_time : {} ms"//
                            + "\n\t" //
                            + "----------------------------------------------------------",
                    mapper.writeValueAsString(ex.toString()),
                    Thread.currentThread().getId() + "---" + Thread.currentThread().getName(),httpResponse.getStatus(),costTime);
            startTime.remove();
        } catch (JsonProcessingException e) {
            log.error("{}", e);
        }
    }


    private Map<String, String> requestParamsMap(HttpServletRequest request) {
        Enumeration<String> names = request.getHeaderNames();
        Map<String, String> map = new HashMap<>(16);
        while (names.hasMoreElements()) {
            String element = names.nextElement();
            String value = request.getHeader(element);
            map.put(element, value);
        }
        return map;
    }
}
