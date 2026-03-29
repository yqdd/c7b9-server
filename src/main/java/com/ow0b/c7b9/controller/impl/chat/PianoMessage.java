package com.ow0b.c7b9.controller.impl.chat;

import com.ow0b.ai.client.message.Message;
import com.ow0b.ai.client.message.Role;

import java.util.LinkedList;
import java.util.List;

public class PianoMessage extends Message
{
    public final List<Integer> audios = new LinkedList<>();

    public PianoMessage(Role role, String content)
    {
        super(role, content);
    }
    public PianoMessage(Role role, String content, String reasoning)
    {
        super(role, content);
        this.reasoning.append(reasoning);
    }
}