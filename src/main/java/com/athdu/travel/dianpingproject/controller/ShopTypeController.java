package com.athdu.travel.dianpingproject.controller;

import com.athdu.travel.dianpingproject.dto.Result;
import com.athdu.travel.dianpingproject.service.IShopTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author baizhejun
 * @create 2022 -10 -17 - 14:59
 */
@RestController
@RequestMapping("/shop-type")
public class ShopTypeController {
    @Resource
    private IShopTypeService typeService;

    @GetMapping("list")
    public Result queryTypeList(){

        return typeService.showList();
    }
}
