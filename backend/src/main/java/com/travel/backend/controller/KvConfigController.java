package com.travel.backend.controller;

import cn.hutool.core.util.ObjectUtil;
import com.travel.backend.domain.R;
import com.travel.backend.domain.model.KvConfig;
import com.travel.backend.service.KvConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author Mao
 * @time 2022-05-20
 */
@Tag(name = "公共配置管理")
@RestController
@RequestMapping("/common")
public class KvConfigController {

    @Autowired
    private KvConfigService kvConfigService;

    @Operation(summary = "通过key获取配置数据")
    @PostMapping("/getValue")
    public R<String> getConfigValueByKey(@RequestParam("key") String key) {
        KvConfig entity = kvConfigService.getEntityByConfigKey(key);
        return R.ok((ObjectUtil.isEmpty(entity)) ?null :entity.getConfigValue());
    }

}
