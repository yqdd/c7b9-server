package com.ow0b.c7b9.service.impl;

import com.google.gson.Gson;
import com.ow0b.ai.client.ChatClient;
import com.ow0b.ai.client.DashscopeAudioClient;
import com.ow0b.ai.client.message.Message;
import com.ow0b.ai.client.message.Role;
import com.ow0b.c7b9.ChatWriter;
import com.ow0b.c7b9.controller.impl.chat.ContextAccessDeniedException;
import com.ow0b.c7b9.service.AudioService;
import com.ow0b.c7b9.service.ChatService;
import com.ow0b.c7b9.service.UserService;
import com.ow0b.c7b9.service.converter.ConverterService;
import com.ow0b.c7b9.service.database.json.ContextData;
import com.ow0b.c7b9.service.database.json.Conversations;
import com.ow0b.c7b9.service.database.json.Practice;
import com.ow0b.c7b9.service.database.mapper.AudioMapper;
import com.ow0b.c7b9.service.database.mapper.ContextMapper;
import com.ow0b.c7b9.service.database.model.Audio;
import com.ow0b.c7b9.service.database.model.Context;
import com.ow0b.midi.AnalyzeResult;
import com.ow0b.midi.Midi;
import com.ow0b.midi.MidiImpl;
import com.ow0b.midi.Note;
import jakarta.annotation.PostConstruct;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import retrofit2.Retrofit;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Slf4j
@Service
public class ChatServiceImpl implements ChatService, TempDir
{
    @Setter(onMethod_ = @Autowired)
    private ApplicationContext applicationContext;

    @Setter(onMethod_ = @Autowired)
    private Gson gson;
    @Setter(onMethod_ = @Autowired)
    private UserService userService;
    @Setter(onMethod_ = @Autowired)
    private AudioService audioService;
    @Setter(onMethod_ = @Autowired)
    private ContextMapper contextMapper;
    @Setter(onMethod_ = @Autowired)
    private AudioMapper audioMapper;
    @Setter(onMethod_ = @Autowired)
    private Retrofit retrofit;

    private ConverterService converterService;
    @PostConstruct
    public void init()
    {
        converterService = retrofit.create(ConverterService.class);
    }

    @Override
    public int newConversationContext(int uid, @Nullable String title, @Nullable ContextData data)
    {
        Context context = new Context(uid);
        contextMapper.insert(context);
        int sid = context.getSid();
        contextMapper.updateConversation(uid, sid);
        if(data != null) contextMapper.setContextData(sid, gson.toJson(data));
        return sid;
    }
    @Override
    public Conversations getConversations(int uid)
    {
        return gson.fromJson(contextMapper.getConversations(uid), Conversations.class);
    }
    @Override
    public Context getContext(int uid, int sid) throws ContextAccessDeniedException
    {
        Context context = contextMapper.getContext(sid);
        if(context.getUid() != uid) throw new ContextAccessDeniedException();
        else return context;
    }
    @Override
    public void deleteContext(int uid, int sid) throws ContextAccessDeniedException
    {
        Context context = contextMapper.getContext(sid);
        if(context != null)
        {
            if(context.getUid() != uid) throw new ContextAccessDeniedException();
            //删除音频数据
            ContextData data = gson.fromJson(context.getData(), ContextData.class);
            List<Integer> audios = new ArrayList<>();
            data.forEach(m -> audios.addAll(m.audios));
            audios.forEach(aid -> audioService.delete(uid, aid));
            //删除对话数据
            contextMapper.deleteContext(sid);
        }
        //删除标题
        Conversations conv = gson.fromJson(contextMapper.getConversations(uid), Conversations.class);
        conv.remove(String.valueOf(sid));
        contextMapper.setConversation(uid, gson.toJson(conv));
    }

    @Override
    public ContextData getContextData(int uid, int sid) throws ContextAccessDeniedException
    {
        return gson.fromJson(getContext(uid, sid).getData(), ContextData.class);
    }
    @Override
    public void setConversations(int uid, Conversations conv)
    {
        contextMapper.setConversation(uid, gson.toJson(conv));
    }

