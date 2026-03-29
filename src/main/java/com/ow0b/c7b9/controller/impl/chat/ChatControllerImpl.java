package com.ow0b.c7b9.controller.impl.chat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ow0b.ai.client.ChatClient;
import com.ow0b.ai.client.abstracted.ChatConfig;
import com.ow0b.ai.client.function.ToolCalls;
import com.ow0b.ai.client.message.Message;
import com.ow0b.ai.client.message.Messages;
import com.ow0b.ai.client.message.Role;
import com.ow0b.c7b9.annotation.LoginRequired;
import com.ow0b.c7b9.controller.ChatController;
import com.ow0b.c7b9.controller.impl.audio.AudioNotFoundException;
import com.ow0b.c7b9.service.AudioService;
import com.ow0b.c7b9.service.ChatService;
import com.ow0b.c7b9.service.Encryption;
import com.ow0b.c7b9.service.database.json.ContextData;
import com.ow0b.c7b9.service.database.json.Conversations;
import com.ow0b.c7b9.service.database.model.User;
import com.ow0b.c7b9.ChatWriter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.nio.file.AccessDeniedException;
import java.util.*;

@LoginRequired
@Slf4j
@RestController
public class ChatControllerImpl implements ChatController
{
    private record ChatData(User user, Thread thread, ChatWriter writer, Messages messages) {}
    private final static HashMap<Integer, ChatData> chats = new HashMap<>();

    @Setter(onMethod_ = @Autowired)
    private Gson gson;
    @Setter(onMethod_ = @Autowired)
    private ApplicationContext context;
    @Setter(onMethod_ = @Autowired)
    private ChatService chatService;
    @Setter(onMethod_ = @Autowired)
    private AudioService audioService;

