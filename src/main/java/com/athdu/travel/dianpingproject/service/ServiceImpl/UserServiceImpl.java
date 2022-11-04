package com.athdu.travel.dianpingproject.service.ServiceImpl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import com.athdu.travel.dianpingproject.dto.LoginFormDTO;
import com.athdu.travel.dianpingproject.dto.Result;
import com.athdu.travel.dianpingproject.dto.UserDTO;
import com.athdu.travel.dianpingproject.entity.User;
import com.athdu.travel.dianpingproject.mapper.UserMapper;
import com.athdu.travel.dianpingproject.service.IUserService;
import com.athdu.travel.dianpingproject.utils.RegexUtils;
import com.athdu.travel.dianpingproject.utils.UserHolder;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.hutool.core.lang.UUID;

import java.util.concurrent.TimeUnit;

import static com.athdu.travel.dianpingproject.utils.RedisConstants.USER_SIGN_KEY;

/**
 * @author baizhejun
 * @create 2022 -10 -16 - 16:17
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
//       1.校验手机号
        if (RegexUtils.isPhoneInvalid(phone)){
            //2.返回错误信息
            return Result.fail("手机号格式错误!");
        }
//        生成手机验证码
        String code  = RandomUtil.randomNumbers(6);
        log.info(code);
//        保存验证码到session redis
        stringRedisTemplate.opsForValue().set("login:"+phone,code,2L, TimeUnit.MINUTES);
        log.debug("发送验证码成功");
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号格式错误！");
        }
//        从redis获取验证码并校验
        String cacheCode =stringRedisTemplate.opsForValue().get("login:"+phone);
        String code = loginForm.getCode();
        if (cacheCode == null || !cacheCode.equals(code)){
//            不一致报错
            return Result.fail("验证码错误");
        }
//        查询用户 Select * from tb_user where phone = ?
        User user  = query().eq("phone",phone).one();

//        判断用户是否存在
        if (user == null) {
//            创建新用户，保存
            user = createUserWithPhone(phone);
        }
//        保存用户信息到redis
//        随机生成token作为登录令牌
        String token = UUID.randomUUID().toString(true);
//        去掉多余信息
        UserDTO userDTO = BeanUtil.copyProperties(user,UserDTO.class);
        Map<String,Object> userMap = BeanUtil.beanToMap(userDTO,new HashMap<>(), CopyOptions.create().setIgnoreNullValue(true).setFieldValueEditor((fieldName,fieldValue) -> fieldValue.toString()));
//        存储
        String tokenKey = "{login:token}"+token;
        stringRedisTemplate.opsForHash().putAll(tokenKey,userMap);
//        设置token有效期
        stringRedisTemplate.expire(tokenKey,30,TimeUnit.MINUTES);
        return Result.ok(token);
    }


    @Override
    public Result signCount() {
//        1.获取当前登录用户
        Long userId = UserHolder.getUser().getId();
//        获取日期
        LocalDateTime now = LocalDateTime.now();
//        拼接key
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
//        获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
//         5.获取本月截止今天为止的所有的签到记录，返回的是一个十进制的数字 BITFIELD sign:5:202203 GET u14 0
        List<Long> result = stringRedisTemplate.opsForValue().bitField(
                key,
                BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0)

        );
//健壮性判断
        if (result == null || result.isEmpty()){
//            没有任何签到解雇
            return Result.ok(0);
        }
        Long num = result.get(0);
        if (num == null|| num == 0){
            return Result.ok(0);
        }
//        循环遍历
        int count = 0;
        int res = 0;
        for (int i = 0;i<dayOfMonth;i++){
            if ((num & 1) == 0){
                res = Math.max(res,count);
                count = 0;
            }else{
//                如果不为0，说明已签到，计数器加一
                count++;
            }
//            把数字右移一位，抛弃最后一个bit位
            num = num>>1;
        }




        return Result.ok(res);
    }

    @Override
    public Result logout() {
        UserHolder.removeUser();


        return Result.ok("已退出");
    }

    @Override
    public Result sign() {
        // 1.获取当前登录用户
        Long userId = UserHolder.getUser().getId();
        // 2.获取日期
        LocalDateTime now = LocalDateTime.now();
        // 3.拼接key
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
        // 4.获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        // 5.写入Redis SETBIT key offset 1
        stringRedisTemplate.opsForValue().setBit(key, dayOfMonth - 1, true);
        return Result.ok();

    }

    private User createUserWithPhone(String phone) {
//        创建用户
        User user = new User();
        user.setPhone(phone);
        user.setNickName("user_" + RandomUtil.randomString(10));
//        保存用户
        save(user);
        return user;
    }

}
