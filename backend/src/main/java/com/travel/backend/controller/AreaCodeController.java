package com.travel.backend.controller;


import com.travel.backend.domain.R;
import com.travel.backend.domain.dto.AreaCodeQueryDTO;
import com.travel.backend.domain.vo.AreaCodeNodeVO;
import com.travel.backend.service.AreaCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 省市区编码 前端控制器
 * </p>
 *
 * @author 自动生成
 * @since 2022-05-06
 */
@RestController
@RequestMapping(value = {"/areaCode"})
@Tag(name = "省市区相关")
public class AreaCodeController {

    @Autowired
    private AreaCodeService areaCodeService;

    @PostMapping("tree")
    @Operation(summary = "查询省市区，树形数据")
    public R<List<AreaCodeNodeVO>> getTree (@RequestBody AreaCodeQueryDTO dto) {
        return R.ok(areaCodeService.getTree(dto));
    }
}
