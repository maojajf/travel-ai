package com.travel.backend.controller;

import cn.hutool.core.util.ObjectUtil;
import com.github.pagehelper.PageHelper;
import com.travel.backend.constants.Authority;
import com.travel.backend.constants.CurrentUser;
import com.travel.backend.constants.InvokerContext;
import com.travel.backend.domain.CommonPage;
import com.travel.backend.domain.PageDTO;
import com.travel.backend.domain.R;
import com.travel.backend.domain.ResultCode;
import com.travel.backend.domain.dto.RouteGenerateDTO;
import com.travel.backend.domain.vo.RouteListVo;
import com.travel.backend.exception.ServiceException;
import com.travel.backend.service.RouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Tag(name = "攻略相关")
@RestController
@RequestMapping("/routes")
public class RouteController {

    private static final MediaType SSE_UTF8 = new MediaType("text", "event-stream", StandardCharsets.UTF_8);

    @Autowired
    private  RouteService routeService;

    @Authority
    @Operation(summary = "数据生成")
    @PostMapping(path = "/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> generateStream(@RequestHeader("X-Device-Id") String deviceId,
                                                     @Valid @RequestBody RouteGenerateDTO request) {
        CurrentUser userInfo = InvokerContext.getUserCtx();
        if (ObjectUtil.isEmpty(userInfo)) {
            throw new ServiceException(ResultCode.UNAUTHORIZED.getMessage(), ResultCode.UNAUTHORIZED.getCode());
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(SSE_UTF8);
        headers.set(HttpHeaders.CACHE_CONTROL, "no-cache, no-transform");
        headers.add("X-Accel-Buffering", "no");
        headers.add(HttpHeaders.CONNECTION, "keep-alive");
        return ResponseEntity.ok()
            .headers(headers)
            .body(routeService.generateStream(validateDeviceId(deviceId), request,userInfo.getUserId()));
    }

    @Operation(summary = "判断用户一天生成次数")
    @PostMapping("/createNum")
    @Authority
    public R<Boolean> createNum(@RequestHeader("X-Device-Id") String deviceId) {
        CurrentUser userInfo = InvokerContext.getUserCtx();
        if (ObjectUtil.isEmpty(userInfo)) {
            throw new ServiceException(ResultCode.UNAUTHORIZED.getMessage(), ResultCode.UNAUTHORIZED.getCode());
        }
        return R.ok(routeService.createNum(userInfo.getUserId(), validateDeviceId(deviceId)));
    }

    @Operation(summary = "历史列表")
    @GetMapping("/history")
    @Authority
    public R<CommonPage<RouteListVo>> listHistory(@ParameterObject PageDTO page) {
        CurrentUser userInfo = InvokerContext.getUserCtx();
        if (ObjectUtil.isEmpty(userInfo)) {
            throw new ServiceException(ResultCode.UNAUTHORIZED.getMessage(), ResultCode.UNAUTHORIZED.getCode());
        }
        PageHelper.startPage(page.getPage(), page.getPageSize());
        return R.ok(routeService.listHistory(userInfo.getUserId()));
    }

    @Operation(summary = "收藏列表")
    @GetMapping("/favorites")
    @Authority
    public R<CommonPage<RouteListVo>> listFavorites(@ParameterObject PageDTO page) {
        CurrentUser userInfo = InvokerContext.getUserCtx();
        if (ObjectUtil.isEmpty(userInfo)) {
            throw new ServiceException(ResultCode.UNAUTHORIZED.getMessage(), ResultCode.UNAUTHORIZED.getCode());
        }
        PageHelper.startPage(page.getPage(), page.getPageSize());
        return R.ok(routeService.listFavorites(userInfo.getUserId()));
    }

    @PostMapping("/{routeId}/favorite")
    @Operation(summary = "收藏")
    @Authority
    public R favorite(@PathVariable("routeId") String routeId) {
        CurrentUser userInfo = InvokerContext.getUserCtx();
        if (ObjectUtil.isEmpty(userInfo)) {
            throw new ServiceException(ResultCode.UNAUTHORIZED.getMessage(), ResultCode.UNAUTHORIZED.getCode());
        }
        routeService.favorite(userInfo.getUserId(), routeId);
        return R.ok();
    }

    @DeleteMapping("/{routeId}/favorite")
    @Operation(summary = "取消收藏")
    @Authority
    public R unfavorite(@PathVariable("routeId") String routeId) {
        CurrentUser userInfo = InvokerContext.getUserCtx();
        if (ObjectUtil.isEmpty(userInfo)) {
            throw new ServiceException(ResultCode.UNAUTHORIZED.getMessage(), ResultCode.UNAUTHORIZED.getCode());
        }
        routeService.unfavorite(userInfo.getUserId(), routeId);
        return R.ok();
    }

    @Operation(summary = "详情")
    @GetMapping("/{routeId}")
    @Authority(needLogon = Authority.NeedLogon.NO)
    public R<RouteListVo> getDetail(@PathVariable("routeId") String routeId) {
        CurrentUser userInfo = InvokerContext.getUserCtx();
        return R.ok(routeService.getDetail(routeId,ObjectUtil.isEmpty(userInfo)?null:userInfo.getUserId()));
    }

    private String validateDeviceId(String deviceId) {
        if (!StringUtils.hasText(deviceId)) {
            throw new ResponseStatusException(BAD_REQUEST, "X-Device-Id is required");
        }
        return deviceId.trim();
    }
}
