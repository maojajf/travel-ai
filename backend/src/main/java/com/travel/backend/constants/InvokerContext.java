package com.travel.backend.constants;

/**
 * 类描述：
 *
 * @author luokun
 * @data 2019-07-24
 */
public class InvokerContext {

    /**
     * 用户上下文
     */
    private static final ThreadLocal<CurrentUser> _USER_CTX = new ThreadLocal<>();

    public static final String _USER_CTX_KEY = "_USER_CTX";

    private InvokerContext() {
    }

    public static void setUserCtx(CurrentUser userCtx) {
        clear();
        _USER_CTX.set(userCtx);
    }

    public static CurrentUser getUserCtx() {
        return _USER_CTX.get();
    }

    public static void clear() {
        _USER_CTX.remove();
    }
}
