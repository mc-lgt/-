package com.mc.yupicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mc.yupicturebackend.constant.UserConstant;
import com.mc.yupicturebackend.exception.BusinessException;
import com.mc.yupicturebackend.exception.ErrorCode;
import com.mc.yupicturebackend.exception.ThrowUtils;
import com.mc.yupicturebackend.model.dto.user.UserQueryRequest;
import com.mc.yupicturebackend.model.entity.User;
import com.mc.yupicturebackend.model.enums.UserRoleEnum;
import com.mc.yupicturebackend.model.vo.LoginUserVO;
import com.mc.yupicturebackend.model.vo.UserVO;
import com.mc.yupicturebackend.service.UserService;
import com.mc.yupicturebackend.mapper.UserMapper;
import com.mc.yupicturebackend.utils.EncryptPassword;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
* @author mc
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2024-12-22 13:30:28
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    /**
     * 用户注册
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1.参数校验
        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount, userPassword, checkPassword),
                new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空!!!"));
        ThrowUtils.throwIf(userAccount.length() < 4,
                new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号长度过短!!!"));
        ThrowUtils.throwIf(userAccount.length() > 24,
                new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号长度过长!!!"));
        ThrowUtils.throwIf(userPassword.length() < 8 || checkPassword.length() < 8,
                new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码长度过短!!!"));
        ThrowUtils.throwIf(userPassword.length() > 24 || checkPassword.length() > 24,
                new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码长度过长!!!"));
        ThrowUtils.throwIf(!userPassword.equals(checkPassword),
                new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致!!!"));
        // 2.验证用户账号是否与数据库中的账号出现重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        Long count = this.baseMapper.selectCount(queryWrapper);
        ThrowUtils.throwIf(count > 0, new BusinessException(ErrorCode.PARAMS_ERROR, "账号已存在!!!"));
        // 3.密码加密
        String encryptPassword = EncryptPassword.getEncryptPassword(userPassword);
        // 4.添加到数据库
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("无名");
        user.setUserRole(UserRoleEnum.USER.getValue());

        boolean saved = save(user);
        ThrowUtils.throwIf(!saved, ErrorCode.SYSTEM_ERROR, "保存数据库失败!!!");
        return user.getId();
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1.参数校验
        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount, userPassword),
                new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空!!!"));
        ThrowUtils.throwIf(userAccount.length() < 4,
                new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号长度过短!!!"));
        ThrowUtils.throwIf(userAccount.length() > 24,
                new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号长度过长!!!"));
        ThrowUtils.throwIf(userPassword.length() < 8,
                new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码长度过短!!!"));
        ThrowUtils.throwIf(userPassword.length() > 24,
                new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码长度过长!!!"));
        // 2.查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        User user = this.baseMapper.selectOne(queryWrapper);
        ThrowUtils.throwIf(user == null,
                new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在!!!"));
        // 3.校验密码
        String encryptPassword = EncryptPassword.getEncryptPassword(userPassword);
        ThrowUtils.throwIf(!encryptPassword.equals(user.getUserPassword()),
                new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误!!!"));
        // 4.记录用户登录态
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
        return getLoginUserVO(user);
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 1.从session中获取登录用户
        User currentUser = (User) request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        // 2.判断登录用户是否为空
        ThrowUtils.throwIf(currentUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户没有登录");
        // 3.防止数据库中当前用户信息修改，从数据库中查询最新数据
        currentUser = this.getById(currentUser.getId());
        // 2.判断登录用户是否被逻辑删除
        ThrowUtils.throwIf(currentUser.getIsDelete() == 1, ErrorCode.FORBIDDEN_ERROR, "用户被禁止访问");
        return currentUser;
    }

    /**
     * 获取脱敏类的用户信息
     *
     * @param user 用户
     * @return 脱敏后的用户信息
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 1.从session中获取登录用户
        User currentUser = (User) request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        // 2.判断登录用户是否为空
        ThrowUtils.throwIf(currentUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户没有登录");
        // 3.移除用户登录态
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true;
    }

    /**
     * 获得脱敏后的用户信息
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }
    /**
     * 获取脱敏后的用户列表
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), "ascend".equals(sortOrder), sortField);
        return queryWrapper;
    }

}




