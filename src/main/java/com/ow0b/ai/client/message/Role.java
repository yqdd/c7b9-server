package com.ow0b.ai.client.message;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Role
{
    USER("user"),
    ASSISTANT("assistant"),
    SYSTEM("system"),
    TOOL("tool");

    public final String value;
}