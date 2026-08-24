package com.travel.backend.service;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.travel.backend.domain.dto.AreaCodeQueryDTO;
import com.travel.backend.domain.model.AreaCode;
import com.travel.backend.domain.vo.AreaCodeNodeVO;
import com.travel.backend.mapper.AreaCodeMapper;
import com.travel.backend.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Mao
 * @time 2022-05-20
 */
@Service
@Slf4j
public class AreaCodeService {

    @Autowired
    private AreaCodeMapper areaCodeMapper;

    @Autowired
    private RedisUtil redisUtil;


    private static Map<String, String> CITY_CODE = new HashMap<>();
    static {
        CITY_CODE.put("310100", "310000");
        CITY_CODE.put("500100", "500000");
        CITY_CODE.put("120200", "120000");
        CITY_CODE.put("110100", "110000");
        CITY_CODE.put("810100", "810000");
        CITY_CODE.put("820100", "820000");
    }

    public List<AreaCodeNodeVO> getTree(AreaCodeQueryDTO dto) {
        String redisKey = "travel:area:code:node:" + JSONObject.toJSONString(dto);
        if (redisUtil.hasKey(redisKey)) {
            return (List<AreaCodeNodeVO>) redisUtil.get(redisKey);
        }

        QueryWrapper<AreaCode> queryWrapper = new QueryWrapper<>();
        if (ObjectUtil.isNotEmpty(dto.getLevel())) {
            switch (dto.getLevel()) {
                case 1:
                    queryWrapper.lambda().eq(AreaCode::getLevel, 1);
                    break;
                case 2:
                    queryWrapper.lambda().in(AreaCode::getLevel, Arrays.asList(1, 2));
                    break;
                default:
                    break;
            }
        }
        if (ObjectUtil.isNotEmpty(dto.getName())) {
            dto.setName(
                    dto.getName().replaceAll("%", "\\\\%").replaceFirst("_", "\\\\_")
                            .replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]")
            );

            queryWrapper.lambda().like(AreaCode::getName, dto.getName());
        }
        if (ObjectUtil.isNotEmpty(dto.getPcode())) {
            queryWrapper.lambda().eq(AreaCode::getPcode, dto.getPcode());
        }
        queryWrapper.lambda().orderByAsc(AreaCode::getWeight);
        List<AreaCode> list = areaCodeMapper.selectList(queryWrapper);
        List<AreaCodeNodeVO> areaCodeNodeVOList = list.stream().filter((item) -> ObjectUtil.isEmpty(item.getPcode())).map((po) -> {
            AreaCodeNodeVO areaCodeNodeVO = new AreaCodeNodeVO();
            BeanUtils.copyProperties(po, areaCodeNodeVO);
            areaCodeNodeVO.setChildren(generateTree(areaCodeNodeVO, list));
            return areaCodeNodeVO;
        }).sorted(Comparator.comparingInt(vo -> (vo.getWeight() == null ? 0 : vo.getWeight()))).collect(Collectors.toList());

        redisUtil.set(redisKey, areaCodeNodeVOList,5, TimeUnit.MINUTES);
        return areaCodeNodeVOList;
    }

    /**
     * 递归生成树
     *
     * @param root
     * @param all
     * @return
     */
    private List<AreaCodeNodeVO> generateTree(AreaCodeNodeVO root, List<AreaCode> all) {
        List<AreaCodeNodeVO> children = all.stream().filter((vo) -> vo.getPcode().equals(root.getCode())).map((menu) -> {
            AreaCodeNodeVO areaCodeNodeVO = new AreaCodeNodeVO();
            BeanUtils.copyProperties(menu, areaCodeNodeVO);
            //找到子菜单
            areaCodeNodeVO.setChildren(generateTree(areaCodeNodeVO, all));

            if (CITY_CODE.containsKey(areaCodeNodeVO.getCode())) {
                areaCodeNodeVO.setCode(CITY_CODE.get(areaCodeNodeVO.getCode()));
            }
            return areaCodeNodeVO;
        }).sorted(Comparator.comparingInt(menu -> (menu.getWeight() == null ? 0 : menu.getWeight()))).collect(Collectors.toList());
        return children;
    }
}
