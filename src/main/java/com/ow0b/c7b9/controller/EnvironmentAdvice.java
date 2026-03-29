package com.ow0b.c7b9.controller;

import com.ow0b.c7b9.service.database.model.User;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

public interface EnvironmentAdvice
{
    /// 通过当前http头信息获取请求客户端登录的用户
    User user(@RequestHeader Map<String, String> headers);
}
