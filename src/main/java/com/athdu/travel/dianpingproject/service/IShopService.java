package com.athdu.travel.dianpingproject.service;

import com.athdu.travel.dianpingproject.dto.Result;
import com.athdu.travel.dianpingproject.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;


/**
 * <p>
 *  服务类
 * </p>
 *
 * @author baizhejun
 * @since 2021-12-22
 */
public interface IShopService extends IService<Shop> {

    Result queryById(Long id);

    Result update(Shop shop);

    Result queryShopByType(Integer typeId, Integer current, Double x, Double y);
}
