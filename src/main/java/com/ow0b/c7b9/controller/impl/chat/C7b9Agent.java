package com.ow0b.c7b9.controller.impl.chat;

import com.google.gson.Gson;
import com.ow0b.ai.client.function.Description;
import com.ow0b.ai.client.function.Name;
import com.ow0b.ai.client.function.Required;
import com.ow0b.ai.client.message.SimpleSystemPrompt;
import com.ow0b.c7b9.ChatWriter;
import com.ow0b.c7b9.service.AudioService;
import com.ow0b.c7b9.service.ChatService;
import com.ow0b.c7b9.service.database.model.Audio;
import com.ow0b.c7b9.service.database.model.User;
import com.ow0b.midi.AnalyzeResult;
import com.ow0b.midi.Midi;
import com.ow0b.midi.MidiImpl;
import com.ow0b.midi.analyzer.Analyzer;
import com.ow0b.midi.library.Library;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

@SimpleSystemPrompt("""
        你是一个AI钢琴老师，你可以听到用户发送的音频，并根据音频内容进行回复。
        如果音频中有人在说话，这个人很可能是用户，可以直接文字回复用户在音频中说的内容。
        如果用户发送的是钢琴曲，你可以识别到演奏的曲目，你需要向用户介绍ta演奏曲目的特点、背景信息，分析用户演奏的优点，提出建议和练习的改进方案。
                目前只支持以下作曲家的曲目：巴赫，贝多芬，肖邦，德彪西，李斯特，莫扎特，拉赫玛尼诺夫，拉威尔，舒伯特，舒曼，卡普斯汀（部分），斯克里亚宾
        如果用户需要续写ta发送的音频，你需要调用“续写”工具，返回的编号使用<locateAudio/>标签生成链接让用户播放，使用<midiChart/>生成可视化midi钢琴窗
        
        你可以发送多个以下XML格式，在手机端会将其渲染为用户演奏的音频数据图表
        格式必须严格遵守（比如id属性值必须用引号括起来），如果错误了手机端会渲染出一个提示块，提醒用户重新让你生成一遍
            <speedChart id="编号" />      向用户展示ta演奏的历史速度
            <forceChart id="编号" />      向用户展示ta演奏的力度（和实例演奏的对比）
            <rhythmChart id="编号" />     向用户展示ta演奏的节奏波动幅度折线图
            <midiChart id="编号" />       向用户展示ta演奏的midi钢琴窗图（会显示错音信息）
            <refMidiChart id="编号" />    向用户展示对应示例演奏的midi钢琴窗图（可以播放供用户参考，所以一定要单独列出来并提示用户这是示例演奏）
        
            （其他组件）
            <locateAudio id="编号" skip="从指定秒数开始播放" tip="超链接显示的提示" />
                                         生成一个超链接，用户点击后会播放ta发送的音频，可以skip指定到错音或节奏不稳位置让用户播放参考
                    示例：<locateAudio id="1" skip="3.5" tip="点我播放音频" />，生成一个显示为“点我播放音频”的超链接，点击后从3.5秒开始播放）
        
            <intent activity="页面名" />  生成一个按钮，用户点击后app会跳转到指定页面
                    页面名可以是以下值：
                        钢琴窗     手机上的模拟钢琴，不是真的钢琴，一般用于推荐给用户创作旋律
                        节拍器     支持n对n复杂节奏，不过一般要曲目有这些复杂节奏再告诉用户n对n节拍功能，一般打正常节奏慢速练或者稳速度
                        和弦听辨
                        节奏听辨
        
        
        尽量不要用 markdown ``` 代码块，会有渲染问题，用中文回复用户。
        """)
public class C7b9Agent
{
    private final Gson gson;
    private final Library library;
    private final User user;
    private final ChatWriter writer;
    private final PianoMessage message;
    private final AudioService audioService;
    private final ChatService chatService;
    public C7b9Agent(ApplicationContext context, User user, ChatWriter writer, PianoMessage message)
    {
        this.user = user;
        this.writer = writer;
        this.message = message;
        this.gson = context.getBean(Gson.class);
        this.library = context.getBean(Library.class);
        this.audioService = context.getBean(AudioService.class);
        this.chatService = context.getBean(ChatService.class);
    }

