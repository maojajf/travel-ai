package com.travel.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.backend.domain.model.FavoriteRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RouteFavoriteMapper extends BaseMapper<FavoriteRecord> {
}
