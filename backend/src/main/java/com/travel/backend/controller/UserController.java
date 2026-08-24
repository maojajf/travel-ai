package com.travel.backend.controller;

import com.travel.backend.constants.HeaderNames;
import com.travel.backend.domain.R;
import com.travel.backend.domain.dto.UserLoginDTO;
import com.travel.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@Tag(name = "登录相关接口")
@RestController
@RequestMapping("/user")
public class UserController  {

	@Autowired
	private UserService userService;

//	@Operation(summary = "检查Token是否可用")
//	@PostMapping("/checkUserToken")
//	@Authority
//	public R<Boolean> checkUserToken(@RequestHeader(HeaderNames.X_AUTH_TOKEN) String token,
//			@RequestHeader(HeaderNames.X_APP_CODE) String appCode) {
//		return R.ok(wxUserService.checkUserToken(token, appCode));
//	}

	@Operation(summary = "登录、注册微信端用户")
	@PostMapping("/regOrLogin")
	public R<String> regOrLogin(@Valid @RequestBody UserLoginDTO dto,
			@RequestHeader(HeaderNames.X_DEVICE_ID) String deviceId) {
		return R.ok(userService.regOrLogin(dto,deviceId));
	}

}
