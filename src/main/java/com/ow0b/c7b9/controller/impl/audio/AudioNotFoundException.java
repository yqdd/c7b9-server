package com.ow0b.c7b9.controller.impl.audio;

public class AudioNotFoundException extends RuntimeException
{
    public AudioNotFoundException(int rid)
    {
        super(String.format("不存在音频资源：%d", rid));
    }
}
