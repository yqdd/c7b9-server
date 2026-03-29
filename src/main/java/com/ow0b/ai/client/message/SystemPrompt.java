package com.ow0b.ai.client.message;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(SystemPrompts.class)
public @interface SystemPrompt
{
    String key();
    String prompt();
}
