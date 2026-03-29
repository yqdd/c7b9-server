package com.ow0b.ai.client.message;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ow0b.ai.client.abstracted.AiClient;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class Messages
{
    private final static Gson gson = new Gson();
    private final AiClient client;
    private final LinkedList<Message> list = new LinkedList<>();
    public final Map<String, String> system = new LinkedHashMap<>();
    public List<Message> getMessageList()
    {
        return list.stream().filter(m -> m.role != Role.SYSTEM).toList();
    }
    public void add(Role role, String content)
    {
        add(new Message(role, content));
    }
    public void add(Message message)
    {
        list.add(message);
    }
    public void addAll(Collection<? extends Message> messages)
    {
        list.addAll(messages);
    }
    public void addAll(Messages messages)
    {
        list.addAll(messages.list);
    }
    public Message getLast()
    {
        return list.getLast();
    }
    public void removeAll(Role... role)
    {
        list.removeIf(msg -> Arrays.stream(role).anyMatch(r -> r == msg.role));
    }
    public void clear()
    {
        list.clear();
    }
    public void forEach(Consumer<Message> consumer)
    {
        list.forEach(consumer);
    }
    public void system(String prompt)
    {
        system.put(null, prompt);
    }
    public void system(Class<?> clazz)
    {
        if(clazz.isAnnotationPresent(SystemPrompts.class) || clazz.isAnnotationPresent(SystemPrompt.class))
        {
            SystemPrompt[] prompts = clazz.getDeclaredAnnotationsByType(SystemPrompt.class);
            for(SystemPrompt prompt : prompts)
                system.put(prompt.key(), prompt.prompt());
        }
        if(clazz.isAnnotationPresent(SimpleSystemPrompt.class))
        {
            system.put(null, clazz.getAnnotation(SimpleSystemPrompt.class).value());
        }
    }
    public void system(Object object)
    {
        Class<?> clazz = object.getClass();
        do
        {
            if(clazz.isAnnotationPresent(SystemPrompts.class) || clazz.isAnnotationPresent(SimpleSystemPrompt.class))
                system(clazz);

            clazz = clazz.getSuperclass();
        }
        while(object.getClass().getSuperclass() != Object.class);
    }

    public JsonArray requestJson()
    {
        StringBuilder systemPrompt = new StringBuilder();
        if(system.containsKey(null)) systemPrompt.append(system.get(null)).append("\n\n");
        system.forEach((k, v) ->
        {
            if(k != null)
            {
                if(!client.keys.isEmpty() && !client.keys.contains(k)) throw new RuntimeException("未定义的system prompt key：" + k);
                systemPrompt.append("# ").append(k).append("\n").append(v).append("\n\n");
            }
        });

        return JsonParser.parseString(gson.toJson(
                Stream.concat(Stream.of(new Message(Role.SYSTEM, systemPrompt.toString())), list.stream())
                        .map(msg ->
                        {
                            JsonObject obj = new JsonObject();
                            obj.addProperty("role", msg.role.value);
                            obj.addProperty("content", msg.content.toString());
                            if(!msg.tool_calls.isEmpty()) obj.add("tool_calls", JsonParser.parseString(gson.toJson(msg.tool_calls)));
                            if(msg.role == Role.TOOL) obj.addProperty("tool_call_id", msg.toolCallId);
                            return obj;
                        }).toList())).getAsJsonArray();
    }
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        list.forEach(m -> builder.append(m.role)
                .append("> ")
                .append(m.content)
                .append(m.tool_calls.isEmpty() ? "" : m.tool_calls)
                .append("\n"));
        return builder.toString();
    }
}
