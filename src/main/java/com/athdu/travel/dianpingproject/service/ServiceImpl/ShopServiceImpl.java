package com.athdu.travel.dianpingproject.service.ServiceImpl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.athdu.travel.dianpingproject.dto.Result;
import com.athdu.travel.dianpingproject.entity.Shop;
import com.athdu.travel.dianpingproject.mapper.ShopMapper;
import com.athdu.travel.dianpingproject.service.IShopService;
import com.athdu.travel.dianpingproject.utils.CacheClient;
import com.athdu.travel.dianpingproject.utils.RedisData;
import com.athdu.travel.dianpingproject.utils.SystemConstants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisCommands;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.scheduling.quartz.LocalDataSourceJobStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.athdu.travel.dianpingproject.utils.RedisConstants.*;

/**
 * @author baizhejun
 * @create 2022 -10 -17 - 14:20
 */
@Service
@Slf4j
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
//    新建线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Resource
    CacheClient cacheClient;
    @Override
    public Result queryById(Long id) {
// 解决缓存穿透
        Shop shop = cacheClient
                .queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_NULL_TTL, TimeUnit.MINUTES);

        // 互斥锁解决缓存击穿
        // Shop shop = cacheClient
        //         .queryWithMutex(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        // 逻辑过期解决缓存击穿
        // Shop shop = cacheClient
        //         .queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, 20L, TimeUnit.SECONDS);
        if (shop == null){
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
    }

    @Override
    @Transactional
// TODO 采用异步cannel方式解决数据一致问题
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null){
            return Result.fail("店铺id不能为空");
        }
//        更新数据库
        updateById(shop);

//        删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY+id);
        return Result.ok();
    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
//        1.判断是否需要根据坐标查询，按数据库查询
        if (x == null || y == null){
//            不需要坐标查询，直接数据库查询
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            return Result.ok(page.getRecords());
        }
//        计算分页参数

        int from = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        int end = current * SystemConstants.DEFAULT_PAGE_SIZE;
        log.info("end = ",end,"begin=" ,from);
//        查询redis、按照距离排序、分页。结果：shopId、distance
        String key = SHOP_GEO_KEY + typeId;
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo().search(
                key,
                GeoReference.fromCoordinate(x, y),
                new Distance(5000),
//                带上距离
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end)
        );

//        解析出id
        if (results == null){
            log.info("results == null");
            return Result.ok(Collections.emptyList());
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();
        if (list.size() <=from){
            log.info("list.size() <=from");
//            没有下一页了，结束
            return Result.ok(Collections.emptyList());
        }
//        截取from - end 部分
        Map<String, Distance> distanceMap = new HashMap<>(list.size());
        ArrayList<Object> ids = new ArrayList<>(list.size());
        list.stream().skip(from).forEach(result -> {
//            获取店铺id
            String shopIdStr = result.getContent().getName();
            ids.add(Long.valueOf(shopIdStr));

            Distance distance = result.getDistance();
            distanceMap.put(shopIdStr,distance);

        });
//        根据id查询shop

        log.info("根据id查询shop");
        String str = StrUtil.join(",", ids);
        List<Shop> shops = query().in("id", ids).last("ORDER BY FIELD(id ," + str + ")").list();
        for (Shop shop : shops){
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }



        return Result.ok(shops);
    }

