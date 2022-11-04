package com.athdu.travel.dianpingproject.controller;

import cn.hutool.core.bean.BeanUtil;
import com.athdu.travel.dianpingproject.dto.LoginFormDTO;
import com.athdu.travel.dianpingproject.dto.Result;
import com.athdu.travel.dianpingproject.dto.UserDTO;
import com.athdu.travel.dianpingproject.entity.User;
import com.athdu.travel.dianpingproject.entity.UserInfo;
import com.athdu.travel.dianpingproject.service.IUserInfoService;
import com.athdu.travel.dianpingproject.service.IUserService;
import com.athdu.travel.dianpingproject.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

/**
 * @author baizhejun
 * @create 2022 -10 -16 - 16:37
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {
   @Resource
   private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;

   /*
   发送手机验证码
    */
    @PostMapping("code")
    public Result sendCode(@RequestParam("phone")String phone, HttpSession session){

        return userService.sendCode(phone,session);
    }
/*/
    登录功能
 */
    @PostMapping("login")
    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session){

        return userService.login(loginForm,session);
    }

    // TODO 功能未实现
    @PostMapping("logout")
    public Result logout(){
        //登出功能
        return userService.logout();
    }
    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId){
        // 查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        // 返回
        return Result.ok(info);
    }

    @GetMapping("/{id}")
    public Result queryUserById(@PathVariable("id") Long userId){
        // 查询详情
        User user = userService.getById(userId);
        if (user == null) {
            return Result.ok();
        }
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        // 返回
        return Result.ok(userDTO);
    }
    @PostMapping("/sign")
    public Result sign(){
        return userService.sign();
    }

    @GetMapping("/sign/count")
    public Result signCount(){
        return userService.signCount();
    }

    @GetMapping("/me")
    public Result me(){
        // 获取当前登录的用户并返回
        UserDTO user = UserHolder.getUser();
        return Result.ok(user);
    }

}
