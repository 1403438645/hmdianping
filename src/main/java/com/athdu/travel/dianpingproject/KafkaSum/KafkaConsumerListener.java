//package com.athdu.travel.dianpingproject.KafkaSum;
//
//import com.alibaba.otter.canal.client.kafka.protocol.KafkaMessage;
//import com.athdu.travel.dianpingproject.entity.VoucherOrder;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.kafka.clients.consumer.ConsumerRecord;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.kafka.support.Acknowledgment;
//import org.springframework.stereotype.Component;
//
//import javax.annotation.Resource;
//
///**
// * @author baizhejun
// * @create 2022 -11 -01 - 15:01
// */
//@Component
//@Slf4j
//public class KafkaConsumerListener {
//
//    @Resource
//    KafkaTemplate<String,Object> kafkaTemplate;
//
//    @KafkaListener(topics = "dianpingproject",groupId = "group.demo")
//    public void kafkaListen(ConsumerRecord<String, VoucherOrder> record, Acknowledgment ack){
//
//        VoucherOrder value = record.value();
//
//        log.info(value.toString());
//        //        手动提交ack
//        ack.acknowledge();
//
//
//    }
//}
