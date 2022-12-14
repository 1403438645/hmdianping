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

    //    ????????????????????????????????????
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    //    ????????????????????????
    @PostConstruct
    private void init() {
        SECKILL_ORDER_EXECUTOR.submit(new VourcherOrderHandler());
    }



    private class VourcherOrderHandler implements Runnable {


        @Override
        public void run() {
            while(true){
                try {
//                    1.????????????????????????????????????  XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS s1 >
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create("stream.orders", ReadOffset.lastConsumed())
                    );

//                    2?????????????????????????????????
                    if (list == null || list.isEmpty()){
                        continue;
                    }
//                    ????????????
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> value = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
//                    ????????????
                    createVoucherOrder(voucherOrder);
//                    ??????XACK
                    stringRedisTemplate.opsForStream().acknowledge("stream.orders","g1",record.getId());
                }catch (Exception e){

                    log.error("??????????????????",e);
                    handlePendingList();
                }
            }
        }
    }

         //   ????????????????????? ??????pending-list??????????????????
    private void handlePendingList(){
        while(true){
            try {
                // 1.??????pending-list?????????????????? XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS s1 0
                List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                        Consumer.from("g1", "c1"),
                        StreamReadOptions.empty().count(1),
                        StreamOffset.create("stream.orders", ReadOffset.from("0"))
                );
//            ??????????????????????????????
                if (list == null || list.isEmpty()){
                    break;
                }
//            ????????????
                MapRecord<String, Object, Object> record = list.get(0);
                Map<Object, Object> value = record.getValue();
                VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
//            ????????????
                createVoucherOrder(voucherOrder);
//            ??????XACK
                stringRedisTemplate.opsForStream().acknowledge("stream.orders","g1",record.getId());
            }catch (Exception e){
                log.error("??????????????????",e);
            }


        }
    }
//        //??????????????????
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


        //    ????????????????????????????????????
        public Result seckillVoucher(Long voucherId) {
            Long userId = UserHolder.getUser().getId();
            Long orderId = redisworker.nextId("order");
            //    ??????lua??????
            Long result = stringRedisTemplate.execute(
                    SECKILL_SCRIPT,
                    Collections.emptyList(),
                    voucherId.toString(), userId.toString(), String.valueOf(orderId)
            );
//            kafkaProducer(voucherId);

            int r = result.intValue();


//    2.?????????????????????0
            if (r != 0) {
//        2.1??????0????????????
                return Result.fail(r == 1 ? "????????????" : " ??????????????????");

            }
//    ????????????id
            return Result.ok(orderId);
        }

        //    ????????????????????????????????????
//public Result seckillVoucher(Long voucherId){
//    Long userId = UserHolder.getUser().getId();
//    Long orderId = redisworker.nextId("order");
////    ??????lua??????
//    Long result = stringRedisTemplate.execute(
//            SECKILL_SCRIPT,
//            Collections.emptyList(),
//            voucherId.toString(),userId.toString(),orderId.toString()
//    );
//    int r = result.intValue();
////    2.?????????????????????0
//    if (r != 0){
////        2.1??????0????????????
//        return Result.fail(r==1 ?"????????????" : " ??????????????????");
//
//    }
////    ????????????id
//    return Result.ok(orderId);
//}
//    @Override
//    public Result seckillVoucher(Long voucherId) {
////        1.???????????????
//        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
////        2.????????????????????????
//        if (voucher.getBeginTime().isAfter(LocalDateTime.now())){
////            ????????????
//            return Result.fail("??????????????????");
//
//        }
//        if (voucher.getEndTime().isBefore(LocalDateTime.now())){
////            ????????????
//            return Result.fail("??????????????????");
//
//        }
//        if (voucher.getStock() < 1){
////            ????????????
//            return Result.fail("????????????");
//        }
//        return createVoucherOrder(voucherId);
//    }
        private void createVoucherOrder(VoucherOrder voucherOrder) {
//        5.????????????
            Long userId = voucherOrder.getUserId();
//        1.???????????????
//        SimpleRedisLock lock = new SimpleRedisLock("lock:order:" + userId,stringRedisTemplate);
//            ????????????
//            RLock lock = redissonClient.getLock("{lock}:order"+userId);



//        2.???????????????
//            boolean isLock = lock.tryLock();
//        ??????
//            if (!isLock) {
//                log.error("??????????????????");
//                return;
//            }

                //        4.????????????
                int count = query().eq("user_id", userId).eq("voucher_id", voucherOrder.getVoucherId()).count();
//        5.??????????????????
                if (count > 0) {
                    log.error("?????????????????????");
                    return;
                }
//        6????????????
                boolean success = seckillVoucherService.update().setSql("stock = stock -1").eq("voucher_id", voucherOrder.getVoucherId()).gt("stock", 0).update();
                if (!success) {
//            ??????????????????
                    log.error("????????????");
                    return;

                }
////        7.????????????
//            VoucherOrder voucherOrder = new VoucherOrder();
////        ??????id
//            Long orderId = redisworker.nextId("order");
//            voucherOrder.setId(orderId);
////        ??????id
//            voucherOrder.setUserId(userId);
////        ?????????id
//            voucherOrder.setVoucherId(voucherId);
                save(voucherOrder);
////        8.????????????id
//            return ;



//                lock.unlock();



        }
    }

