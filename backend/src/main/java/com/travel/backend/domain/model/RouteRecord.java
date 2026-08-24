package com.travel.backend.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@TableName("route_record")
@Accessors(chain = true)
public class RouteRecord {

    @TableId(value = "id", type = IdType.INPUT)
    private Integer id;

    private String deviceId;

    private String province;

    private String city;

    private Date startAt;

    private Date endAt;

    private String travelGroup;

    private String budgetLevel;

    private String summary;

    @TableField("content_markdown")
    private String contentMarkdown;

    private Date createdAt;

    private Date updatedAt;

    private Integer userId;
}
