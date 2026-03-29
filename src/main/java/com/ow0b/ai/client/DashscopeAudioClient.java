package com.ow0b.ai.client;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationOutput;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.google.gson.JsonObject;
import com.ow0b.ai.client.abstracted.AiClient;
import com.ow0b.ai.client.abstracted.ChatConfig;
import com.ow0b.ai.client.abstracted.DeltaConsumer;
import com.ow0b.ai.client.abstracted.DeltaContent;
import io.reactivex.Flowable;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;
import java.util.Map;

// 注意：qwen音频模型不能用m4a格式的，会报audio is empty（貌似只能用mp3和wav）
@Slf4j
@Getter @Setter
public class DashscopeAudioClient extends AiClient
{
    private String message;
    private String uri;
    public void setUriFile(File file)
    {
        // 阿里云的Dashscope上传文件兼容Win和Linux路径
        if(System.getProperty("os.name").toLowerCase().startsWith("windows"))
            uri = new File(file.getAbsolutePath()).toURI().toString().replace("file:/", "file:///");
        else
            uri = file.getAbsolutePath();
    }

    public DashscopeAudioClient(String url, String key, String model)
    {
        super(url, key, model);
    }
    public DashscopeAudioClient(String res, String prefix)
    {
        super(res, prefix);
    }

    @Override
    public void registry(Object obj)
    {
        throw new UnsupportedOperationException("V1Client不支持 Function Call");
    }
    public MultiModalConversationParam param(ChatConfig config)
    {
        return MultiModalConversationParam.builder()
                .apiKey(getApiKey())
                .incrementalOutput(config.stream)
                .modalities(List.of("text"))
                .maxTokens(config.maxTokens > 0 ? config.maxTokens : null)
                .model(model)
                .messages(List.of(message(message, uri)))
                .build();
    }
    @Override
    public JsonObject body(ChatConfig config)
    {
        return param(config).getHttpBody();
    }
    @Override
    public void call(DeltaConsumer listener, ChatConfig config)
    {
        log.info("{}", param(config).getHttpBody());
        try
        {
            MultiModalConversation conv = new MultiModalConversation();
            if(config.stream)
            {
                Flowable<MultiModalConversationResult> result = conv.streamCall(param(config));
                result.blockingForEach(message ->
                {
                    List<Map<String, Object>> data = message.getOutput().getChoices().get(0).getMessage().getContent();
                    if(!data.isEmpty())
                    {
                        String content = (String) data.get(0).get("text");
                        listener.accept(new DeltaContent(content, null));
                    }
                });
            }
            else
            {
                MultiModalConversationResult result = conv.call(param(config));
                List<MultiModalConversationOutput.Choice> choices = result.getOutput().getChoices();
                String content = (String) choices.get(0).getMessage().getContent().get(0).get("text");
                listener.accept(new DeltaContent(content, null));
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
    private MultiModalMessage message(String userContent, String audioUri)
    {
        return MultiModalMessage.builder().role("user")
                .content(List.of(Map.of("text", userContent), Map.of("audio", audioUri)))
                .build();
    }
}
