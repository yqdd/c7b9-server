package com.ow0b.ai.client.message;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.ow0b.ai.client.abstracted.DeltaContent;
import com.ow0b.ai.client.function.ToolCalls;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Objects;

@Slf4j
@ToString
public class Message
{
    public Role role;
    public final StringBuilder reasoning = new StringBuilder();
    public final StringBuilder content = new StringBuilder();
    public final ToolCalls tool_calls = new ToolCalls();
    @ToString.Exclude
    public String toolCallId;

    public Message(Role role)
    {
        this.role = role;
    }
    public Message(Role role, String content)
    {
        this(role);
        this.content.append(content);
    }
    /// 这里的role主要是避免使用到老版本 String role, String content 构造器
    public Message(Role role, String toolCallId, String content)
    {
        this(Role.TOOL, content);
        if(role != Role.TOOL) throw new RuntimeException("定义toolCallId Role必须为TOOL");
        this.toolCallId = toolCallId;
    }
    public Message(Role role, ToolCalls.ToolCall toolCall)
    {
        this(role, "");
        tool_calls.add(toolCall);
    }

    private static final Gson gson = new Gson();
    private String currentAppendToolCallId;
    public DeltaContent append(JsonObject json)
    {
        DeltaContent delta = new DeltaContent();
        if(json.get("role") != null)
            role = Role.valueOf(json.get("role").getAsString().toUpperCase());
        if(json.get("content") != null && !json.get("content").isJsonNull())
            content.append(Objects.requireNonNullElse(delta.content = json.get("content").getAsString(), ""));
        if(json.get("reasoning_content") != null && !json.get("reasoning_content").isJsonNull())
            reasoning.append(Objects.requireNonNullElse(delta.reasoning = json.get("reasoning_content").getAsString(), ""));
        if(json.get("tool_calls") != null)
        {
            ToolCalls calls = gson.fromJson(json.get("tool_calls"), ToolCalls.class);
            calls.forEach(t ->
            {
                if(t.id != null && !t.id.isEmpty())
                    tool_calls.add(new ToolCalls.ToolCall(currentAppendToolCallId = t.id, new ToolCalls.Function(t.function.name, "")));
                else
                    tool_calls.stream()
                            .filter(c -> c.id.equals(currentAppendToolCallId))
                            .findFirst()
                            .orElseThrow().function.arguments.append(t.function.arguments);
            });
        }
        return delta;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class FunctionCall
    {
        public String name;
        public final StringBuilder arguments = new StringBuilder();
    }
}
