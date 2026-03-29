package com.ow0b.c7b9.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// 用于标注需要登录但又不需要使用User信息的api类
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginRequired
{
}