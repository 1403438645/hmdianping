package com.athdu.travel.dianpingproject.service.ServiceImpl;

import com.athdu.travel.dianpingproject.dto.Result;
import com.athdu.travel.dianpingproject.entity.SeckillVoucher;
import com.athdu.travel.dianpingproject.entity.Voucher;
import com.athdu.travel.dianpingproject.mapper.VoucherMapper;
import com.athdu.travel.dianpingproject.service.ISeckillVoucherService;
import com.athdu.travel.dianpingproject.service.IVoucherService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

import static com.athdu.travel.dianpingproject.utils.RedisConstants.SECKILL_STOCK_KEY;

/**
 * @author baizhejun
 * @create 2022 -10 -25 - 11:59
 */
@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryVoucherOfShop(Long shopId) {
        // 查询优惠券信息
        List<Voucher> vouchers = query().eq("shop_id",shopId).list();
        // 返回结果
        return Result.ok(vouchers);
    }

    @Override
    @Transactional
    public void addSeckillVoucher(Voucher voucher) {
        // 保存优惠券
        save(voucher);
        // 保存秒杀信息
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        seckillVoucherService.save(seckillVoucher);
        // 保存秒杀库存到Redis中
        stringRedisTemplate.opsForValue().set(SECKILL_STOCK_KEY + voucher.getId(), voucher.getStock().toString());
    }
}
