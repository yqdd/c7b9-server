package com.ow0b.ai.client.abstracted;

public interface DeltaConsumer
{
    /// 这里exception传出lambda表达式中抛出的异常
    void accept(DeltaContent delta) throws Exception;
}