    @Override
    @RequestMapping("/chat")
    public StreamingResponseBody chat(@ModelAttribute User user,
                                      @RequestParam("message") String content,
                                      @RequestParam(value = "id", required = false, defaultValue = "-1") int id,
                                      @RequestBody(required = false) List<Integer> audios)
    {
        //刷新标题
        chatService.titleConversations(user.getUid());
        int[] sid = new int[] {id == -1 ? chatService.newConversationContext(user.getUid(), content, null) : id};
        return outputStream ->
        {
            try
            {
                if(!chats.containsKey(sid[0]))
                {
                    ChatClient chatClient = context.getBean(ChatClient.class);
                    ChatWriter writer = new ChatWriter(new OutputStreamWriter(outputStream));
                    Thread chatThread = new Thread(() ->
                    {
                        try
                        {
                            PianoMessage userMessage = new PianoMessage(Role.USER, content);
                            C7b9Agent agent = new C7b9Agent(context, user, writer, userMessage);
                            if(audios != null) userMessage.audios.addAll(audios);

                            writer.sendStreamJson("context", "id", sid[0]);
                            log.info("{} {}", id, chatClient.messages);
                            chatClient.messages.clear();
                            chatClient.messages.addAll(chatService.getContextData(user.getUid(), sid[0]));
                            //log.info("{} {}", sid[0], chatClient.messages);
                            chatClient.messages.add(userMessage);
                            if(audios != null && !audios.isEmpty())
                            {
                                String toolCallId = "call_" + Encryption.encryptMD5(String.valueOf(audios.hashCode()));
                                String toolName = "识别音频";
                                Message toolCallMessage = new Message(Role.ASSISTANT, new ToolCalls.ToolCall(toolCallId, new ToolCalls.Function(toolName, "")));
                                chatClient.messages.add(toolCallMessage);
                                chatClient.messages.add(new Message(Role.TOOL, toolCallId, agent.info()));
                            }
                            chatClient.registry(agent);
                            chatClient.call(delta -> writer.sendStreamJson("message",
                                    new ChatWriter.Datum("content", delta.content),
                                    new ChatWriter.Datum("reasoning", delta.reasoning)),
                                    ChatConfig.builder().stream(true).thinking(true).build());

                            //保存数据
                            ContextData data = new ContextData(chatClient.messages.getMessageList());
                            //Conversations conv = chatService.getConversations(user.uid);
                            chatService.setContextData(user.getUid(), sid[0], data);
                            if(chats.containsKey(sid[0]))
                            {
                                chats.get(sid[0]).writer.close();
                                chats.remove(sid[0]);
                                writer.close();
                            }
                        }
                        catch (Exception e)
                        {
                            log.error("", e);
                            Thread.currentThread().interrupt();
                        }
                    });
                    chats.put(sid[0], new ChatData(user, chatThread, writer, chatClient.messages));
                    chatThread.start();
                    chatThread.join();
                }
                else
                {
                    try(ChatWriter writer = chats.get(sid[0]).writer())
                    {
                        writer.addMirror(new OutputStreamWriter(outputStream));
                        chats.get(sid[0]).thread().join();
                    }
                }
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        };
    }

    @Override
    @RequestMapping("/context")
    public Map<String, List<?>> context(@ModelAttribute User user, @RequestParam(value = "id") int sid)
    {
        ContextData data = chatService.getContextData(user.getUid(), sid);
        data.removeIf(m -> m.role == Role.SYSTEM);
        return Map.of("data", data);
    }

    @Override
    @RequestMapping("/context/delete")
    public Map<String, String> delete(@ModelAttribute User user,
                                      @RequestParam(value = "id") int sid)
    {
        chatService.deleteContext(user.getUid(), sid);
        return Map.of("info", "删除成功");
    }

    @Override
    @RequestMapping("/context/rename")
    public Map<String, String> rename(@ModelAttribute User user,
                                      @RequestParam(value = "id") int sid,
                                      @RequestParam(value = "name") String name)
    {
        Conversations conv = chatService.getConversations(user.getUid());
        String idStr = String.valueOf(sid);
        if(!conv.containsKey(idStr)) return Map.of("error", "重命名失败：不存在会话");
        conv.put(idStr, name);
        chatService.setConversations(user.getUid(), conv);
        return Map.of("info", "重命名成功");
    }

    @Override
    @RequestMapping("/chat/cancel")
    public Map<String, String> cancel(@ModelAttribute User user,
                                      @RequestParam(value = "id", required = false, defaultValue = "-1") int sid) throws Exception
    {
        if(chats.containsKey(sid))
        {
            if(chats.get(sid).user.getUid() != user.getUid()) throw new AccessDeniedException("无法访问连接");
            chats.get(sid).thread.interrupt();
            chats.get(sid).writer.close();
            chats.remove(sid);
            return Map.of("info", "已停止生成");
        }
        else return Map.of("error", "不存在对话");
    }

    @Override
    @RequestMapping("/chat/reconnect")
    public StreamingResponseBody reconnect(@ModelAttribute User user,
                                         @RequestParam(value = "id", required = false, defaultValue = "-1") int sid)
    {
        return outputStream ->
        {
            Writer writer = new OutputStreamWriter(outputStream);
            if(chats.containsKey(sid))
            {
                if(chats.get(sid).user.getUid() != user.getUid()) throw new AccessDeniedException("无法访问连接");
                try(ChatWriter chatWriter = chats.get(sid).writer())
                {
                    chatWriter.addMirror(writer);
                    chats.get(sid).thread().join();
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
            }
            else writer.write(gson.toJson(Map.of("error", "未找到会话（会话可能已结束）")));
        };
    }

    @ExceptionHandler({ContextAccessDeniedException.class})
    public Map<String, String> contextAccessDenied()
    {
        return Map.of("error", "没有访问权限");
    }
    @ExceptionHandler({AudioNotFoundException.class})
    public Map<String, String> resourceNotFound()
    {
        return Map.of("error", "不存在资源");
    }
}
