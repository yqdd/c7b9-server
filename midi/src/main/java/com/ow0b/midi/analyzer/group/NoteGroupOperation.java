package com.ow0b.midi.analyzer.group;

import com.ow0b.midi.Note;

import java.util.Collection;
import java.util.List;

public interface NoteGroupOperation
{
    /// 获取与另一group相似度
    float similarity(NoteGroup g2);
    /// 获取该group起始音
    float start();
    /// 获取该group结束音
    float end();
    /// 返回一个数组，编号0为 {@link NoteGroupOperation#start()}，编号1为 {@link NoteGroupOperation#end()}
    float[] range();

    /// 获取该group所有音平均力度
    float force();
    /// 获取该group在另一堆参考group中可能的错音
    List<Note> mistake(Collection<NoteGroup> g2s);
}
