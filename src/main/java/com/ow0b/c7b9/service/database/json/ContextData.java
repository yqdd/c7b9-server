package com.ow0b.c7b9.service.database.json;

import com.ow0b.ai.client.message.Message;
import com.ow0b.c7b9.controller.impl.chat.PianoMessage;

import java.util.ArrayList;
import java.util.List;

public class ContextData extends ArrayList<PianoMessage>    //这里不能用Message类，Gson转换基类会不是PianoMessage
{
    public ContextData() {}
    public ContextData(List<Message> messages)
    {
        //这里不要用匿名内部类来初始化PianoMessage，gson会对不上类型从而换成null
        addAll(messages.stream()
                .map(m ->
                {
                    if(m instanceof PianoMessage pm) return pm;
                    else
                    {
                        PianoMessage pm = new PianoMessage(m.role, m.content.toString(), m.reasoning.toString());
                        pm.tool_calls.addAll(m.tool_calls);
                        pm.toolCallId = m.toolCallId;
                        return pm;
                    }
                })
                .toList());
    }
}