package com.athdu.travel.dianpingproject.service;

import com.athdu.travel.dianpingproject.dto.Result;
import com.athdu.travel.dianpingproject.entity.Voucher;
import com.baomidou.mybatisplus.extension.service.IService;


/**
 * <p>
 *  服务类
 * </p>
 *
 * @author baizhejun
 * @since 2021-12-22
 */
public interface IVoucherService extends IService<Voucher> {

    Result queryVoucherOfShop(Long shopId);

    void addSeckillVoucher(Voucher voucher);
}
