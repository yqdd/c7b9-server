package com.ow0b.ai.client.function;

import lombok.Builder;

@Builder
public class MethodResult
{
    public String content;
    /// 为true时调用方法后就不会再生成回复，一般在方法调用
    @Builder.Default
    public boolean except = false;
}