    @Override
    public void setContextData(int uid, int sid, @Nullable ContextData data) throws ContextAccessDeniedException
    {
        if(contextMapper.getContext(sid).getUid() != uid) throw new ContextAccessDeniedException();
        contextMapper.setContextData(sid, gson.toJson(data));
    }
    @Override
    public void titleConversations(int uid)
    {
        Conversations conversations = getConversations(uid);
        for(Map.Entry<String, String> entry : Set.copyOf(conversations.entrySet()))
        {
            String sid = entry.getKey(), title = entry.getValue();
            if(title.equals("新对话"))
            {
                ContextData data = getContextData(uid, Integer.parseInt(sid));
                if(!data.isEmpty())
                {
                    ChatClient chatClient = applicationContext.getBean(ChatClient.class);
                    chatClient.setModel("deepseek-v3");
                    chatClient.messages.addAll(data);
                    chatClient.messages.add(new Message(Role.USER, """
                            给上面的聊天内容起一个标题，不多于10个字，如果存在钢琴曲信息最好带上钢琴曲名（注意不要使用编号）
                            
                            不要输出除了标题以外的其他任何内容，不要加引号和书名号
                            """));
                    try { conversations.put(sid, chatClient.call()); }
                    catch (Exception e) { conversations.put(sid, "自动命名失败"); }
                }
            }
        }
        contextMapper.setConversation(uid, gson.toJson(conversations));
    }


