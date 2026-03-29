package com.ow0b.ai.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.ow0b.ai.client.abstracted.AiClient;
import com.ow0b.ai.client.abstracted.ApiConnection;
import com.ow0b.ai.client.abstracted.ChatConfig;
import com.ow0b.ai.client.abstracted.DeltaConsumer;
import com.ow0b.ai.client.function.MethodResult;
import com.ow0b.ai.client.function.ToolCalls;
import com.ow0b.ai.client.function.ToolMethods;
import com.ow0b.ai.client.message.Message;
import com.ow0b.ai.client.message.Messages;
import com.ow0b.ai.client.message.Role;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class ChatClient extends AiClient
{
    public final Messages messages = new Messages(this);
    public final ToolMethods functions = new ToolMethods(this);

    public ChatClient(String url, String key, String model)
    {
        super(url, key, model);
    }
    public ChatClient(String res, String prefix)
    {
        super(res, prefix);
    }
    @Override
    public void registry(Object obj)
    {
        functions.add(obj);
        messages.system(obj);
    }
    @Override
    public JsonObject body(ChatConfig config)
    {
        return body(config, messages);
    }
    public JsonObject body(ChatConfig config, Messages messages)
    {
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        //避免将子类的参数传入到请求中
        body.add("messages", messages.requestJson());
        if(config.functionCall && functions.size() > 0) body.add("tools", functions.toolsJson());
        if(config.maxTokens > 0) body.addProperty("max_tokens", config.maxTokens);
        if(config.stream) body.addProperty("stream", true);
        if(config.thinking) body.addProperty("enable_thinking", true);
        return body;
    }

    private int toolCallAttempt = 0;
    @Override
    public void call(DeltaConsumer listener)
    {
        call(listener, ChatConfig.builder().stream(true).build());
    }
    @Override
    public void call(DeltaConsumer listener, ChatConfig config)
    {
        call(listener, config, messages);
    }
    public void call(DeltaConsumer listener, ChatConfig config, Messages messages)
    {
        Message message = new Message(Role.ASSISTANT);
        System.out.println(body(config, messages).toString());
        try(ApiConnection conn = ApiConnection.create(this, body(config, messages).toString()))
        {
            if(config.stream)
            {
                String line;
                while((line = conn.data()) != null)
                {
                    JsonElement element = JsonParser.parseString(line);
                    if(element.isJsonObject())
                    {
                        JsonObject response = element.getAsJsonObject();
                        ApiConnection.error(response);
                        if(!response.getAsJsonArray("choices").isEmpty())
                        {
                            JsonObject delta = response.getAsJsonArray("choices").get(0).getAsJsonObject().get("delta").getAsJsonObject();
                            listener.accept(message.append(delta));
                        }
                    }
                }
            }
            else
            {
                JsonObject response = JsonParser.parseString(conn.body()).getAsJsonObject();
                ApiConnection.error(response);
                if(!response.getAsJsonArray("choices").isEmpty())
                {
                    JsonObject content = response.getAsJsonArray("choices").get(0).getAsJsonObject().get("message").getAsJsonObject();
                    listener.accept(message.append(content));
                }
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        messages.add(message);
        if(!message.tool_calls.isEmpty() && toolCallAttempt++ < config.maxToolCallAttempt)
        {
            boolean nextCall = true;
            for(ToolCalls.ToolCall toolCall : message.tool_calls)
            {
                try
                {
                    MethodResult result = functions.invoke(toolCall.function.name, toolCall.function.arguments.toString());
                    if(result.except) nextCall = false;
                    messages.add(new Message(Role.TOOL, toolCall.id, result.content));
                }
                catch (Exception e)
                {
                    messages.add(new Message(Role.TOOL, toolCall.id, "调用失败：" + e.getMessage()));
                    log.error("", e);
                }
            }
            if(nextCall) call(listener, config);
        }
        toolCallAttempt = 0;
    }
}