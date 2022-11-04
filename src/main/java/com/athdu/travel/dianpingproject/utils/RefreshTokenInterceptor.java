package com.athdu.travel.dianpingproject.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.athdu.travel.dianpingproject.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author baizhejun
 * @create 2022 -10 -16 - 17:59
 * 用于刷新token时间的，所以接到任何命令都会放行
 */
public class RefreshTokenInterceptor implements HandlerInterceptor {
    private StringRedisTemplate stringRedisTemplate;
//    因为不是spring配置类，所以不能直接注入
    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        获取请求头中的token
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)){
            return true;
        }
//        基于token获取redis的用户
        String key = "{login:token}" + token;
        Map<Object,Object> userMap = stringRedisTemplate.opsForHash().entries(key);
//        判读用户是否存在
        if (userMap.isEmpty()){
            return true;
        }
//        将查询的hash数据转为userDTO
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
//        存在，就将用户信息存到ThreadLoacl
        UserHolder.saveUser(userDTO);
//        刷新token的有效期
        stringRedisTemplate.expire(key,30, TimeUnit.MINUTES);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
//        移除用户
        UserHolder.removeUser();
    }

}
