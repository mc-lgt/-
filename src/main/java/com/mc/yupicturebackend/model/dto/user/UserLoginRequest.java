package com.mc.yupicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @author mc
 */
@Data
public class UserLoginRequest implements Serializable {
    private static final long serialVersionUID = 1167731687044133057L;

    private String userAccount;
    private String userPassword;
}
