package com.athdu.travel.dianpingproject.service;

import com.athdu.travel.dianpingproject.dto.LoginFormDTO;
import com.athdu.travel.dianpingproject.dto.Result;
import com.athdu.travel.dianpingproject.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpSession;

/**
 * @author baizhejun
 * @create 2022 -10 -16 - 16:16
 */
public interface IUserService extends IService<User> {

    Result sendCode(String phone, HttpSession session);

    Result login(LoginFormDTO loginForm, HttpSession session);

    Result sign();

    Result signCount();

    Result logout();
}
