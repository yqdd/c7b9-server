package com.ow0b.c7b9.service;

import com.ow0b.c7b9.controller.impl.audio.AudioAccessDeniedException;
import com.ow0b.c7b9.service.database.json.Practice;
import com.ow0b.c7b9.service.database.model.Audio;
import com.ow0b.midi.AnalyzeResult;
import com.ow0b.midi.Midi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface AudioService
{
    /// 将m4a转为mp3存储到数据库
    int saveMp3(int uid, byte @NotNull [] m4a);
    /// 只保存mid，没有对应的mp3文件
    int saveMid(int uid, byte @NotNull [] mid);
    /// 只保存mid，没有对应的mp3文件（使用Midi对象）
    int saveMid(int uid, @NotNull Midi midi);
    /// 通过创建的secret密钥获取audio
    Audio get(@NotNull String secret) throws AudioAccessDeniedException;
    /// 通过aid获取audio（uid用于识别音频是否属于该用户）
    Audio get(int uid, int aid) throws AudioAccessDeniedException;
    /// 删除aid对应的音频的所有数据
    void delete(int uid, int aid) throws AudioAccessDeniedException;
    /// 创建一个secret密钥，允许在seconds秒内访问音频
    String access(int uid, int aid, int seconds) throws AudioAccessDeniedException;
    /// 设置mp3音频的mid数据
    void setMidData(int uid, int aid, byte @NotNull [] data) throws AudioAccessDeniedException;
    /// 设置音频的描述数据
    void setAudioContentData(int uid, int aid, @Nullable String content) throws AudioAccessDeniedException;
    /// 设置音频的分析数据
    void setAnalyzeData(int uid, int aid, @Nullable AnalyzeResult analysis) throws AudioAccessDeniedException;

    /// 获取用户历史练习数据
    Practice getPractice(int uid);
    /// 设置用户历史练习数据
    void setPractice(int uid, @NotNull Practice practice);

    /// 获取mp3音频的总时长
    float mp3Time(byte[] mp3);
    /// 将数据库的mp3文件转为.mid格式
    byte[] toMidi(int uid, int aid);
    /// 将Midi转为llm提示词格式（用于传入rwkv模型续写）
    String encodeTokenizer(@NotNull Midi midi);
    /// 将（续写后的）llm提示词转回Midi
    byte[] decodeTokenizer(@NotNull String tokenizer);
}
