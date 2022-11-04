package com.athdu.travel.dianpingproject.service;

import com.athdu.travel.dianpingproject.dto.Result;
import com.athdu.travel.dianpingproject.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;


/**
 * <p>
 *  服务类
 * </p>
 *
 * @author baizhejun
 * @since 2021-12-22
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    Result seckillVoucher(Long voucherId);


}
