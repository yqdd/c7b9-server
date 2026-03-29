package com.ow0b.midi.analyzer;

import com.ow0b.midi.AnalyzeResult;
import com.ow0b.midi.Midi;
import com.ow0b.midi.Note;
import com.ow0b.midi.analyzer.group.NoteGroup;
import com.ow0b.midi.library.Library;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public interface Analyzer
{
    /// 获取原始midi对象
    Midi getMidi();
    /// 获取所有音组（离得近的音分为一组）
    List<NoteGroup> getGroups();
    /// 获取所有音组的连续的（不会有突变的0值）力度
    List<Float> getForces();

    /// 传入通过 {@link Library#findAll(Analyzer, float, float, Consumer)} 获取的匹配项，生成 力度、速度、节奏、踏板、错音 等数据
    AnalyzeResult analyze(Library.FindItem item);
}
