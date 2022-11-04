package com.athdu.travel.dianpingproject.utils;

import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author baizhejun
 * @create 2022 -10 -16 - 17:47
 */
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        1.判断是否需要拦截（threadlocal中是否有用户）
        if (UserHolder.getUser() == null){
            response.setStatus(401);
            //拦截
            return false;
        }
        return true;
    }

}
