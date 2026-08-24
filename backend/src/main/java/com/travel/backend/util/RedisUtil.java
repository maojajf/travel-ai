package com.travel.backend.util;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.travel.backend.domain.ResultCode;
import com.travel.backend.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metric;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.util.CollectionUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @DESCRIPTION redis 工具
 * @create 2020/2/26
 */
@Slf4j
public class RedisUtil {

    public RedisTemplate<Object, Object> redisTemplate;

    public RedisUtil(RedisTemplate<Object, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    final Random random = new Random();

    /**
     * 修改Key的名字
     *
     * @param oldKey
     * @param newKey
     */
    public void rename(String oldKey, String newKey) {
        if (hasKey(oldKey)) {
            redisTemplate.rename(oldKey, newKey);
        }
    }

    public List<Object> executePipelined(RedisCallback<?> action) {
        return redisTemplate.executePipelined(action);
    }

    public Set<String> scan(String matchKey) {
        HashSet keys = new HashSet();
        ScanOptions options = ScanOptions.scanOptions().match(matchKey).count(1000).build();
        Cursor<String> cursor = (Cursor<String>) redisTemplate.executeWithStickyConnection(redisConnection -> new ConvertingCursor<>(redisConnection.scan(options), redisTemplate.getKeySerializer()::deserialize));

        cursor.forEachRemaining(key -> {
            keys.add(key);
        });

        return keys;
    }

    /**
     * 指定缓存失效时间
     *
     * @param key  键
     * @param time 时间(秒)
     * @return
     */
    public boolean expire(String key, long time) {
        try {
            if (time > 0) {
                redisTemplate.expire(key, time, TimeUnit.SECONDS);
            }
            return true;
        } catch (Exception e) {
            log.error("{}", e);
            return false;
        }
    }

    /**
     * 指定缓存失效时间
     */
    public boolean expire(String key, long time, TimeUnit unit) {
        try {
            if (time > 0) {
                redisTemplate.expire(key, time, unit);
            }
            return true;
        } catch (Exception e) {
            log.error("{}", e);
            return false;
        }
    }

    /**
     * 根据key 获取过期时间
     *
     * @param key 键 不能为null
     * @return 时间(秒) 返回0代表为永久有效
     */
    public long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    public long getExpire(String key, TimeUnit unit) {
        return redisTemplate.getExpire(key, unit);
    }

    /**
     * 判断key是否存在
     *
     * @param key 键
     * @return true 存在 false不存在
     */
    public boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("redis 判断key是否存在 执行异常", e);
        }
        return false;
    }

    /**
     * 判断key是否存在
     * (默认存在，一些业务需要避免重复)
     *
     * @param key 键
     * @return true 存在 false不存在
     */
    public boolean hasKeyV2(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("redis 判断key是否存在 执行异常", e);
        }
        return true;
    }

    /**
     * 判断key是否存在
     *
     * @param key 键
     * @return true 存在 false不存在
     */
    public boolean notHasKey(String key) {
        try {
            return !redisTemplate.hasKey(key);
        } catch (Exception e) {
            return false;
        }
    }

    private static final int DELETE_THRESHOLD = 100;

    public DataType type(String key) {
        return redisTemplate.type(key);
    }

    /**
     * 删除大批量数据时，分次删除
     *
     * @param key
     */
    public void delAll(String key, DataType type) {
        long count = 0;
        switch (type) {
            case SET:
                count = sGetSetSize(key);
                if (count < DELETE_THRESHOLD) {
                    redisTemplate.delete(key);
                }
                break;
            case HASH:
                count = hSize(key);
                if (count < DELETE_THRESHOLD) {
                    del(key);
                }
                break;
            case ZSET:
                count = zSize(key);
                if (count < DELETE_THRESHOLD) {
                    redisTemplate.delete(key);
                }
                break;
            case LIST:
                count = lGetListSize(key);
                if (count < DELETE_THRESHOLD) {
                    redisTemplate.delete(key);
                }
                break;
            case STRING:
                redisTemplate.delete(key);
                break;
            default:
                break;
        }
    }

    /**
     * 删除缓存
     *
     * @param key 可以传一个值 或多个
     */
    @SuppressWarnings("unchecked")
    public void del(String... key) {
        if (key != null && key.length > 0) {
            if (key.length == 1) {
                redisTemplate.delete(key[0]);
            } else {
                redisTemplate.delete(CollectionUtils.arrayToList(key));

            }
        }
    }

    public void del(Set<String> keys) {
        if (keys.size() > 0) {
            redisTemplate.delete(keys);
        }
    }

    // ============================String=============================

    /**
     * 普通缓存获取
     *
     * @param key 键
     * @return 值
     */
    public Object get(String key) {
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }

    public <T> T get(String key, Class<T> type) {
        if (StrUtil.isEmpty(key)) {
            return null;
        }
        Object o = redisTemplate.opsForValue().get(key);
        if (ObjectUtil.isNull(o)) {
            return null;
        }
        return Convert.convert(type, o);
    }

    public <T> T getAndSet(String key, Object newValue, Class<T> returnType) {
        if (StrUtil.isEmpty(key)) {
            return null;
        }
        Object o = redisTemplate.opsForValue().getAndSet(key, newValue);
        if (ObjectUtil.isNull(o)) {
            return null;
        }
        return Convert.convert(returnType, o);
    }

    public List<Object> mGet(List<Object> keys) {
        return keys.size() == 0 ? null : redisTemplate.opsForValue().multiGet(keys);
    }

    /**
     * 普通缓存放入
     *
     * @param key   键
     * @param value 值
     * @return true成功 false失败
     */
    public boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            log.error("redis.Set报错,key={}, value={}", key, value, e);
            return false;
        }
    }

    /**
     * 普通缓存放入并设置时间
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒) time要大于0 如果time小于等于0 将设置无限期
     * @return true成功 false 失败
     */
    public boolean set(String key, Object value, long time) {
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            log.error("{}", e);
            return false;
        }
    }

    /**
     * 普通缓存放入并设置时间
     *
     * @param key   键
     * @param value 值
     * @param time  time要大于0 如果time小于等于0 将设置无限期
     * @param unit  时间单位
     * @return true成功 false 失败
     */
    public boolean set(String key, Object value, long time, TimeUnit unit) {
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, unit);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            log.error("{}", e);
            return false;
        }
    }

    /**
     * 原子设置缓存（如果不存在）
     *
     * @param key 键
     * @param value 值
     * @param time 过期时间(秒)
     * @return true: 设置成功（key不存在） false: 设置失败（key已存在）
     */
    public boolean setIfAbsent(String key, Object value, long time) {
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value, time, TimeUnit.SECONDS));
        } catch (Exception e) {
            log.error("原子设置缓存失败: {}", key, e);
            return false;
        }
    }

    /**
     * 递增
     *
     * @param key   键
     * @param delta 要增加几(大于0)
     * @return
     */
    public long incr(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("递增因子必须大于0");
        }
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 递增
     *
     * @param key   键
     * @param delta 要增加几(大于0)
     * @return
     */
    public long incr(String key, long delta, long time, TimeUnit unit) {
        if (delta < 0) {
            throw new RuntimeException("递增因子必须大于0");
        }
        long result = redisTemplate.opsForValue().increment(key, delta);
        expire(key, time, unit);

        return result;
    }

    /**
     * 递减
     *
     * @param key   键
     * @param delta 要减少几(小于0)
     * @return
     */
    public long decr(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("递减因子必须大于0");
        }
        return redisTemplate.opsForValue().increment(key, -delta);
    }

    /**
     * 递增
     *
     * @param key   键
     * @param delta 要增加几(大于0)
     * @return
     */
    public long decr(String key, long delta, long time, TimeUnit unit) {
        if (delta < 0) {
            throw new RuntimeException("递减因子必须大于0");
        }
        long result = redisTemplate.opsForValue().increment(key, -delta);
        expire(key, time, unit);

        return result;
    }

    // ================================Map=================================

    /**
     * HashGet
     *
     * @param key  键 不能为null
     * @param item 项 不能为null
     * @return 值
     */
    public Object hget(String key, String item) {
        return redisTemplate.opsForHash().get(key, item);
    }

    public <T> T hget(String key, String item, Class<T> type) {
        Object o = redisTemplate.opsForHash().get(key, item);
        if (ObjectUtil.isNull(o)) {
            return null;
        }
        return Convert.convert(type, o);
    }

    /**
     * 获取hashKey对应的所有键值
     *
     * @param key 键
     * @return 对应的多个键值
     */
    public Map<Object, Object> hGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 获取hashKey对应的所有值
     *
     * @param key 键
     * @return 对应的多个值
     */
    public List<Object> hVals(String key) {
        return redisTemplate.opsForHash().values(key);
    }

    public List<Object> hmget(String key, List<String> fields) {
        return redisTemplate.opsForHash().multiGet(key, (List<Object>) (List<?>) fields);
    }

    /**
     * 获取hashKey对应的所有键值--(使用hscan,防止获取大数据是造成redis卡顿）
     *
     * @param key
     * @return
     */
    public Map<Object, Object> hmgetBig(String key) {
        Map<Object, Object> map = new HashMap<>();
        Cursor<Map.Entry<Object, Object>> cursor = redisTemplate.boundHashOps(key).scan(ScanOptions.scanOptions().match("*").count(1000).build());
        while (cursor.hasNext()) {
            Map.Entry<Object, Object> entry = cursor.next();
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }

    /**
     * 获取hashKey对应的所有键值
     *
     * @param key 键
     * @return 对应的多个键值
     */
    public Long hSize(String key) {
        return redisTemplate.opsForHash().size(key);
    }

    /**
     * HashSet
     *
     * @param key 键
     * @param map 对应多个键值
     * @return true 成功 false 失败
     */
    public boolean hmset(String key, Map<String, Object> map) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            return true;
        } catch (Exception e) {
            log.error("{}", e);
            return false;
        }
    }

    /**
     * HashSet 并设置时间
     *
     * @param key  键
     * @param map  对应多个键值
     * @param time 时间(秒)
     * @return true成功 false失败
     */
    public boolean hmset(String key, Map<String, Object> map, long time) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            log.error("{}", e);
            return false;
        }
    }

    /**
     * HashSet 并设置时间
     *
     * @param key 键
     * @param map 对应多个键值
     * @return true成功 false失败
     */
    public boolean hmset(String key, Map<String, Object> map, long time, TimeUnit unit) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            if (time > 0) {
                expire(key, time, unit);
            }
            return true;
        } catch (Exception e) {
            log.error("{}", e);
            return false;
        }
    }

    /**
     * 查询一张hash表中的key
     *
     * @param path 键
     * @return Set 返回key的set集合
     */
    public Set<Object> getAllKeys(String path) {
        try {
            Set<Object> keys = redisTemplate.keys(path + "*");
            return keys;
        } catch (Exception e) {
            log.error("{}", e);
            return null;
        }
    }

    /**
     * 向一张hash表中放入数据,如果不存在将创建
     *
     * @param key   键
     * @param item  项
     * @param value 值
     * @return true 成功 false失败
     */
    public boolean hset(String key, String item, Object value) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            return true;
        } catch (Exception e) {
            log.error("{}", e);
            return false;
        }
    }

    /**
     * 向一张hash表中放入数据,如果不存在将创建
     *
     * @param key   键
     * @param item  项
     * @param value 值
     * @param time  时间(秒) 注意:如果已存在的hash表有时间,这里将会替换原有的时间
     * @return true 成功 false失败
     */
    public boolean hset(String key, String item, Object value, long time, TimeUnit unit) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            if (time > 0) {
                expire(key, time, unit);
            }
            return true;
        } catch (Exception e) {
            log.error("{}", e);
            return false;
        }
    }

    public boolean hset(String key, String item, Object value, long time) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            log.error("{}", e);
            return false;
        }
    }

    /**
     * 删除hash表中的值
     *
     * @param key  键 不能为null
     * @param item 项 可以使多个 不能为null
     */
    public void hdel(String key, Object... item) {
        redisTemplate.opsForHash().delete(key, item);
    }

    public void hdel(String key, String... item) {
        redisTemplate.opsForHash().delete(key, item);
    }

    /**
     * 判断hash表中是否有该项的值
     *
     * @param key  键 不能为null
     * @param item 项 不能为null
     * @return true 存在 false不存在
     */
    public boolean hHasKey(String key, String item) {
        return redisTemplate.opsForHash().hasKey(key, item);
    }

    /**
     * 获取hash表中所有的key
     *
     * @param key
     * @return
     */
    public Set<Object> hKeys(String key) {
        try {
            return redisTemplate.opsForHash().keys(key);
        } catch (Exception e) {
            log.error("{}", e);
            return null;
        }
    }

    /**
     * hash递增 如果不存在,就会创建一个 并把新增后的值返回
     *
     * @param key  键
     * @param item 项
     * @param by   要增加几(大于0)
     * @return
     */
    public double hincr(String key, String item, double by) {
        return redisTemplate.opsForHash().increment(key, item, by);
    }

    public double hincr(String key, String item, double by, long time, TimeUnit unit) {
        if (by < 0) {
            throw new RuntimeException("递增因子必须大于0");
        }
        double result = redisTemplate.opsForHash().increment(key, item, by);
        expire(key, time, unit);

        return result;
    }

    /**
     * hash递减
     *
     * @param key  键
     * @param item 项
     * @param by   要减少记(小于0)
     * @return
     */
    public double hdecr(String key, String item, double by) {
        return redisTemplate.opsForHash().increment(key, item, -by);
    }

    // ============================set=============================

    /**
     * 根据key获取Set中的所有值
     *
     * @param key 键
     * @return
     */
    public Set<Object> sGet(String key) {
        try {
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            log.error("{}", e);
            return null;
        }
    }

    /**
     * 根据value从一个set中查询,是否存在
     *
     * @param key   键
     * @param value 值
     * @return true 存在 false不存在
     */
    public boolean sHasKey(String key, Object value) {
        try {
            return redisTemplate.opsForSet().isMember(key, value);
        } catch (Exception e) {
            log.error("{}", e);
            return false;
        }
    }

    /**
     * 将数据放入set缓存
     *
     * @param key    键
     * @param values 值 可以是多个
     * @return 成功个数
     */
    public long sSet(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().add(key, values);
        } catch (Exception e) {
            log.error("{}", e);
            return 0;
        }
    }

    public List<Object> sSetBatch(String key, List<Integer> list){
        return redisTemplate.executePipelined(new RedisCallback<Integer>() {
            @Override
            public Integer doInRedis(RedisConnection redisConnection) throws DataAccessException {
                list.forEach(x->{
                    redisConnection.sAdd(key.getBytes(StandardCharsets.UTF_8),x.toString().getBytes(StandardCharsets.UTF_8));
                });
                return null;
            }
        });
    }

    /**
     * 将set数据放入缓存
     *
     * @param key    键
     * @param time   时间(秒)
     * @param values 值 可以是多个
     * @return 成功个数
     */
    public long sSetAndTime(String key, long time, Object... values) {
        try {
            Long count = redisTemplate.opsForSet().add(key, values);
            if (time > 0) {
                expire(key, time);
            }

            return count;
        } catch (Exception e) {
            log.error("{}", e);
            return 0;
        }
    }

    /**
     * 将set数据放入缓存
     *
     * @param key    键
     * @param time   时间(秒)
     * @param values 值 可以是多个
     * @return 成功个数
     */
    public long sSetAndTime(String key, long time, TimeUnit unit, Object... values) {
        try {
            Long count = redisTemplate.opsForSet().add(key, values);
            if (time > 0) {
                expire(key, time, unit);
            }

            return count;
        } catch (Exception e) {
            log.error("{}", e);
            return 0;
        }
    }

    /**
     * 获取set缓存的长度
     *
     * @param key 键
     * @return
     */
    public long sGetSetSize(String key) {
        try {
            return redisTemplate.opsForSet().size(key);
        } catch (Exception e) {
            log.error("{}", e);
            return 0;
        }
    }

    /**
     * 移除值为value的
     *
     * @param key    键
     * @param values 值 可以是多个
     * @return 移除的个数
     */
    public long setRemove(String key, Object... values) {
        try {
            Long count = redisTemplate.opsForSet().remove(key, values);
            return count;
        } catch (Exception e) {
            log.error("{}", e);
            return 0;
        }
    }

    /**
     * Set随机返回 count 个元素
     *
     * @param key
     * @param count
     * @param returnType
     * @param <T>
     * @return
     */
    public <T> Set<T> sDistinctRandomMembers(String key, long count, Class<T> returnType) {
        try {
            Set<Object> list = redisTemplate.opsForSet().distinctRandomMembers(key, count);
            if (ObjectUtil.isEmpty(list)) {
                return null;
            }
            return list.stream().map(item -> Convert.convert(returnType, item)).collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("{}", e);
        }
        return null;
    }

    /**
     * Set随机返回 count 个元素
     *
     * @param key
     * @param count
     * @return
     */
    public Set<Object> sDistinctRandomMembers(String key, long count) {
        try {
            Set<Object> list = redisTemplate.opsForSet().distinctRandomMembers(key, count);
            return list;
        } catch (Exception e) {
            log.error("{}", e);
        }
        return null;
    }

    // ===============================list=================================

    /**
     * 获取list缓存的内容
     *
     * @param key   键
     * @param start 开始
     * @param end   结束 0 到 -1代表所有值
     * @return
     */
    public List<Object> lGet(String key, long start, long end) {
        try {
            return redisTemplate.opsForList().range(key, start, end);
        } catch (Exception e) {
            log.error("{}", e);
            return null;
        }
    }

    /**
     * 获取list缓存的长度
     *
     * @param key 键
     * @return
     */
    public long lGetListSize(String key) {
        try {
            return redisTemplate.opsForList().size(key);
        } catch (Exception e) {
            log.error("{}", e);
            return 0;
        }
    }

    /**
     * 通过索引 获取list中的值
     *
     * @param key   键
     * @param index 索引 index>=0时， 0 表头，1 第二个元素，依次类推；index<0时，-1，表尾，-2倒数第二个元素，依次类推
     * @return
     */
    public Object lGetIndex(String key, long index) {
        try {
            return redisTemplate.opsForList().index(key, index);
        } catch (Exception e) {
            log.error("{}", e);
            return null;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     * @return
     */
    public boolean lSet(String key, Object value) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            return true;
        } catch (Exception e) {
            log.error("{}", e);
            return false;
        }
    }

    /**
     * 将list放入缓存，左插入
     *
     * @param key   键
     * @param value 值
     * @return
     */
    public boolean lSetLeft(String key, Object value) {
        try {
            redisTemplate.opsForList().leftPush(key, value);
            return true;
        } catch (Exception e) {
            log.error("{}", e);
            return false;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒)
     * @return
     */
    public boolean lSet(String key, Object value, long time) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            if (time > 0) {
                expire(key, time);
            }

            return true;
        } catch (Exception e) {
            log.error("{}", e);
            return false;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     * @return
     */
    public boolean lSet(String key, List<Object> value) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            return true;
        } catch (Exception e) {
            log.error("{}", e);
            return false;
        }
    }

    /**
     * 清空list
     *
     * @param key
     * @param start
     * @param end
     * @return
     */
    public boolean lTrim(String key, long start, long end) {
        redisTemplate.opsForList().trim(key, start, end);
        return true;
    }

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒)
     * @return
     */
    public boolean lSet(String key, List<Object> value, long time) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒)
     * @return
     */
    public boolean lSet(String key, List<Object> value, long time, TimeUnit unit) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            if (time > 0) {
                expire(key, time, unit);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 先清空list再将list放入缓存
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒)
     * @param unit
     * @return
     */
    public boolean lReSet(String key, List<Object> value, long time, TimeUnit unit) {
        try {
            redisTemplate.execute(new SessionCallback<Object>() {
                @Override
                public Object execute(RedisOperations redisOperations) throws DataAccessException {
                    redisOperations.multi();
                    redisTemplate.delete(key);
                    redisTemplate.opsForList().rightPushAll(key, value);
                    if (time > 0) {
                        expire(key, time, unit);
                    }
                    return redisOperations.exec();
                }
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 移出并获取列表的【第一个元素】， 如果列表没有元素会阻塞列表直到等待超时或发现可弹出元素为止。
     *
     * @param key
     * @param timeout 超时时间
     * @param unit    时间单位
     *                key没有内容或不存在时，则会阻塞，直到有值返回或者超时。
     *                当超期时间到达时，keys列表仍然没有内容，则返回Null
     * @return Object
     */
    public Object lblpop(String key, long timeout, TimeUnit unit) {
        return redisTemplate.opsForList().leftPop(key, timeout, unit);
    }

    /**
     * 根据索引修改list中的某条数据
     *
     * @param key   键
     * @param index 索引
     * @param value 值
     * @return
     */
    public boolean lUpdateIndex(String key, long index, Object value) {
        try {
            redisTemplate.opsForList().set(key, index, value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 移除N个值为value
     *
     * @param key   键
     * @param count 移除多少个
     * @param value 值
     * @return 移除的个数
     */
    public long lRemove(String key, long count, Object value) {
        try {
            Long remove = redisTemplate.opsForList().remove(key, count, value);
            return remove;
        } catch (Exception e) {
            return 0;
        }
    }

    /**------------------zSet相关操作--------------------------------*/
    /**
     * 添加元素,有序集合是按照元素的score值由小到大排列
     *
     * @param key
     * @param value
     * @param score
     * @return 如果缓存上没有则新增并返回true, 存在则更新，返回false
     */
    public Boolean zAdd(String key, Object value, double score) {
        return redisTemplate.opsForZSet().add(key, value, score);
    }

    /**
     * 添加元素,有序集合是按照元素的score值由小到大排列
     *
     * @param key
     * @param value
     * @param score
     * @return
     */
    public Boolean zAdd(String key, Object value, double score, long time, TimeUnit unit) {
        boolean b = redisTemplate.opsForZSet().add(key, value, score);
        if (time > 0) {
            expire(key, time, unit);
        }
        return b;
    }

    /**
     * 批量添加元素,有序集合是按照元素的score值由小到大排列
     *
     * @param key
     * @param map
     * @param time
     * @param unit
     * @return
     */
    public Boolean zAdd(String key, Map<Object, Double> map, long time, TimeUnit unit) {
        Set<TypedTuple<Object>> typedTupleSet = new HashSet<TypedTuple<Object>>();
        map.forEach((k, v) -> {
            typedTupleSet.add(new DefaultTypedTuple<Object>(k, v));
        });
        if (time > 0) {
            boolean b = expire(key, time, unit);
        }
        return redisTemplate.opsForZSet().add(key, typedTupleSet) > 0 ? true : false;
    }

    /**
     * 批量添加元素,有序集合是按照元素的score值由小到大排列
     *
     * @param key
     * @param map
     * @return
     */
    public Boolean zAdd(String key, Map<Object, Double> map) {
        Set<TypedTuple<Object>> typedTupleSet = new HashSet<TypedTuple<Object>>();
        map.forEach((k, v) -> {
            typedTupleSet.add(new DefaultTypedTuple<Object>(k, v));
        });
        return redisTemplate.opsForZSet().add(key, typedTupleSet) > 0 ? true : false;
    }

    /**
     * 移除zset key对应的set里面的部分元素
     *
     * @param key
     * @param values
     * @return
     */
    public Long zRemove(String key, Object... values) {
        return redisTemplate.opsForZSet().remove(key, values);
    }

    /**
     * 增加元素的score值，并返回增加后的值
     *
     * @param key
     * @param value
     * @param delta
     * @return
     */
    public Double zIncrementScore(String key, String value, double delta) {
        return redisTemplate.opsForZSet().incrementScore(key, value, delta);
    }

    /**
     * 增加元素的score值，并返回增加后的值
     *
     * @param key
     * @param value
     * @param delta
     * @return
     */
    public Double zIncrementScore(String key, long value, double delta) {
        return redisTemplate.opsForZSet().incrementScore(key, value, delta);
    }

    /**
     * 返回元素在集合的排名,有序集合是按照元素的score值由小到大排列
     *
     * @param key
     * @param value
     * @return 0表示第一位
     */
    public Long zRank(String key, Object value) {
        return redisTemplate.opsForZSet().rank(key, value);
    }

    /**
     * 返回元素在集合的排名,按元素的score值由大到小排列
     *
     * @param key
     * @param value
     * @return
     */
    public Long zReverseRank(String key, Object value) {
        return redisTemplate.opsForZSet().reverseRank(key, value);
    }

    /**
     * 获取集合的元素, 从小到大排序
     *
     * @param key
     * @param start 开始位置
     * @param end   结束位置, -1查询所有
     * @return
     */
    public Set<Object> zRange(String key, long start, long end) {
        return redisTemplate.opsForZSet().range(key, start, end);
    }

    /**
     * 获取集合元素, 并且把score值也获取
     *
     * @param key
     * @param start 开始位置
     * @param end   结束位置, -1查询所有
     * @return
     */
    public Set<TypedTuple<Object>> zRangeWithScores(String key, long start, long end) {
        return redisTemplate.opsForZSet().rangeWithScores(key, start, end);
    }

    /**
     * 根据Score值查询集合元素
     *
     * @param key
     * @param min 最小值
     * @param max 最大值
     * @return
     */
    public Set<Object> zRangeByScore(String key, double min, double max) {
        return redisTemplate.opsForZSet().rangeByScore(key, min, max);
    }

    /**
     * 根据Score值查询集合元素, 从小到大排序
     *
     * @param key
     * @param min 最小值
     * @param max 最大值
     * @return
     */
    public Set<TypedTuple<Object>> zRangeByScoreWithScores(String key, double min, double max) {
        return redisTemplate.opsForZSet().rangeByScoreWithScores(key, min, max);
    }

    /**
     * 根据Score值查询集合元素, 从小到大排序 取其中的一部分
     *
     * @param key
     * @param min
     * @param max
     * @param start
     * @param end
     * @return
     */
    public Set<TypedTuple<Object>> zRangeByScoreWithScores(String key, double min, double max, long start, long end) {
        return redisTemplate.opsForZSet().rangeByScoreWithScores(key, min, max, start, end);
    }

    /**
     * 获取集合的元素, 从大到小排序
     *
     * @param key
     * @param start
     * @param end
     * @return
     */
    public Set<Object> zReverseRange(String key, long start, long end) {
        return redisTemplate.opsForZSet().reverseRange(key, start, end);
    }

    /**
     * 获取集合的元素, 从大到小排序, 并返回score值
     *
     * @param key
     * @param start
     * @param end
     * @return
     */
    public Set<TypedTuple<Object>> zReverseRangeWithScores(String key, long start, long end) {
        return redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
    }

    /**
     * 根据Score值查询集合元素, 从大到小排序
     *
     * @param key
     * @param min
     * @param max
     * @return
     */
    public Set<Object> zReverseRangeByScore(String key, double min, double max) {
        return redisTemplate.opsForZSet().reverseRangeByScore(key, min, max);
    }

    /**
     * 根据Score值查询集合元素, 从大到小排序
     *
     * @param key
     * @param min
     * @param max
     * @return
     */
    public Set<TypedTuple<Object>> zReverseRangeByScoreWithScores(String key, double min, double max) {
        return redisTemplate.opsForZSet().reverseRangeByScoreWithScores(key, min, max);
    }

    /**
     * 根据Score值查询集合元素, 从大到小排序 其中一部分
     *
     * @param key
     * @param min
     * @param max
     * @param start
     * @param end
     * @return
     */
    public Set<Object> zReverseRangeByScore(String key, double min, double max, long start, long end) {
        return redisTemplate.opsForZSet().reverseRangeByScore(key, min, max, start, end);
    }

    /**
     * 根据score值获取集合元素数量
     *
     * @param key
     * @param min
     * @param max
     * @return
     */
    public Long zCount(String key, double min, double max) {
        return redisTemplate.opsForZSet().count(key, min, max);
    }

    /**
     * 获取集合大小
     *
     * @param key
     * @return
     */
    public Long zSize(String key) {
        return redisTemplate.opsForZSet().size(key);
    }

    /**
     * 获取集合大小
     *
     * @param key
     * @return
     */
    public Long zZCard(String key) {
        return redisTemplate.opsForZSet().zCard(key);
    }

    /**
     * 获取集合中value元素的score值
     *
     * @param key
     * @param value
     * @return
     */
    public Double zScore(String key, Object value) {
        return redisTemplate.opsForZSet().score(key, value);
    }

    /**
     * 移除指定索引位置的成员
     *
     * @param key
     * @param start
     * @param end
     * @return
     */
    public Long zRemoveRange(String key, long start, long end) {
        return redisTemplate.opsForZSet().removeRange(key, start, end);
    }

    /**
     * 根据指定的score值的范围来移除成员
     *
     * @param key
     * @param min
     * @param max
     * @return
     */
    public Long zRemoveRangeByScore(String key, double min, double max) {
        return redisTemplate.opsForZSet().removeRangeByScore(key, min, max);
    }

    /**
     * @param key
     * @param options
     * @return
     */
    public Cursor<TypedTuple<Object>> zScan(String key, ScanOptions options) {
        return redisTemplate.opsForZSet().scan(key, options);
    }

    public Set<Object> keys(String key) {
        return redisTemplate.keys(key);
    }

    // 锁名称
    //public static final String LOCK_PREFIX = "redis_lock";
    // 加锁失效时间，毫秒
    //public static final int LOCK_EXPIRE = 300; // ms
    public static final int LOCK_EXPIRE = 10000; // ms

    /**
     * 分布式锁
     *
     * @param key key值
     * @param expire 过期时间，毫秒
     * @return 是否获取到
     */
    public boolean getLock(String key, int expire) {
        //String lock = LOCK_PREFIX + key;
        String lock = key;
        // 利用lambda表达式
        return (Boolean) redisTemplate.execute((RedisCallback) connection -> {
            //long expireAt = System.currentTimeMillis() + LOCK_EXPIRE + 1;
            int ex = expire == 0 ? LOCK_EXPIRE : expire;
            long expireAt = System.currentTimeMillis() + ex + 1;
            Boolean acquire = connection.setNX(lock.getBytes(), String.valueOf(expireAt).getBytes());
            if (acquire) {
                return true;
            } else {
                byte[] value = connection.get(lock.getBytes());
                if (Objects.nonNull(value) && value.length > 0) {
                    long expireTime = Long.parseLong(new String(value));
                    // 如果锁已经过期
                    if (expireTime < System.currentTimeMillis()) {
                        // 重新加锁，防止死锁
                        byte[] oldValue = connection.getSet(lock.getBytes(), String.valueOf(System.currentTimeMillis() + LOCK_EXPIRE + 1).getBytes());
                        return Long.parseLong(new String(oldValue)) < System.currentTimeMillis();
                    }
                }
            }
            return false;
        });
    }

    public void getLock(String key, int expire , Runnable runnable) {
        boolean lock = getLock(key, expire);
        try {
            if (lock) {
                log.info("获取到锁 key:{}" , key);
                runnable.run();
            }else {
                log.info("获取锁失败 key:{}" , key);
                throw new ServiceException("系统繁忙，请稍后重试");
            }
        }finally {
            if (lock) {
                log.info("释放锁 key:{}" , key);
                unLock(key);
            }
        }
    }

    /**
     * 分布式锁 - setnxex
     * @return 是否成功获取锁
     */
    public boolean tryLock(String key, int expire, TimeUnit timeUnit) {
        return Optional.ofNullable(redisTemplate.opsForValue().setIfAbsent(key, 0, expire, timeUnit)).orElse(false);
    }

    /**
     * 分布式锁 - setnxex-自旋一段时间
     * @return 是否成功获取锁
     *
     * timeOut默认请求锁的超时时间(ms 毫秒)-自旋的时间
     */
    public boolean tryLockSpin(String key, int expire, TimeUnit timeUnit, long timeOut) {
        // 请求锁超时时间，纳秒
        long timeout = timeOut * 1000000;
        // 系统当前时间，纳秒
        long nowTime = System.nanoTime();
        while ((System.nanoTime() - nowTime) < timeout) {
            if (Optional.ofNullable(redisTemplate.opsForValue().setIfAbsent(key, 0, expire, timeUnit)).orElse(false)) {
                // 上锁成功结束请求
                return true;
            }
            // 每次请求等待一段时间
            this.seleep(10, 50000);
        }
        return false;
    }

    /**
     * 分布式锁 - setnxex-阻塞直到拿到锁
     * @return 是否成功获取锁
     *
     */
    public boolean tryLockBlock(String key, int expire, TimeUnit timeUnit) {
        // 系统当前时间，纳秒
        while (true) {
            if (Optional.ofNullable(redisTemplate.opsForValue().setIfAbsent(key, 0, expire, timeUnit)).orElse(false)) {
                // 上锁成功结束请求
                return true;
            }
            // 每次请求等待一段时间
            this.seleep(10, 50000);
        }
    }

    /**
     * @param millis 毫秒
     * @param nanos  纳秒
     * @Title: seleep
     * @Description: 线程等待时间
     * @author yuhao.wang
     */
    private void seleep(long millis, int nanos) {
        try {
            Thread.sleep(millis, random.nextInt(nanos));
        } catch (InterruptedException e) {
            log.info("获取分布式锁休眠被中断：", e);
        }
    }

    /**
     * 从 set 移除并返回集合中的一个随机元素
     * @param key
     * @return
     */
    public Object pop(String key) {
        try {
            Object obj = redisTemplate.opsForSet().pop(key);
            return obj;
        } catch (Exception e) {
            log.error("{}", e);
        }
        return null;
    }

    /**
     * 从 List 移除并返回集合中的一个随机元素
     * @param key
     * @return
     */
    public Object popList(String key) {
        try {
            Object obj = redisTemplate.opsForList().leftPop(key);
            return obj;
        } catch (Exception e) {
            log.error("{}", e);
        }
        return null;
    }

    /**
     * 删除锁
     *
     * @param key
     */
    public void unLock(String key) {
        redisTemplate.delete(key);
    }


    /**
     * 获取hash结构的key集合
     * @param key hash:key
     * @return
     */
    public Set<Object> hgetKeys(String key) {
        return redisTemplate.opsForHash().keys(key);
    }

    /**
     * 分布式锁
     * @param sup
     * @param key
     * @param lockTime 毫秒
     * @return
     */
    public <T> T distributedLock (Supplier<T> sup, String key, int lockTime) {
        //分布式锁
        boolean lock = false;
        try {
            log.debug("开始获取锁,key:{}", key);
            //获取锁
            lock = this.getLock(key, lockTime);
            if (lock) {
                log.debug("获取到锁");
                return sup.get();
            } else {
                log.debug("获取不到锁");
                //获取不到锁，操作太过频繁
                throw new ServiceException(ResultCode.ERROR.getMessage(),ResultCode.ERROR.getCode());
            }
        } finally {
            log.debug("释放锁");
            //释放锁
            if (lock) {
                this.unLock(key);
            }
        }

    }

    /**
     * 分布式锁
     * @param sup
     * @param key
     * @param lockTime 毫秒
     * @param e 未拿到锁抛出异常
     * @return
     */
    public <T> T distributedLock (Supplier<T> sup, String key, int lockTime, ServiceException e) {
        //分布式锁
        boolean lock = false;
        try {
            log.debug("开始获取锁,key:{}", key);
            //获取锁
            lock = this.getLock(key, lockTime);
            if (lock) {
                log.debug("获取到锁");
                return sup.get();
            } else {
                log.debug("获取不到锁");
                //获取不到锁，操作太过频繁
                throw e;
            }
        } finally {
            log.debug("释放锁");
            //释放锁
            if (lock) {
                this.unLock(key);
            }
        }

    }

    /**
     * 统计bitmap中，value为1的个数
     * 非常适用于统计网站的每日活跃用户数等类似的场景
     *
     * @param key
     * @return
     */
    public Long bitCount(String key) {
        return redisTemplate.execute((RedisCallback<Long>) con -> con.bitCount(key.getBytes()));
    }

    /**
     * 二进制存储
     *
     * @param key    不存在时，自动生成一个新的字符串值。
     * @param offset 偏移量
     * @param value  true  false
     * @param unit   时间格式
     * @param time   过期时间
     * @return
     */
    public boolean setBit(String key, long offset, boolean value, long time, TimeUnit unit) {
        if (StrUtil.isEmpty(key)) {
            return false;
        }
        try {
            redisTemplate.opsForValue().setBit(key, offset, value);
            expire(key, time, unit);
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    public boolean setBit(String key, long offset, boolean value) {
        if (StrUtil.isEmpty(key)) {
            return false;
        }
        try {
            redisTemplate.opsForValue().setBit(key, offset, value);
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    public boolean getBit(String key, long offset) {
        if (StrUtil.isEmpty(key)) {
            return false;
        }
        return redisTemplate.opsForValue().getBit(key, offset);
    }


    //    ==========================  GEO 相关计算  =========================
    /**
     * GEO 添加批量地理位置（带过期时间）
     * @param key   键值
     * @param memberMap 批量添加对象和坐标
     * @param time  时间
     * @param unit  单位
     * @return
     */
    public boolean setGeoBatch(String key, Map<String, Point> memberMap, long time, TimeUnit unit) {
        if (StrUtil.isEmpty(key)) {
            return false;
        }
        try {
            // 创建GeoLocation列表
            List<RedisGeoCommands.GeoLocation<Object>> locations = new ArrayList<>();
            for (Map.Entry<String, Point> entry : memberMap.entrySet()) {
                locations.add(new RedisGeoCommands.GeoLocation<>(
                        entry.getKey(),
                        entry.getValue()
                ));
            }

            redisTemplate.opsForGeo().add(key, locations);
            expire(key, time, unit);
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * GEO 添加批量地理位置
     * @param key   键值
     * @param memberMap 批量添加对象和坐标
     * @return
     */
    public boolean setGeoBatch(String key, Map<String, Point> memberMap) {
        if (StrUtil.isEmpty(key)) {
            return false;
        }
        try {
            // 创建GeoLocation列表
            List<RedisGeoCommands.GeoLocation<Object>> locations = new ArrayList<>();
            for (Map.Entry<String, Point> entry : memberMap.entrySet()) {
                locations.add(new RedisGeoCommands.GeoLocation<>(
                        entry.getKey(),
                        entry.getValue()
                ));
            }

            redisTemplate.opsForGeo().add(key, locations);
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * GEO 添加单个地理位置
     * @param key   键值
     * @param point 坐标
     * @param member 对象
     * @return
     */
    public boolean setGeoOne(String key, Point point, String member) {
        if (StrUtil.isEmpty(key)) {
            return false;
        }
        try {
            redisTemplate.opsForGeo().add(key, point, member);
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * GEO 添加单个地理位置（带过期时间）
     * @param key   键值
     * @param point 坐标
     * @param member 对象
     * @param time  时间
     * @param unit  单位
     * @return
     */
    public boolean setGeoOne(String key, Point point, String member,long time, TimeUnit unit) {
        if (StrUtil.isEmpty(key)) {
            return false;
        }
        try {
            redisTemplate.opsForGeo().add(key, point, member);
            expire(key, time, unit);
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 获取两个地理位置之间的距离
     * @param key   键值
     * @param member1 对象1
     * @param member2 对象2
     * @param metric  单位
     * @return
     */
    public Double getGeoDistance(String key, String member1, String member2, Metric metric) {
        if (StrUtil.isEmpty(key)) {
            return 0.0;
        }
        try {
            Distance distance = redisTemplate.opsForGeo().distance(key, member1, member2,(ObjectUtil.isEmpty(metric))? Metrics.NEUTRAL :metric);
            return distance.getValue();
        } catch (Exception e) {
            return 0.0;
        }
    }

//    ==========================  GEO 相关计算  =========================
}