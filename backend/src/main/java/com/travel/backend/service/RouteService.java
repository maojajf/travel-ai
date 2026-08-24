package com.travel.backend.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.travel.backend.domain.CommonPage;
import com.travel.backend.domain.ResultCode;
import com.travel.backend.domain.dto.RouteGenerateDTO;
import com.travel.backend.domain.vo.RouteListVo;
import com.travel.backend.exception.ServiceException;
import com.travel.backend.mapper.RouteFavoriteMapper;
import com.travel.backend.mapper.RouteRecordMapper;
import com.travel.backend.domain.model.FavoriteRecord;
import com.travel.backend.domain.model.RouteRecord;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import com.travel.backend.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
public class RouteService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final long STREAM_FLUSH_INTERVAL_MILLIS = 60L;
    private static final int DAILY_GENERATE_LIMIT = 5;
    private static final String DAILY_GENERATE_LIMIT_KEY_PREFIX = "route:generate:daily:";
    private static final String GENERATE_PRECHECK_KEY_PREFIX = "route:generate:precheck:";
    private static final long GENERATE_PRECHECK_TTL_MINUTES = 5L;
    private static final String GENERATE_PRECHECK_CONSUMED = "CONSUMED";

    @Autowired
    private  RouteRecordMapper routeRecordMapper;
    @Autowired
    private  RouteFavoriteMapper routeFavoriteMapper;
    @Autowired
    private  AiRouteStreamClient aiRouteStreamClient;
    @Autowired
    private  RoutePromptBuilder routePromptBuilder;
    @Autowired
    private  RouteMarkdownSanitizer routeMarkdownSanitizer;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    @Qualifier("threadPoolTaskExecutor")
    private Executor streamExecutor;

    public SseEmitter generateStream(String deviceId, RouteGenerateDTO request,Integer userId) {
        consumeGeneratePrecheck(userId, deviceId);
        SseEmitter emitter = new SseEmitter(0L);
        streamExecutor.execute(() -> streamRoute(emitter, deviceId, request,userId));
        return emitter;
    }

    /**
     * 一个用户一天只能使用5次
     * @param userId
     */
    public Boolean createNum(Integer userId, String deviceId)  {
        checkAndIncrementDailyGenerateLimit(userId);
        markGeneratePrecheck(userId, deviceId);
        return true;
    }

    private void checkAndIncrementDailyGenerateLimit(Integer userId) {
        LocalDate today = LocalDate.now();
        String key = DAILY_GENERATE_LIMIT_KEY_PREFIX + userId + ":" + today;
        long secondsToTomorrow = Math.max(1, Duration.between(
            LocalDateTime.now(),
            LocalDateTime.of(today.plusDays(1), LocalTime.MIN)
        ).getSeconds());

        long usedCount = redisUtil.incr(key, 1);
        if (usedCount == 1) {
            redisUtil.expire(key, secondsToTomorrow);
        }

        if (usedCount > DAILY_GENERATE_LIMIT) {
            throw new ServiceException("今日路线生成次数已达上限，请明天再试", ResultCode.ERROR.getCode());
        }
    }

    private void markGeneratePrecheck(Integer userId, String deviceId) {
        redisUtil.set(
            buildGeneratePrecheckKey(userId, deviceId),
            UUID.randomUUID().toString(),
            GENERATE_PRECHECK_TTL_MINUTES,
            TimeUnit.MINUTES
        );
    }

    private void consumeGeneratePrecheck(Integer userId, String deviceId) {
        String key = buildGeneratePrecheckKey(userId, deviceId);
        String precheckToken = redisUtil.getAndSet(key, GENERATE_PRECHECK_CONSUMED, String.class);
        if (!StringUtils.hasText(precheckToken) || GENERATE_PRECHECK_CONSUMED.equals(precheckToken)) {
            throw new ServiceException("今日路线生成次数已达上限，请明天再试", ResultCode.ERROR.getCode());
        }
        redisUtil.del(key);
    }

    private String buildGeneratePrecheckKey(Integer userId, String deviceId) {
        return GENERATE_PRECHECK_KEY_PREFIX + userId + ":" + deviceId;
    }

    public CommonPage<RouteListVo> listHistory(Integer userId) {
        List<RouteListVo> routeRecords = routeRecordMapper.listHistory(userId);

        if(CollectionUtil.isEmpty(routeRecords)){
            return new CommonPage<>();
        }

        Set<Integer> favoriteIds = findFavoriteRouteIds(userId);
        routeRecords.forEach(record -> record.setFavorite(favoriteIds.contains(Integer.valueOf(record.getRouteId()))));
        return CommonPage.restPage(routeRecords);
    }

    public CommonPage<RouteListVo> listFavorites(Integer userId) {
        List<RouteListVo> routeRecords = routeRecordMapper.listFavorites(userId);

        if(CollectionUtil.isEmpty(routeRecords)){
            return new CommonPage<>();
        }
        return CommonPage.restPage(routeRecords);
    }

    public RouteListVo getDetail(String routeId, Integer userId) {
        RouteRecord record = findRoute(routeId);
        RouteListVo routeListVo = new RouteListVo();
        BeanUtil.copyProperties(record, routeListVo);
        if(ObjectUtil.isNotEmpty(userId)){
            boolean b =  existsFavorite(routeId, userId);
            routeListVo.setFavorite( b);
        }
        routeListVo.setStartAt(format(record.getStartAt()));
        routeListVo.setEndAt(format(record.getEndAt()));
        return routeListVo;
    }

    public void favorite(Integer userId, String routeId) {
        findRoute(routeId);
        if (!existsFavorite(routeId, userId)) {
            FavoriteRecord record = new FavoriteRecord();
            record.setRouteId(Integer.valueOf(routeId));
            record.setUserId(userId);
            routeFavoriteMapper.insert( record);
        }
    }

    public void unfavorite(Integer userId, String routeId) {
        findRoute(routeId);
        routeFavoriteMapper.delete(new LambdaUpdateWrapper<FavoriteRecord>()
            .eq(FavoriteRecord::getRouteId, routeId)
            .eq(FavoriteRecord::getUserId, userId));
    }

    private RouteRecord createRecord(String deviceId, RouteGenerateDTO request, String markdown,Integer userId) {
        RouteRecord record = new RouteRecord();
        record.setDeviceId(deviceId)
                    .setProvince(request.getProvince())
                    .setCity(request.getCity())
                    .setStartAt(DateUtil.parse(request.getStartDate() + " " + request.getStartTime()))
                    .setEndAt(DateUtil.parse(request.getEndDate() + " " + request.getEndTime()))
                    .setTravelGroup(request.getTravelGroup())
                    .setBudgetLevel(request.getBudgetLevel())
                    .setSummary(routePromptBuilder.buildSummary(request, markdown))
                    .setContentMarkdown(markdown)
                    .setUserId(userId);
        return record;
    }

    private void streamRoute(SseEmitter emitter, String deviceId, RouteGenerateDTO request,Integer userId) {
        try {
            String generationId = UUID.randomUUID().toString();
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("generationId", generationId);
            meta.put("title", request.getCity() + " 旅行路线生成中");
            emitter.send(SseEmitter.event().name("meta").data(meta));

            RouteStreamAccumulator accumulator = new RouteStreamAccumulator(
                routeMarkdownSanitizer,
                STREAM_FLUSH_INTERVAL_MILLIS
            );

            aiRouteStreamClient.generate(request, chunk -> accumulator.append(chunk, deltaChunk ->
                sendDelta(emitter, deltaChunk, accumulator.getReceivedChars())
            ));

            accumulator.flush(deltaChunk -> sendDelta(emitter, deltaChunk, accumulator.getReceivedChars()));
            String sanitizedMarkdown = accumulator.complete(deltaChunk ->
                sendDelta(emitter, deltaChunk, accumulator.getReceivedChars())
            );

            RouteRecord record = createRecord(deviceId, request, sanitizedMarkdown,userId);
            routeRecordMapper.insert(record);

            Map<String, Object> done = new LinkedHashMap<>();
            done.put("routeId", record.getId());
            done.put("summary", record.getSummary());
            done.put("contentMarkdown", sanitizedMarkdown);
            done.put("createdAt", format(record.getCreatedAt()));
            done.put("favorite", false);
            emitter.send(SseEmitter.event().name("done").data(done));
            emitter.complete();
        } catch (Exception exception) {
            log.error("Failed to stream route markdown", exception);
            try {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("code", "STREAM_ERROR");
                error.put("message", "生成失败，请稍后重试。");
                emitter.send(SseEmitter.event().name("error").data(error));
            } catch (IOException ignored) {
                // Ignore secondary send failure.
            }
            emitter.completeWithError(exception);
        }
    }

    private void sendDelta(SseEmitter emitter, String deltaChunk, int receivedChars) {
        if (deltaChunk == null || deltaChunk.isEmpty()) {
            return;
        }

        try {
            Map<String, Object> delta = new LinkedHashMap<>();
            delta.put("chunk", deltaChunk);
            delta.put("receivedChars", receivedChars);
            emitter.send(SseEmitter.event().name("delta").data(delta));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to forward SSE chunk", exception);
        }
    }

    private RouteRecord findRoute(String routeId) {
        RouteRecord record = routeRecordMapper.selectById(routeId);
        if (ObjectUtil.isEmpty(record)) {
            throw new ServiceException(ResultCode.NOTEXISTED.getMessage(), ResultCode.NOTEXISTED.getCode());
        }
        return record;
    }

    private String format(Date value) {
        if (value == null) {
            return "";
        }
        return DateUtil.format(value, DATE_TIME_FORMATTER);
    }

    private boolean existsFavorite(String routeId, Integer userId) {
        return routeFavoriteMapper.selectCount(new LambdaQueryWrapper<FavoriteRecord>()
            .eq(FavoriteRecord::getRouteId, routeId)
            .eq(FavoriteRecord::getUserId, userId)) > 0;
    }

    private Set<Integer> findFavoriteRouteIds(Integer userId) {
        List<FavoriteRecord> favorites = routeFavoriteMapper.selectList(new LambdaQueryWrapper<FavoriteRecord>()
            .eq(FavoriteRecord::getUserId, userId));
        Set<Integer> favoriteIds = new HashSet<>();
        for (FavoriteRecord favorite : favorites) {
            favoriteIds.add(Integer.valueOf(favorite.getRouteId()));
        }
        return favoriteIds;
    }
}
