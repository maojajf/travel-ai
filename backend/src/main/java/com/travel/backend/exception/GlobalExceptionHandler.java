package com.travel.backend.exception;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.travel.backend.domain.R;
import com.travel.backend.util.Convert;
import com.travel.backend.util.EscapeUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.internal.engine.ConstraintViolationImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 全局异常处理器
 * 
 * @author sxh
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 请求方式不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public R handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e,
                                                 HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',不支持'{}'请求", requestURI, e.getMethod());
        return R.fail(e.getMessage());
    }

    /**
     * 业务异常
     */
    @ExceptionHandler(ServiceException.class)
    public R handleServiceException(ServiceException e, HttpServletRequest request)
    {
        log.warn(e.getMessage(), e);
        Integer code = e.getCode();
        return ObjectUtil.isNotEmpty(code) ? R.fail(code, e.getMessage()) : R.fail(e.getMessage());
    }

    /**
     * 请求路径中缺少必需的路径变量
     */
    @ExceptionHandler(MissingPathVariableException.class)
    public R handleMissingPathVariableException(MissingPathVariableException e, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        log.error("请求路径中缺少必需的路径变量'{}',发生系统异常.", requestURI, e);
        return R.fail(String.format("请求路径中缺少必需的路径变量[%s]", e.getVariableName()));
    }
    /**
     * 自定义验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R handleMethodArgumentNotValidException(MethodArgumentNotValidException e)
    {
        log.error(e.getMessage(), e);
        String message = e.getBindingResult().getFieldError().getDefaultMessage();
        return R.fail(message);
    }

    /**
     * 请求参数类型不匹配
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public R handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        String value = Convert.toStr(e.getValue());
        if (StringUtils.isNotEmpty(value))
        {
            value = EscapeUtil.clean(value);
        }
        log.error("请求参数类型不匹配，参数[{}]要求类型为：'{}'，但输入值为：'{}'", e.getName(), e.getRequiredType().getName(), value);
        log.error("请求参数类型不匹配'{}',发生系统异常.", requestURI, e);

        return R.fail("请求参数类型不匹配");

    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R handleMethodArgumentTypeMismatchException(HttpMessageNotReadableException e, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();

        log.error("请求参数类型不匹配'{}',发生系统异常{}.", requestURI, e.getMessage());

        return R.fail("请求参数类型不匹配");

    }
    @ExceptionHandler(HandlerMethodValidationException.class)
    public R handleMethodArgumentTypeMismatchException(HandlerMethodValidationException e, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();

        log.error("请求参数类型不匹配'{}',发生系统异常{}.", requestURI, e.getMessage());

        return R.fail("请求参数类型不匹配");

    }
    /**
     * 拦截未知的运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public R handleRuntimeException(RuntimeException e, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',发生未知异常.", requestURI, e);
        return R.fail("正在加载中，请稍候");
    }

    /**
     * 系统异常
     */
    @ExceptionHandler(Exception.class)
    public R handleException(Exception e, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',发生系统异常.", requestURI, e);
        return R.fail("正在加载中，请稍候");
    }
    /**
     * 系统异常
     */
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public R handleException(jakarta.validation.ConstraintViolationException e, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',发生系统异常.", requestURI, e);
        
        StringBuffer message = new StringBuffer();
        
        Set<ConstraintViolation<?>> s = e.getConstraintViolations();
        if (s == null) {
        	return R.fail(e.getMessage());
        }
        for (ConstraintViolation<?> constraintViolation : s) {
        	log.info("{}", ((ConstraintViolationImpl)constraintViolation));
        	message.append( ((ConstraintViolationImpl)constraintViolation).getMessage()).append(";");
        	
		}
        return R.fail(message.toString());
    }

    /**
     * 自定义验证异常
     */
    @ExceptionHandler(BindException.class)
    public R handleBindException(BindException e)
    {
        log.error(e.getMessage(), e);
        String message = e.getAllErrors().get(0).getDefaultMessage();
        return R.fail(message);
    }

    
}
