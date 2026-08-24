package com.travel.backend.util;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;

import java.util.*;


/**
 * provide the ability to parse the token only , not to create new one
 * the token key should determinate by appid which from http header
 * @author SamLin(66017083@qq.com)
 *
 */
public class JWTUtil {
	public static String TOKEN_ISSUER="xxshop";
	//temperatory put to caching map
	public static Map<String,String> appAccessKeyMap=new HashMap<>();
	static{
		appAccessKeyMap.put("applet", "xx-access-default");
		appAccessKeyMap.put("government", "government-access-default");
		appAccessKeyMap.put("sxh-platform-mp/main","applet");
	}

	public static String getAccessKey(String appcode){
		return StringUtils.isBlank(appAccessKeyMap.get(appcode))?"sxh-platform-mp/main":appAccessKeyMap.get(appcode);
	}
	/**
	 * this method may through ExpiredJwtException / InterruptedException exception if the
	 * token expired or not able to be parsed, at that time should ask the user to logon again.
	 * return 401 is a good option
	 * @param accessToken
	 * @return
	 */
	public static String getSubject(String accessToken,String appCode){
		String subject = Jwts.parser().setSigningKey(getAccessKey(appCode)).parseClaimsJws(accessToken).getBody().getSubject();
		return subject;
	}
	public static Date getExpiration(String accessToken,String appCode){
		Date expiration = Jwts.parser().setSigningKey(getAccessKey(appCode)).parseClaimsJws(accessToken).getBody().getExpiration();
		return expiration;
	}
	public static Claims getClaims(String accessToken,String appCode){
		Claims claims = null;
		try {
			claims = Jwts.parser().setSigningKey(getAccessKey(appCode)).parseClaimsJws(accessToken).getBody();
		} catch (ExpiredJwtException e) {
			claims = e.getClaims();
		}
		return claims;
	}

	public static String createDefaultAccessToken(String userIdentity, String appcode){
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.HOUR_OF_DAY,15);
		String accessToken = createNewAccessToken(userIdentity, cal.getTime(), appcode);
		return accessToken;
	}
	/**
	 * create a new access token for that subject ... means auto extend the access time
	 * @return
	 */
	public static String createNewAccessToken(String user,Date expirationTime, String appcode){
		String accessToken = Jwts.builder()
				.setIssuer(TOKEN_ISSUER)
				.setId(UUID.randomUUID().toString())
				.setSubject(user)
				.setExpiration(expirationTime)
				.signWith(SignatureAlgorithm.HS256, appcode)
				.compact();
		return accessToken;
	}

	public static String resolveAssessTokenFromCookie(HttpServletRequest request){
		Cookie[] reqCookies = request.getCookies();
		String accessToken = null;
		if(reqCookies!=null){
			for(Cookie cookie:reqCookies){
				if(cookie.getName().equals("access-token")){
					accessToken = cookie.getValue();
					break;
				}
			}
		}
		return accessToken;
	}

	public static String getVerifyCodeTokenFromCookie(HttpServletRequest request){
		Cookie[] reqCookies = request.getCookies();
		String verifyCodeToken = null;
		if(reqCookies!=null){
			for(Cookie cookie:reqCookies){
				if(cookie.getName().equals("verify-token")){
					verifyCodeToken = cookie.getValue();
					break;
				}
			}
		}
		return verifyCodeToken;
	}

	public static String resolveAppcodeFromHeader(HttpServletRequest request){
		String appcode =request.getHeader("appcode");
		return appcode;
	}

	//获取登录验证
	public static String getTokenSubject(HttpServletRequest request){
		String accessToken = resolveAssessTokenFromCookie(request);

		return accessToken==null?accessToken:getSubject(accessToken,resolveAppcodeFromHeader(request));
	}
	//获取验证码
	public static String getVerifyTokenSubject(HttpServletRequest request){
		String accessToken = getVerifyCodeTokenFromCookie(request);
		return accessToken==null?accessToken:getSubject(accessToken,resolveAppcodeFromHeader(request));
	}
}