//    public Shop passThrough(Long id){
//        String key = CACHE_SHOP_KEY + id;
////1.从redis查询商铺缓存
//        String shopJson  =  stringRedisTemplate.opsForValue().get(key);
////        2.判断是否存在
//        if (StrUtil.isNotBlank(shopJson)){
//            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
//            return shop;
//        }
////        判断命中的是不是空值,不是空值，直接返回null
//        if (shopJson != null){
//            return null;
//        }
////        不存在，根据id查询数据库
//        Shop shop  = getById(id);
////        mysql不存在，返回错误信息
//        if (shop == null){
//            stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL,TimeUnit.MINUTES);
//            return null;
//
//        }
////        存在就写入redis
//        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop));
//        stringRedisTemplate.expire(key,30, TimeUnit.MINUTES);
////        返回
//        return shop;
//    }
//    public Shop queryWithMutex(Long id){
//        String key = CACHE_SHOP_KEY + id;
////1.从redis查询商铺缓存
//        String shopJson  =  stringRedisTemplate.opsForValue().get(key);
////        2.判断是否存在
//        if (StrUtil.isNotBlank(shopJson)){
//            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
//            return shop;
//        }
////        判断命中的是不是空值,不是空值，直接返回null
//        if (shopJson != null){
//            return null;
//        }
////        4.实现缓存重建
////        4.1 获取互斥锁
//        String lockKey  = CATCHE_LOCK_SHOP + id;
//        Shop shop = null;
//        try {
//            boolean isLock = tryLock(lockKey);
//            if (!isLock){
//                Thread.sleep(50);
//                return queryWithMutex(id);
//            }
////            获取锁成功，根据id查询数据库
//            shopJson = stringRedisTemplate.opsForValue().get(key);
//            if (StrUtil.isBlank(shopJson)){
//                shop  = getById(id);
//            }
//            shop = JSONUtil.toBean(shopJson,Shop.class);
//
//            //        mysql不存在，返回错误信息
//            if (shop == null){
//                stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL,TimeUnit.MINUTES);
//                return null;
//
//            }
////        存在就写入redis
//            stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop));
//            stringRedisTemplate.expire(key,30, TimeUnit.MINUTES);
//
//        }catch (InterruptedException e){
//            e.printStackTrace();
//        }
//
////       释放互斥锁
//        unlock(key);
//
//        return shop;
//
//
//    }
////    用逻辑过期去解决
//    public Shop LogicalExpire(Long id){
//        String key = CACHE_SHOP_KEY + id;
////1.从redis查询商铺缓存
//        String shopJson  =  stringRedisTemplate.opsForValue().get(key);
////        2.判断是否存在
//        if (StrUtil.isBlank(shopJson)){
////            不存在返回null
//            return null;
//        }
////        4.命中，需把json反序列化为对象
//        RedisData redisData = JSONUtil.toBean(shopJson,RedisData.class);
////        redisData.get本身是一个JsonObject对象
//        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(),Shop.class);
//        LocalDateTime expireTime = redisData.getLocalDateTime();
////        5.判断是否过期
//        if (expireTime.isAfter(LocalDateTime.now())){
//            return shop;
//        }
////        6.过期了，需要缓存重建
//        String lockKey = CATCHE_LOCK_SHOP +id;
//        boolean isLock = tryLock(lockKey);
////        判断是否获取锁成功
//        if (isLock) {
////            成功，开启独立线程，实现线程缓存
//            CACHE_REBUILD_EXECUTOR.submit(() ->{
//                try {
//                    //                重建缓存
//                    this.saveShop2Redis(id,20L);
//
//                }catch(Exception e) {
//                    throw new RuntimeException(e);
//                }finally{
//                    //                释放锁
//                    unlock(lockKey);
//                }
//
//
//            });
////            返回过期的商铺信息
//
//        }
//        return shop;
//    }
//    private boolean tryLock(String key){
//        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key,"1",10,TimeUnit.SECONDS);
//        return BooleanUtil.isTrue(flag);
//    }
//    public void unlock(String key){
//        stringRedisTemplate.delete(key);
//    }
//    private void saveShop2Redis(Long id,Long time){
////        查询店铺数据
//        Shop shop = getById(id);
////        封装逻辑过期时间
//        RedisData redisData = new RedisData();
//        redisData.setData(shop);
//        redisData.setLocalDateTime(LocalDateTime.now().plusSeconds(time));
////        写入redis
//        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,JSONUtil.toJsonStr((redisData)));
//    }
}
