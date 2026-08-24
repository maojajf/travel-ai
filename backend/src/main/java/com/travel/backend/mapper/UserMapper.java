package com.travel.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.backend.domain.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户
 *
 * @author Sanjeev 252126641@qq.com
 * @since 1.0.0 2024-11-28
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    User getByUopenid(@Param("openid") String openid);
}