package com.athdu.travel.dianpingproject.service.ServiceImpl;

import com.athdu.travel.dianpingproject.entity.UserInfo;
import com.athdu.travel.dianpingproject.mapper.UserInfoMapper;
import com.athdu.travel.dianpingproject.service.IUserInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * @author baizhejun
 * @create 2022 -10 -25 - 11:59
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {
}
