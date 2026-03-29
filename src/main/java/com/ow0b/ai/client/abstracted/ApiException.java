package com.ow0b.ai.client.abstracted;

public class ApiException extends RuntimeException
{
    public String type;
    public String code;
    public ApiException(String message)
    {
        super(message);
    }
}
