package com.travel.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.backend.domain.model.RouteRecord;
import com.travel.backend.domain.vo.RouteListVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RouteRecordMapper extends BaseMapper<RouteRecord> {
    /**
     * 获取用户历史记录
     */
    List<RouteListVo> listHistory(@Param("userId") Integer userId);
     /**
     * 获取用户收藏记录
     */
    List<RouteListVo> listFavorites(@Param("userId")Integer userId);
}
