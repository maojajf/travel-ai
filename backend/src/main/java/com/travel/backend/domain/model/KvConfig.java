package com.travel.backend.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * @author Mao
 * @time 2022-05-20
 * @decr xxx
 */
@Data
@TableName("kv_config")
public class KvConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String configKey;

    /**
     * 配置value
     */
    private String configValue;

    /**
     * 状态 1可用 2删除 3冻结
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;
}
