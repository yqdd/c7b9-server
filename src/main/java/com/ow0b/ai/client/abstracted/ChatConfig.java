package com.ow0b.ai.client.abstracted;

import lombok.Builder;

@Builder
public class ChatConfig
{
    @Builder.Default
    public boolean stream = false;
    @Builder.Default
    public boolean functionCall = true;
    @Builder.Default
    public boolean thinking = false;
    @Builder.Default
    public int maxToolCallAttempt = 10;
    @Builder.Default
    public int maxTokens = -1;
}
