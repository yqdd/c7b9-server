package com.ow0b.c7b9.controller.impl.user;

public class AuthNotFoundException extends RuntimeException
{
    public AuthNotFoundException(String message)
    {
        super(message);
    }
}
