package com.athdu.travel.dianpingproject.service.ServiceImpl;

import cn.hutool.core.bean.BeanUtil;
import com.athdu.travel.dianpingproject.dto.Result;
import com.athdu.travel.dianpingproject.entity.VoucherOrder;
import com.athdu.travel.dianpingproject.mapper.VoucherOrderMapper;
import com.athdu.travel.dianpingproject.service.ISeckillVoucherService;
import com.athdu.travel.dianpingproject.service.IVoucherOrderService;

import com.athdu.travel.dianpingproject.utils.RedisWorker;
import com.athdu.travel.dianpingproject.utils.UserHolder;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.athdu.travel.dianpingproject.utils.RedisConstants.TOPIC_NAME;

/**
 * @author baizhejun
 * @create 2022 -10 -20 - 16:29
 */
@Service
@Slf4j
public class VoucherOderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private RedisWorker redisworker;
    @Resource
    private RedissonClient redissonClient;


    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    @Resource
    private KafkaTemplate<String, VoucherOrder> kafkaTemplate;


    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    //    创建线程池，执行异步操作
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    //    初始化之后在执行
    @PostConstruct
    private void init() {
        SECKILL_ORDER_EXECUTOR.submit(new VourcherOrderHandler());
    }



    private class VourcherOrderHandler implements Runnable {


        @Override
        public void run() {
            while(true){
                try {
//                    1.获取消息队列中的订单信息  XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS s1 >
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create("stream.orders", ReadOffset.lastConsumed())
                    );

//                    2。判断订单信息是否为空
                    if (list == null || list.isEmpty()){
                        continue;
                    }
//                    解析数据
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> value = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
//                    创建订单
                    createVoucherOrder(voucherOrder);
//                    确认XACK
                    stringRedisTemplate.opsForStream().acknowledge("stream.orders","g1",record.getId());
                }catch (Exception e){

                    log.error("处理订单异常",e);
                    handlePendingList();
                }
            }
        }
    }

         //   处理异常情况： 获取pending-list中订单的信息
    private void handlePendingList(){
        while(true){
            try {
                // 1.获取pending-list中的订单信息 XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS s1 0
                List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                        Consumer.from("g1", "c1"),
                        StreamReadOptions.empty().count(1),
                        StreamOffset.create("stream.orders", ReadOffset.from("0"))
                );
//            判断订单信息是否为空
                if (list == null || list.isEmpty()){
                    break;
                }
//            解析数据
                MapRecord<String, Object, Object> record = list.get(0);
                Map<Object, Object> value = record.getValue();
                VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
//            创建订单
                createVoucherOrder(voucherOrder);
//            确认XACK
                stringRedisTemplate.opsForStream().acknowledge("stream.orders","g1",record.getId());
            }catch (Exception e){
                log.error("处理订单异常",e);
            }


        }
    }
//        //用来发送消息
//        @Transactional
//        public void kafkaProducer(Long voucherId) {
//            Long userId = UserHolder.getUser().getId();
//            Long orderId = redisworker.nextId("order");
//            VoucherOrder voucherOrder = new VoucherOrder();
//            voucherOrder.setVoucherId(voucherId);
//            voucherOrder.setId(orderId);
//            voucherOrder.setUserId(userId);
//            kafkaTemplate.send(TOPIC_NAME, voucherOrder);
//
//        }


        //    用来判断有没有跟购买资格
        public Result seckillVoucher(Long voucherId) {
            Long userId = UserHolder.getUser().getId();
            Long orderId = redisworker.nextId("order");
            //    执行lua脚本
            Long result = stringRedisTemplate.execute(
                    SECKILL_SCRIPT,
                    Collections.emptyList(),
                    voucherId.toString(), userId.toString(), String.valueOf(orderId)
            );
//            kafkaProducer(voucherId);

            int r = result.intValue();


//    2.判断结果是否为0
            if (r != 0) {
//        2.1不为0说明失败
                return Result.fail(r == 1 ? "库存不足" : " 不能重复购买");

            }
//    返回订单id
            return Result.ok(orderId);
        }

        //    用来判断有没有跟购买资格
//public Result seckillVoucher(Long voucherId){
//    Long userId = UserHolder.getUser().getId();
//    Long orderId = redisworker.nextId("order");
////    执行lua脚本
//    Long result = stringRedisTemplate.execute(
//            SECKILL_SCRIPT,
//            Collections.emptyList(),
//            voucherId.toString(),userId.toString(),orderId.toString()
//    );
//    int r = result.intValue();
////    2.判断结果是否为0
//    if (r != 0){
////        2.1不为0说明失败
//        return Result.fail(r==1 ?"库存不足" : " 不能重复购买");
//
//    }
////    返回订单id
//    return Result.ok(orderId);
//}
//    @Override
//    public Result seckillVoucher(Long voucherId) {
////        1.查询优惠卷
//        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
////        2.判断秒杀是否开始
//        if (voucher.getBeginTime().isAfter(LocalDateTime.now())){
////            尚未开始
//            return Result.fail("秒杀尚未开始");
//
//        }
//        if (voucher.getEndTime().isBefore(LocalDateTime.now())){
////            已经解说
//            return Result.fail("秒杀尚未结束");
//
//        }
//        if (voucher.getStock() < 1){
////            库存不足
//            return Result.fail("库存不足");
//        }
//        return createVoucherOrder(voucherId);
//    }
        private void createVoucherOrder(VoucherOrder voucherOrder) {
//        5.一人一单
            Long userId = voucherOrder.getUserId();
//        1.创建锁对象
//        SimpleRedisLock lock = new SimpleRedisLock("lock:order:" + userId,stringRedisTemplate);
//            创建连锁
//            RLock lock = redissonClient.getLock("{lock}:order"+userId);



//        2.尝试获取锁
//            boolean isLock = lock.tryLock();
//        判断
//            if (!isLock) {
//                log.error("不能重复下单");
//                return;
//            }

                //        4.查询订单
                int count = query().eq("user_id", userId).eq("voucher_id", voucherOrder.getVoucherId()).count();
//        5.判断是否存在
                if (count > 0) {
                    log.error("不允许重复下单");
                    return;
                }
//        6扣减库存
                boolean success = seckillVoucherService.update().setSql("stock = stock -1").eq("voucher_id", voucherOrder.getVoucherId()).gt("stock", 0).update();
                if (!success) {
//            减少库存失败
                    log.error("库存不足");
                    return;

                }
////        7.创建订单
//            VoucherOrder voucherOrder = new VoucherOrder();
////        订单id
//            Long orderId = redisworker.nextId("order");
//            voucherOrder.setId(orderId);
////        用户id
//            voucherOrder.setUserId(userId);
////        代金券id
//            voucherOrder.setVoucherId(voucherId);
                save(voucherOrder);
////        8.返回订单id
//            return ;



//                lock.unlock();



        }
    }

