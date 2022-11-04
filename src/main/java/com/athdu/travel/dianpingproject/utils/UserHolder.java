package com.athdu.travel.dianpingproject.utils;

import com.athdu.travel.dianpingproject.dto.UserDTO;

/**
 * @author baizhejun
 * @create 2022 -10 -16 - 17:50
 */
public class UserHolder {
    public static final ThreadLocal<UserDTO> tl = new ThreadLocal<>();
    public static void saveUser(UserDTO user){
        tl.set(user);
    }
    public static UserDTO getUser(){
        return tl.get();

    }
    public static void removeUser(){
        tl.remove();
    }
}
