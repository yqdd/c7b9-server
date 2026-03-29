package com.ow0b.c7b9.service.impl;

import com.google.gson.Gson;
import com.ow0b.c7b9.service.Encryption;
import com.ow0b.c7b9.service.UserService;
import com.ow0b.c7b9.service.database.json.Practice;
import com.ow0b.c7b9.service.database.mapper.UserMapper;
import com.ow0b.c7b9.service.database.model.User;
import com.ow0b.c7b9.service.exception.AuthorizationException;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Random;

@Slf4j
@Service
public class UserServiceImpl implements UserService
{
    @Setter(onMethod_ = @Autowired)
    private UserMapper userMapper;

    @Override
    public User get(int uid)
    {
        return userMapper.getByUid(uid);
    }
    @Override
    public boolean registry(@NotNull String username, @NotNull String password)
    {
        User user = userMapper.getByUsername(username);
        if(user == null)
        {
            userMapper.insert(username, password);
            return true;
        }
        else return false;
    }
    @Override
    public Authorization login(@NotNull String username, @NotNull String password) throws AuthorizationException
    {
        User user = userMapper.getByUsername(username);
        if(user.getTimelimit().toInstant().compareTo(Instant.now()) > 0)
        {
            //一分钟后再尝试提示
            long time = Instant.now().until(user.getTimelimit().toInstant(), ChronoUnit.SECONDS);
            throw new AuthorizationException("请 " + time + " 秒后再试");
        }
        //尝试次数足够，验证密码
        if(user.getPassword().equals(password))
        {
            Authorization auth = newAuthorization();
            userMapper.setPermit(user.getUid(), auth.permit(), auth.random());
            userMapper.setAttempt(user.getUid(), 0);
            return auth;
        }
        else
        {
            //错误超过5次暂停尝试
            if(user.getAttempt() >= 5)
            {
                userMapper.setLimit(user.getUid(), Timestamp.from(Instant.now().plus(1, ChronoUnit.MINUTES)));
                userMapper.setAttempt(user.getUid(), 0);
            }
            else userMapper.setAttempt(user.getUid(), user.getAttempt() + 1);
            throw new AuthorizationException("密码错误");
        }
    }
    @Override
    public void logout(int uid)
    {
        userMapper.setPermit(uid, "", "");
    }

    @Override
    @Nullable
    public Authorization newPermit(int uid, @NotNull Authorization oldPermit) throws AuthorizationException
    {
        User user = userMapper.getByUid(uid);
        if((user.getPermit() != null && user.getPermit().equals(oldPermit.permit())) &&
                (user.getRandom() == null || user.getRandom().equals(oldPermit.random())))
        {
            Authorization auth = newAuthorization();
            userMapper.setPermit(user.getUid(), auth.permit(), auth.random());
            userMapper.setAttempt(user.getUid(), 0);
            return auth;
        }
        throw new AuthorizationException("用户信息失效");
    }
    private Authorization newAuthorization()
    {
        String permit = Encryption.encryptAES(random(), "a[v}\\zi/9-yi%1^)"), random = random();
        return new Authorization(permit, random);
    }
    private String random()
    {
        return String.valueOf(new Random().nextInt(Integer.MAX_VALUE));
    }

    @Override
    public User get(@NotNull Authorization auth) throws AuthorizationException
    {
        User user = userMapper.getByPermit(auth.permit());
        if(user == null || (user.getRandom() != null && !user.getRandom().equals(auth.random()))) throw new AuthorizationException("用户授权信息过期");
        else return user;
    }
}
