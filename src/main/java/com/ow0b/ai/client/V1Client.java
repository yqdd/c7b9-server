package com.ow0b.ai.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ow0b.ai.client.abstracted.*;
import com.ow0b.ai.client.message.Message;
import com.ow0b.ai.client.message.Role;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter @Setter
public class V1Client extends AiClient
{
    private String message;
    public V1Client(String url, String key, String model)
    {
        super(url, key, model);
    }
    public V1Client(String res, String prefix)
    {
        super(res, prefix);
    }

    @Override
    public void registry(Object obj)
    {
        throw new UnsupportedOperationException("V1Client不支持 Function Call");
    }
    @Override
    public JsonObject body(ChatConfig config)
    {
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("prompt", message);
        if(config.maxTokens > 0) body.addProperty("max_tokens", config.maxTokens);
        if(config.stream) body.addProperty("stream", true);
        return body;
    }
    @Override
    public void call(DeltaConsumer listener, ChatConfig config)
    {
        Message message = new Message(Role.ASSISTANT);
        try(ApiConnection conn = ApiConnection.create(this, body(config).toString()))
        {
            if(config.stream)
            {
                String line;
                message.role = Role.ASSISTANT;
                while((line = conn.data()) != null)
                {
                    JsonElement ele = JsonParser.parseString(line);     //解析 ["DONE"]
                    if(ele.isJsonObject())
                    {
                        JsonObject response = ele.getAsJsonObject();
                        ApiConnection.error(response);
                        if(!response.getAsJsonArray("choices").isEmpty())
                        {
                            String delta = response.getAsJsonArray("choices").get(0).getAsJsonObject().get("text").getAsString();
                            listener.accept(new DeltaContent(delta, null));
                            message.content.append(delta);
                        }
                    }
                }
            }
            else
            {
                System.out.println(123);
                JsonObject response = JsonParser.parseString(conn.body()).getAsJsonObject();
                ApiConnection.error(response);
                if(!response.getAsJsonArray("choices").isEmpty())
                {
                    log.info("{}", response);
                    String content = response.getAsJsonArray("choices").get(0).getAsJsonObject().get("text").getAsString();
                    listener.accept(new DeltaContent(content, null));
                }
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}