package com.mc.yupicturebackend.utils;

import org.springframework.util.DigestUtils;

import static com.mc.yupicturebackend.constant.UserConstant.SALT;

/**
 * @author mc
 */
public class EncryptPassword {
    public static String getEncryptPassword(String userPassword) {
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }
}
