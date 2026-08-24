package com.travel.backend.constants;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Authority {
	
	/**
	 * 是否需要Logon限制，默认需要
	 */
	NeedLogon needLogon() default NeedLogon.YES;

	/**
	 * 权限代码，默认不需要权限控制
	 */
	AuthCode authCode() default AuthCode.un_auth;

	enum NeedLogon {

		YES, NO;

	}

	enum AuthCode {

		un_auth(0), // 默认值，不需要权限控制
		user_login(66) // 用户登录
		;
		long funId;

		AuthCode(long funId) {
			this.funId = funId;
		}

		public long getFunId() {
			return funId;
		}

	}
}
