package com.travel.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.backend.domain.model.KvConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author Mao
 * @time 2022-05-20
 */
@Mapper
public interface KvConfigMapper extends BaseMapper<KvConfig> {
}