    @Description("识别用户上传的音频信息（如果是钢琴曲还会匹配演奏的曲目和力度速度信息等）")
    @Name("识别音频")
    public String info()
    {
        if(!message.audios.isEmpty())
        {
            StringBuilder builder = new StringBuilder();
            //上传了音频文件执行
            for(int aid : message.audios)
            {
                Audio audio = audioService.get(user.getUid(), aid);
                builder.append("（id编号：").append(aid).append("（注意：后续XML标签的id需要用这个值），音频内容").append("：\n");

                if(audio.getContent() == null || audio.getContent().isEmpty())
                {
                    if(chatService.isPianoAudio(aid))
                    {
                        byte[] mid = Objects.requireNonNullElse(audio.getMid(), audioService.toMidi(user.getUid(), aid));

                        AnalyzeResult analysis;
                        if(audio.getAnalysis() != null) analysis = gson.fromJson(audio.getAnalysis(), AnalyzeResult.class);
                        else
                        {
                            Analyzer analyzer = new MidiImpl("用户音频" + aid, mid).analyzer();
                            Consumer<String> listener = getAnalyzeInfoConsumer();
                            analysis = analyzer.analyze(library.findAll(analyzer, 0.4f, 0f, listener).get(0));
                        }

                        if(audio.getMp3() != null)
                        {
                            String content = chatService.audioContent(aid);
                            builder.append("描述：").append(content).append("（模型识别，仅供参考）\n");
                        }
                        if(audio.getMid() == null) audioService.setMidData(user.getUid(), aid, mid);
                        builder.append(chatService.analysisContent(user.getUid(), audio, analysis, writer)).append("\n");
                        audioService.setAnalyzeData(user.getUid(), aid, analysis);      //在调用find后保存analysis
                    }
                    else if(audio.getMp3() != null)
                    {
                        String content = chatService.audioContent(aid);
                        builder.append("音频描述：").append(content).append("\n");
                    }
                    builder.append("）");
                    audioService.setAudioContentData(audio.getUid(), aid, builder.toString());
                }
                else builder.append(audio.getContent());
            }
            return "（用户共发送了" + message.audios.size() + "段音频）\n" + builder;
        }
        else return "（用户没有发送音频）";
    }

    private @NotNull Consumer<String> getAnalyzeInfoConsumer()
    {
        int[] count = new int[1];
        return str ->
        {
            if(count[0] >= 100)
            {
                try { writer.sendStreamJson("context", "find", str); }
                catch (IOException e) { throw new RuntimeException(e); }
                count[0] = 0;
            }
            count[0] ++;
        };
    }

    @Description("续写某段钢琴曲，返回续写后的音频编号，长度是随机的（耗时比较长，用户需要时再调用）")
    @Name("续写")
    public String produce(@Name("id") @Required String aidStr)
    {
        try
        {
            int aid = Integer.parseInt(aidStr);
            StringBuilder builder = new StringBuilder();
            Audio audio = audioService.get(user.getUid(), aid);
            if(chatService.isPianoAudio(aid))
            {
                byte[] mid = Objects.requireNonNullElse(audio.getMid(), audioService.toMidi(user.getUid(), aid));
                Midi midi = new MidiImpl("用户音频" + aid, mid);
                int newAid = audioService.saveMid(user.getUid(), chatService.produce(midi));
                builder.append("已续写音频编号：").append(aid).append("，续写后的新编号：").append(newAid).append("\n")
                        .append("发送 <midiChart id=\"").append(newAid).append("\" /> 显示midi流\n")
                        .append("发送 <locateAudio id=\"").append(newAid).append("\" skip=\"0\" tip=\"点我播放\" /> 显示播放音频链接");
            }
            return builder.toString();
        }
        catch (NumberFormatException e)
        {
            return "音频编号必须为数字";
        }
    }
}
