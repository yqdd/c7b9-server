package com.ow0b.c7b9.service;

import com.ow0b.c7b9.ChatWriter;
import com.ow0b.c7b9.controller.impl.chat.ContextAccessDeniedException;
import com.ow0b.c7b9.service.database.json.ContextData;
import com.ow0b.c7b9.service.database.json.Conversations;
import com.ow0b.c7b9.service.database.model.Audio;
import com.ow0b.c7b9.service.database.model.Context;
import com.ow0b.midi.AnalyzeResult;
import com.ow0b.midi.Midi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ChatService
{
    /// 保存一个新的对话的信息到数据库
    /// @return 返回sid，可获取对话上下文
    int newConversationContext(int uid, @Nullable String title, @Nullable ContextData data);
    /// 获取用户的所有对话的sid和标题
    Conversations getConversations(int uid);
    /// 获取用户的某个sid的对话的所有信息
    Context getContext(int uid, int sid) throws ContextAccessDeniedException;
    /// 删除用户的某个sid对话所有信息（包括其中的音频）
    void deleteContext(int uid, int sid) throws ContextAccessDeniedException;
    /// 获取用户的某个sid的对话的对话数据
    ContextData getContextData(int uid, int sid) throws ContextAccessDeniedException;
    /// 获取用户的所有对话sid或标题数据
    void setConversations(int uid, Conversations conv);
    /// 更新用户的某个sid的对话的对话数据
    void setContextData(int uid, int sid, @Nullable ContextData data) throws ContextAccessDeniedException;

    /// 更新用户所有对话的标题，根据内容生成ai标题
    void titleConversations(int uid);


    /// 调用模型判断是否为钢琴音频
    boolean isPianoAudio(int aid);
    /// 调用模型判断是否需要续写钢琴曲
    boolean isNeedProducing(String text);
    /// 调用模型续写音频
    Midi produce(@NotNull Midi midi);
    /// 调用模型识别音频内容
    String audioContent(int aid);
    /// 返回音频分析结果提示词
    String analysisContent(int uid, @NotNull Audio audio, @Nullable AnalyzeResult analysis, @NotNull ChatWriter writer);
}
