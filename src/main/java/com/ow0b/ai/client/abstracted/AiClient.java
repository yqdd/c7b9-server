package com.ow0b.ai.client.abstracted;

import com.google.gson.JsonObject;
import com.ow0b.ai.client.ChatClient;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

@Slf4j
public abstract class AiClient
{
    private final static Yaml yaml = new Yaml();
    @Getter protected final String apiUrl;
    @Getter protected final String apiKey;
    @Setter @Getter protected String model;
    public List<String> keys;

    public AiClient(String apiUrl, String apiKey, String model)
    {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.model = model;
    }
    protected AiClient(String res, String prefix)
    {
        try(InputStream stream = ChatClient.class.getClassLoader().getResourceAsStream(res))
        {
            Map<?, ?> yml = yaml.load(stream);
            for(String split : prefix.split("\\."))
                if(!split.isEmpty())
                    yml = (Map<?, ?>) yml.get(split);

            Map<?, ?> api = (Map<?, ?>) yml.get("api"),
                    config = (Map<?, ?>) yml.get("config"),
                    systemPrompt = (Map<?, ?>) yml.get("system-prompt");
            apiUrl = api.containsKey("url") ? (String) api.get("url") : "";
            apiKey = api.containsKey("key") ? (String) api.get("key") : null;
            model = api.containsKey("model") ? (String) api.get("model") : "";
            //if(config.containsKey("maxToolCallAttempt")) maxToolCallAttempt = (int) config.get("maxToolCallAttempt");
            //if(systemPrompt.containsKey("newline")) newLine = (int) systemPrompt.get("newline");
            if(systemPrompt != null && systemPrompt.containsKey("keys")) keys = ((List<?>) systemPrompt.get("keys")).stream().map(obj -> (String) obj).toList();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
    /// 注册Function Call对象
    public abstract void registry(Object obj);
    /// 返回POST请求的body
    public abstract JsonObject body(ChatConfig config);
    /// 发送请求，使用 DeltaConsumer 处理结果
    public abstract void call(DeltaConsumer listener, ChatConfig config);
    /// 发送请求（默认配置）
    public void call(DeltaConsumer listener)
    {
        call(listener, ChatConfig.builder().build());
    }
    /// 发送请求（有返回值）
    public String call()
    {
        String[] result = new String[1];
        call(delta -> result[0] = delta.content, ChatConfig.builder().stream(false).build());
        return result[0];
    }
    /// 返回模型提供商，生成请求的各种方法可以调用此方法做兼容
    public Provider getProvider()
    {
        try
        {
            String host = new URL(apiUrl).getHost();
            if("localhost".equalsIgnoreCase(host) || InetAddress.getByName(host).isLoopbackAddress())
                return Provider.LOCAL;
            switch (host)
            {
                case "api.deepseek.com" -> { return Provider.DEEPSEEK; }
                case "dashscope.aliyuncs.com" -> { return Provider.QWEN; }
            }

            System.err.println("警告：使用了未在兼容列表的模型提供商：" + host);
            return Provider.UNKNOWN;
        }
        catch (UnknownHostException | MalformedURLException e)
        {
            throw new RuntimeException(e);
        }
    }
}
