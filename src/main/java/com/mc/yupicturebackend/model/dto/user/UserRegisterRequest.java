package com.mc.yupicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @author mc
 */
@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = -3809413973962646329L;
    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 确认密码
     */
    private String checkPassword;
}

