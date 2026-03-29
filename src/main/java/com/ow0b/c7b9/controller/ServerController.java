package com.ow0b.c7b9.controller;

import org.springframework.http.ResponseEntity;

public interface ServerController
{
    /// 状态码返回服务器是否可用
    ResponseEntity<?> alive();
}