    @Override
    public boolean isPianoAudio(int aid)
    {
        try
        {
            DashscopeAudioClient audioClient = applicationContext.getBean(DashscopeAudioClient.class);
            Audio audio = audioMapper.getByAid(aid);
            if(audio.getMp3() == null) return false;

            File mp3File = File.createTempFile("temp-", ".mp3", temp.toFile());
            saveCache(mp3File, audio.getMp3());
            audioClient.setUriFile(mp3File);
            audioClient.setMessage("该音频是否为一段钢琴演奏的音频，只能输出一个字：是或者否（不能输出其他内容）");
            return audioClient.call().contains("是");
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
    @Override
    public boolean isNeedProducing(String text)
    {
        ChatClient chatClient = applicationContext.getBean(ChatClient.class);
        chatClient.setModel("deepseek-v3");
        chatClient.messages.add(new Message(Role.USER, String.format("""
                            “%s”，该段内容是否在要求续写，只能输出一个字：是或者否（不能输出其他内容）
                            """, text)));
        return chatClient.call().equals("是");
    }

    @Override
    public Midi produce(@NotNull Midi midi)
    {
        /*
        V1Client rwkvClient = applicationContext.getBean(V1Client.class);
        rwkvClient.setMessage(audioService.encodeTokenizer(midi));
        log.info("续写前半段：{}", rwkvClient.getMessage());
        StringBuilder tokenizer = new StringBuilder();
        rwkvClient.call(d -> tokenizer.append(d.content),
                ChatConfig.builder().stream(true).maxTokens(300).build());

        log.info("续写后半段：{}", tokenizer);
        String result = rwkvClient.getMessage() + (rwkvClient.getMessage().endsWith(" ") ? "" : " ") + tokenizer;
        return new MidiImpl(midi.getName() + "的续写", audioService.decodeTokenizer(result));
         */
        try(ResponseBody body = converterService.midiLLMProduce(
                RequestBody.create(audioService.encodeTokenizer(midi), MediaType.get("text/plain"))).execute().body())
        {
            String result = Objects.requireNonNull(body).string();
            return new MidiImpl(midi.getName() + "的续写", audioService.decodeTokenizer(result));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String audioContent(int aid)
    {
        try
        {
            DashscopeAudioClient audioClient = applicationContext.getBean(DashscopeAudioClient.class);
            Audio audio = audioMapper.getByAid(aid);
            if(audio.getMp3() == null) return "该音频是一段midi内容";

            File mp3File = File.createTempFile("temp-", ".mp3", temp.toFile());
            saveCache(mp3File, audio.getMp3());
            audioClient.setUriFile(mp3File);
            audioClient.setMessage("描述音频中的信息（只需要描述信息，不要对音频内容做回复），如果是一段钢琴演奏，则还可以描述一下风格情感等");
            return audioClient.call();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String analysisContent(int uid, @NotNull Audio audio, @Nullable AnalyzeResult analysis, @NotNull ChatWriter writer)
    {
        Practice practice = audioService.getPractice(uid);
        if(analysis != null)
        {
            practice.speed.putIfAbsent(analysis.name, new LinkedList<>());
            practice.speed.get(analysis.name).add(1 / analysis.speed);
            audioService.setPractice(uid, practice);
            return String.format("""
                    + 基本信息
                        - 用户上传音频总时长：%.2fs
                        - 匹配到的曲目：%s %s
                        - %s
                        - 该首曲目用户发送演奏的次数：%d
                    + 速度
                        - %s
                        - %s
                    + 力度
                        - %s
                    + 错音
                        - %s
                    + 节奏
                        - %s
                    """,
                    audioService.mp3Time(audio.getMp3()),
                    analysis.name, similarityPrompt(analysis.similarity),
                    partPrompt(analysis),
                    practice.speed.get(analysis.name).size(),
                    speedPrompt(1 / analysis.speed),
                    speedHistoryPrompt(practice.speed.get(analysis.name)),
                    forcePrompt(analysis),
                    mistakePrompt(analysis),
                    rhythmPrompt(analysis));
        }
        else return String.format("""
                    + 基本信息
                        - 用户上传音频总时长：%.2fs
                        - 未匹配到曲目
                    """, audioService.mp3Time(audio.getMp3()));
    }
    private String similarityPrompt(float similarity)
    {
        if(similarity < 0.5f) return "（存疑，相似度：" + similarity + "）";
        else return "";
    }
    private String partPrompt(AnalyzeResult analysis)
    {
        //发送音频在示例演奏的占比
        float proportion = (analysis.endTime - analysis.startTime) / analysis.totalTime,
                average = (analysis.startTime + analysis.endTime) / 2;

        StringBuilder builder = new StringBuilder();
        if(proportion < 0.3f) builder.append("用户发送的是整首曲目的一小段");
        else if(proportion < 0.7f) builder.append("用户发送的是整首曲目的一大段");
        else builder.append("用户发送的几乎是整首曲目");
        builder.append(String.format("（占比：%.2f）", proportion));

        if(proportion < 0.7f)
        {
            builder.append("，");
            if(average > analysis.totalTime * 0.5f)
            {
                if(average > analysis.totalTime * 0.8f) builder.append("演奏的是末尾段落");
                else builder.append("演奏的是中后段落");
            }
            else
            {
                if(average > analysis.totalTime * 0.8f) builder.append("演奏的是开头段落");
                else builder.append("演奏的是中前段落");
            }
            builder.append(String.format("（整首曲目示例演奏有 %.2f 秒，用户发送的片段匹配到的位置在 %.2fs~%.2fs）", analysis.totalTime, analysis.startTime, analysis.endTime));
        }
        return builder.toString();
    }
    private String speedPrompt(float speed)
    {
        if(speed > 2f) return String.format("用户弹的太快了，是原速的 %f 倍", speed);
        else if(speed > 1.6f) return String.format("用户弹的比较快，是原速的 %f 倍", speed);
        else if(speed > 1.3f) return String.format("用户弹的稍快了一点，是原速的 %f 倍", speed);

        if(speed < 0.5f) return String.format("用户弹的太慢了，是原速的 %f 倍", speed);              //1 / 2
        else if(speed < 0.625f) return String.format("用户弹的比较慢，是原速的 %f 倍", speed);       //1 / 1.6
        else if(speed < 0.769f) return String.format("用户弹的稍慢了一点，是原速的 %f 倍", speed);    //1 / 1.3

        return "用户的速度接近原速";
    }
    private String speedHistoryPrompt(List<Float> history)
    {
        StringBuilder builder = new StringBuilder();
        if(history.size() > 5)
        {
            List<Float> nearSpeed = history.subList(history.size() - 6, history.size());
            float creasing = 0, mono = 1;       //mono表示速度变化的单调性，是否是有规律的变化
            for(int i = 1; i < nearSpeed.size(); i ++)
            {
                float delta = nearSpeed.get(i) - nearSpeed.get(i - 1);
                if(delta * creasing < 0) mono -= Math.abs(delta) * (nearSpeed.size() - 1);
                creasing += delta * (nearSpeed.size() - 1);
            }

            builder.append("用户近期5次演奏的速度");
            if(creasing > 0.2f) builder.append("越来越快");
            else if(creasing < -0.2f) builder.append("越来越慢");
            else builder.append("比较平均");

            if(mono < 0.5f) builder.append("，且用户近期5次演奏的速度波动较大");
        }
        return builder.toString();
    }
    private String forcePrompt(AnalyzeResult analysis)
    {
        StringBuilder light = new StringBuilder(), heavy = new StringBuilder();
        int lightIndex = -1, heavyIndex = -1;
        for(int i = 0; i < analysis.forces.size(); i++)
        {
            if(analysis.forces.get(i) < 0.76f)
            {
                if(lightIndex == -1) lightIndex = i;
            }
            else if(lightIndex > 0)
            {
                float start = analysis.indexesUserTimes.get(lightIndex),
                        end = analysis.indexesUserTimes.get(i);
                light.append(start).append("s-").append(end).append("s   ");
                lightIndex = -1;
            }

            if(analysis.forces.get(i) < 1.3f)
            {
                if(heavyIndex == -1) heavyIndex = i;
            }
            else if(heavyIndex > 0)
            {
                float start = analysis.indexesUserTimes.get(heavyIndex),
                        end = analysis.indexesUserTimes.get(i);
                heavy.append(start).append("s-").append(end).append("s ");
                heavyIndex = -1;
            }
        }
        if(light.isEmpty() && heavy.isEmpty()) return "用户的力度接近原曲力度，没有过轻或过重的地方";
        else
        {
            StringBuilder result = new StringBuilder("用户");
            if(!light.isEmpty()) result.append(String.format("在 %s 弹的过轻", light));
            if(!light.isEmpty() && !heavy.isEmpty()) result.append("，");
            if(!heavy.isEmpty()) result.append(String.format("在 %s 弹的过轻", heavy));
            return result.toString();
        }
    }
    private String mistakePrompt(AnalyzeResult analysis)
    {
        StringBuilder builder = new StringBuilder();
        int mistakeCount = 0, lastMistake = -1;
        for(int i = 0; i < analysis.mistakes.size(); i ++)
        {
            List<Note> groupMistakes = analysis.mistakes.get(i);
            if(groupMistakes.size() >= 6)
            {
                mistakeCount ++;
                lastMistake = i;
            }
            else
            {
                if(lastMistake >= 0)
                {
                    builder.append(analysis.indexesUserTimes.get(lastMistake))
                            .append("s-")
                            .append(analysis.indexesUserTimes.get(i))
                            .append("s    ");
                }
                lastMistake = -1;
            }
        }
        builder.append("存在错音");
        float mistakeRate = (float) mistakeCount / analysis.mistakes.size();
        if(mistakeRate < 0.3f && builder.isEmpty())
            return "用户的演奏几乎没有错音";
        else if(mistakeRate < 0.3f)
            return "用户的演奏错音非常少，在位于用户音频的" + builder;
        else if(mistakeRate < 0.6f)
            return "用户的演奏有一些错音，在位于用户音频的" + builder;
        else
            return "用户的演奏有较多错音，在位于用户音频的" + builder;
    }
    private String rhythmPrompt(AnalyzeResult analysis)
    {
        StringBuilder fast = new StringBuilder(), slow = new StringBuilder();
        int fastIndex = -1, slowIndex = -1;
        for(int i = 0; i < analysis.rhythm.size(); i++)
        {
            if (analysis.rhythm.get(i) < 0.5f)     // 1 / 1.3f
            {
                if (fastIndex == -1) fastIndex = i;
            }
            else if (fastIndex > 0)
            {
                float start = analysis.indexesUserTimes.get(fastIndex),
                        end = analysis.indexesUserTimes.get(i);
                fast.append(start).append("s-").append(end).append("s    ");
                fastIndex = -1;
            }

            if (analysis.rhythm.get(i) > 2f)
            {
                if (slowIndex == -1) slowIndex = i;
            }
            else if (slowIndex > 0)
            {
                float start = analysis.indexesUserTimes.get(slowIndex),
                        end = analysis.indexesUserTimes.get(i);
                slow.append(start).append("s-").append(end).append("s    ");
                slowIndex = -1;
            }
        }
        if(fast.isEmpty() && slow.isEmpty()) return "用户的节奏非常稳定，没有过快或过慢的地方";
        else
        {
            StringBuilder result = new StringBuilder("用户");
            if(!fast.isEmpty()) result.append(String.format("在位于用户音频的 %s 弹的过快", fast));
            if(!fast.isEmpty() && !slow.isEmpty()) result.append("，");
            if(!slow.isEmpty()) result.append(String.format("在位于用户音频的 %s 弹的过慢", slow));
            return result.toString();
        }
    }
}
