package com.mc.yupicturebackend.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 删除请求
 *
 * @author mc
 */
@Data
public class DeleteRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    private static final long serialVersionUID = 1L;
}