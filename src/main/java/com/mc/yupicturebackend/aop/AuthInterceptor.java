package com.mc.yupicturebackend.aop;

import com.mc.yupicturebackend.annotation.AuthCheck;
import com.mc.yupicturebackend.exception.BusinessException;
import com.mc.yupicturebackend.exception.ErrorCode;
import com.mc.yupicturebackend.model.entity.User;
import com.mc.yupicturebackend.model.enums.UserRoleEnum;
import com.mc.yupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * @author mc
 */
@Aspect
@Component
@Slf4j
public class AuthInterceptor {
    @Resource
    private UserService userService;

    /**
     * 执行拦截
     *
     * @param joinPoint 切入点
     * @param authCheck 权限校验注解
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        // 1.获取注解参数
        String mustRole = authCheck.mustRole();
        if (mustRole == null || mustRole.isEmpty()) {
            throw new IllegalArgumentException("mustRole cannot be null or empty");
        }
        // 2.解析所需角色
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
        if (mustRoleEnum == null) {
            // 不需要权限，直接放行
            return joinPoint.proceed();
        }
        // 3.获取当前请求和登录用户
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        User loginUser = userService.getLoginUser(request);
        // 4.解析用户角色和所需角色
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        // 5.权限校验
        if (!hasRequiredRole(userRoleEnum, mustRoleEnum)) {
            log.warn("User [{}] with role [{}] failed to access method requiring role [{}]",
                    loginUser.getUserName(), userRoleEnum, mustRoleEnum);
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "用户权限不足");
        }
        // 6.权限校验通过，执行目标方法
        return joinPoint.proceed();
    }
    private boolean hasRequiredRole(UserRoleEnum userRoleEnum, UserRoleEnum mustRoleEnum) {
        // 检查是否匹配管理员权限
        return UserRoleEnum.ADMIN.equals(mustRoleEnum)
                && UserRoleEnum.ADMIN.equals(userRoleEnum);
    }
}
