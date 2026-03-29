package com.ow0b.c7b9.controller;

import com.ow0b.c7b9.service.database.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

public interface LoginController
{
    /**
     * 登录用户
     * @param username 用户名
     * @param password 用户密码（由客户端加密）
     */
    ResponseEntity<Map<String, String>> login(String username, String password);

    /**
     * 注册用户
     * @param username 用户名
     * @param password 用户密码（由客户端加密）
     */
    ResponseEntity<Map<String, String>> registry(String username, String password);
}
