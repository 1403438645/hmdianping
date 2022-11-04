package com.athdu.travel.dianpingproject.service.ServiceImpl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.athdu.travel.dianpingproject.dto.Result;
import com.athdu.travel.dianpingproject.entity.ShopType;
import com.athdu.travel.dianpingproject.mapper.ShopTypeMapper;
import com.athdu.travel.dianpingproject.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jodd.time.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author baizhejun
 * @create 2022 -10 -17 - 14:58
 */
@Service
@Slf4j
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Override
    public Result showList(){
        String key = "{cache}:typeList";
        List<String> shopTypeList = new ArrayList<>();
        log.info("查询商铺i信息");
//        从redis里面查
        shopTypeList = stringRedisTemplate.opsForList().range(key,0,-1);
//        有就直接拿
        if(shopTypeList!= null &&shopTypeList.size() != 0){
            log.info("缓存不为空");
            List<ShopType> list= new ArrayList<>();
            for (String s:shopTypeList) {
//                先把String变为class
                ShopType shopType = JSONUtil.toBean(s,ShopType.class);
                list.add(shopType);
            }
            return Result.ok(list);
        }
//        没有就从sql查
        List<ShopType> typeList =query().orderByAsc("sort").list();
        log.info("查询数据库");
        if (typeList.isEmpty()){
            log.info("数据库为空");
            return Result.fail("信息初始化失败");
        }
        for (ShopType st: typeList){
            String s = JSONUtil.toJsonStr(st);
            shopTypeList.add(s);
        }
        stringRedisTemplate.opsForList().rightPushAll(key,shopTypeList);
//        stringRedisTemplate.expire(key,30L,TimeUnit.MINUTES);
        log.info("已经缓存到数据库");
        return Result.ok(typeList);

    }


}
