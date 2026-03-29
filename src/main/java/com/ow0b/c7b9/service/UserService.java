package com.ow0b.c7b9.service;

import com.ow0b.c7b9.service.database.json.Practice;
import com.ow0b.c7b9.service.database.model.User;
import com.ow0b.c7b9.service.exception.AuthorizationException;
import com.ow0b.c7b9.service.exception.TooManyLoginAttemptException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Timestamp;

public interface UserService
{
    /// 根据uid获取User信息
    User get(int uid);
    /// 根据permit获取User信息
    User get(@NotNull Authorization permit) throws AuthorizationException;
    /// 判断用户名是否存在，如果不存在则注册用户
    /// @param password 应该直接保存原文，由客户端来加密
    boolean registry(@NotNull String username, @NotNull String password);
    /// 登录用户，初始化permit和random
    /// @return 返回permit和random
    Authorization login(@NotNull String username, @NotNull String password) throws AuthorizationException;
    /// 登出用户
    void logout(int uid);

    record Authorization(@NotNull String permit, @Nullable String random) {}
    /// 验证permit和random并返回一个新的
    Authorization newPermit(int uid, @NotNull Authorization oldPermit) throws AuthorizationException;
}
