package com.travel.backend.service;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.travel.backend.domain.model.KvConfig;
import com.travel.backend.mapper.KvConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Mao
 * @time 2022-05-20
 */
@Service
@Slf4j
public class KvConfigService {

    @Autowired
    private KvConfigMapper kvConfigMapper;

    public KvConfig getEntityByConfigKey(String key) {
        QueryWrapper<KvConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(KvConfig::getConfigKey, key).eq(KvConfig::getStatus, 1);
        KvConfig one = kvConfigMapper.selectOne(queryWrapper);
        if (ObjectUtil.isEmpty(one)) {
            return null;
        }
        return one;
    }
}
