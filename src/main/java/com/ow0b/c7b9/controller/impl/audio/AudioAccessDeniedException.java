package com.ow0b.c7b9.controller.impl.audio;

public class AudioAccessDeniedException extends RuntimeException
{
    public AudioAccessDeniedException(int aid)
    {
        super("无法访问音频" + aid);
    }
    public AudioAccessDeniedException(String message)
    {
        super(message);
    }
}
