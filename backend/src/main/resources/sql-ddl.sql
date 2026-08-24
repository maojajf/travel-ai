CREATE TABLE `user` (
   `id` int unsigned NOT NULL AUTO_INCREMENT,
   `uopenid` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '用户openid',
   `unionid` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户唯一unionid',
   `status` tinyint DEFAULT '1' COMMENT '1.正常 2.冻结 3.其他',
   `last_login_date` timestamp NULL DEFAULT NULL COMMENT '最后登录时间',
   `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
   `silent_login` tinyint DEFAULT '0' COMMENT '用户静默登录标识，1 可静默登录，如用户手动退出，需要改为0',
   `session_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '用户sessionkey',
   PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='微信用户表';

CREATE TABLE `route_record` (
    `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'id',
    `device_id` varchar(128) DEFAULT NULL COMMENT '设备id',
    `province` varchar(64) DEFAULT NULL COMMENT '省',
    `city` varchar(64) DEFAULT NULL COMMENT '市',
    `start_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '开始日期',
    `end_at` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' COMMENT '结束日期',
    `travel_group` varchar(32) NOT NULL COMMENT '出游人群',
    `budget_level` varchar(32) NOT NULL COMMENT '预算档位',
    `summary` varchar(512) NOT NULL COMMENT '摘要',
    `content_markdown` longtext COMMENT '生成数据',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `user_id` int(11) DEFAULT NULL COMMENT '用户id',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='路线生成表';

CREATE TABLE `route_favorite` (
      `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'id',
      `route_id` int(11) NOT NULL COMMENT '路线id',
      `user_id` int(11) DEFAULT NULL COMMENT '用户id',
      `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      PRIMARY KEY (`id`),
      KEY `idx_route_device` (`route_id`,`user_id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='路线收藏表';

CREATE TABLE `area_code` (
     `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
     `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '名称',
     `level` int NOT NULL DEFAULT '1' COMMENT '等级 1-省 2-市 3-区',
     `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT 'level对应唯一code',
     `pcode` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '父级code',
     `weight` int unsigned NOT NULL DEFAULT '0' COMMENT '权重-正序',
     `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 1-正常',
     `lng` decimal(16,10) DEFAULT '0.0000000000' COMMENT '经度',
     `lat` decimal(16,10) DEFAULT '0.0000000000' COMMENT '纬度',
     `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '备注',
     `create_time` datetime DEFAULT NULL COMMENT '创建时间',
     `update_time` datetime DEFAULT NULL COMMENT '修改时间',
     `pinyin` varchar(60) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '拼音（全小写）',
     `pinyin_short` varchar(60) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '首字母拼音（全小写）',
     PRIMARY KEY (`id`) USING BTREE,
     UNIQUE KEY `code` (`code`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3639 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='省市区编码';