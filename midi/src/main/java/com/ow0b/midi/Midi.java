package com.ow0b.midi;

import com.ow0b.midi.analyzer.Analyzer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

public interface Midi
{
    @NotNull String getName();
    float getTime();
    byte @Nullable [] getData();
    @NotNull List<Note> getNotes();

    /// 在指定 start-end 时间段内裁取一段midi
    @NotNull Midi clip(float start, float end);
    /// 保存.mid数据到指定file中
    void save(File file);
    /// 获取所有离得近的音分为一个个音组数据 和 连续的不存在突变的0值的力度数据
    @NotNull Analyzer analyzer();
}