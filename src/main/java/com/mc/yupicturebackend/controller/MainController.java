package com.mc.yupicturebackend.controller;

import com.mc.yupicturebackend.common.BaseResponse;
import com.mc.yupicturebackend.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author mc
 */
@RestController
@RequestMapping("/")
@Slf4j
public class MainController {
    @GetMapping("/health")
    public BaseResponse<String> health(){
        log.info("6666");
        return ResultUtils.success("OK");
    }
}
