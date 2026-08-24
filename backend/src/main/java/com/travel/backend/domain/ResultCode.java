package com.travel.backend.domain;


/**
 * 枚举了一些常用API操作码
 * Created by xbm
 */
public enum ResultCode {
    /**
     *
     */
    ERROR(0, "系统错误"),
    SUCCESS(1, "操作成功"),
    NO_AUTH(2, "无权限操作"),
    PARAMETER_ERROR(3, "参数错误"),
    USERISBIND(4, "用户已存在"),
    NOTEXISTED(6, "记录不存在"),
    EXITED(5, "记录已存在"),
    AUTH_ERROR(7, "账号或密码错误"),
    ROLE_ERROR(8, "权限错误"),
    PASSWORD_ERROR(9, "两次密码输入不一致，请重试"),
    PHONE_ALREDY(10,"当前手机号已被绑定，请更换"),

    UNAUTHORIZED(401, "暂未登录"),
    PATH_NO_EXIST(404, "请求路径错误"),
    ERROR_OF_500(500, "系统错误"),
    SUCCESS_OF_200(200, "操作成功"),
    USER_NOT_EXIST(501, "用户不存在");

    private int code;
    private String message;

    private ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
