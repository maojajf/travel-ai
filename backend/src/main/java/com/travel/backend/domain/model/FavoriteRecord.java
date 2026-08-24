package com.travel.backend.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("route_favorite")
public class FavoriteRecord {

    @TableId(value = "id", type = IdType.INPUT)
    private Integer id;

    private Integer routeId;

    private Integer userId;

    private LocalDateTime createdAt;
}
