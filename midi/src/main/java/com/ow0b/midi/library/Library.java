package com.ow0b.midi.library;

import com.ow0b.midi.analyzer.Analyzer;
import com.ow0b.midi.analyzer.group.NoteGroup;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public interface Library
{
    @AllArgsConstructor
    class Range
    {
        public int startIndex;
        public int endIndex;
        public float startTime;
        public float endTime;
        public List<NoteGroup> groups;
        public List<Integer> count;
        public float value;
        public float sim;
        public float smooth;
    }
    @AllArgsConstructor
    class FindItem
    {
        public Analyzer analyzer;
        public String name;
        public Range range;
    }

    Analyzer getAnalyzer(String name);
    /// 根据名字查找对应的Analyzer
    @NotNull List<FindItem> findFromName(Analyzer analyzer, String name);
    /// 查找预初始化的所有midi按匹配度排序
    List<FindItem> findAll(Analyzer analyzer, float limit, float deviation, @Nullable Consumer<String> infoConsumer);
}
