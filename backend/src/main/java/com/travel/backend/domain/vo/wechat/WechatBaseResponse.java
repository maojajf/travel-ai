package com.travel.backend.domain.vo.wechat;

import cn.hutool.core.util.ObjectUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author zhangpeng
 * @date 2022/9/5 0005
 * @description
 */
@Schema(description = "微信接口基本封装")
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class WechatBaseResponse implements Serializable {

    private final Integer successCode = 0;
    @Schema(description = "错误码 0 表示接口调用success 非0表示失败")
    private Integer errcode;

    @Schema(description = "错误信息")
    private String errmsg;

    @Schema(description = "微信返回数据")
    private String data;

    /**
     * @description: 校验接口调用返回是否success
     * @return: java.lang.Boolean
     **/
    public Boolean vaild(){
        return ObjectUtil.isEmpty(this.errcode) ? true : this.errcode.equals(successCode) ? true : false;
    }
}
