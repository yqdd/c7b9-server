package com.ow0b.c7b9.service.exception;

public class TooManyLoginAttemptException extends Exception
{
    public TooManyLoginAttemptException(String message)
    {
        super(message);
    }
}
