package com.ow0b.midi.analyzer.group;

import com.ow0b.midi.MinHash;
import com.ow0b.midi.Note;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class NoteGroup implements NoteGroupOperation
{
    private static final MinHash minHash = new MinHash(300);
    public final Set<Note> notes;
    public transient final int[] hash;
    public NoteGroup(Collection<Note> notes)
    {
        this.notes = Set.copyOf(notes);
        this.hash = minHash.computeMinHash(notes.stream()
                .map(n -> n.note)
                .collect(Collectors.toSet()));
    }
    public static class Builder extends HashSet<Note>
    {
        public NoteGroup build()
        {
            return new NoteGroup(this);
        }
    }
    public static Builder builder()
    {
        return new Builder();
    }

    @Override
    public float start()
    {
        return (float) notes.stream().mapToDouble(n -> n.start).min().orElse(0);
    }
    @Override
    public float end()
    {
        return (float) notes.stream().mapToDouble(n -> n.end).max().orElse(0);
    }
    @Override
    public float[] range()
    {
        return new float[] {start(), end()};
    }

    @Override
    public float similarity(NoteGroup g2)
    {
        //return g2.notes.isEmpty() ? 0 : (float) g2.notes.stream().filter(n -> notes.stream().map(n0 -> n0.pitch).toList().contains(n.pitch)).count() / g2.notes.size();
        return (float) minHash.computeSimilarity(hash, g2.hash);
    }

    @Override
    public float force()
    {
        return (float) notes.stream().mapToInt(n -> n.force).average().orElse(0);
    }
    @Override
    public List<Note> mistake(Collection<NoteGroup> g2s)
    {
        Set<Note> trueNote = new HashSet<>();        //正确的音
        for(Note note : notes)
        {
            if(note.force < 5) continue;
            for(Note refNote : g2s.stream()
                    .<Note>mapMulti((g, c) -> g.notes.forEach(c))
                    .toList())
            {
                switch ((note.pitch - refNote.pitch) % 12)
                {
                    case 0 -> trueNote.add(note);     //八度或一度
                    case 7 -> trueNote.add(note);     //纯五度
                    case 5 -> trueNote.add(note);     //纯四度
                    case 4 -> trueNote.add(note);     //大三
                    case 3 -> trueNote.add(note);     //小三
                    case 9 -> trueNote.add(note);     //大六
                    case 8 -> trueNote.add(note);     //小六
                }
            }
        }
        return notes.stream().filter(n -> !trueNote.contains(n)).toList();
    }

    /// 获取所有音组的起始位置和结束位置
    public static float[] rangeOf(Collection<NoteGroup> gs, Function<NoteGroup, ? extends Number> start, Function<NoteGroup, ? extends Number> end)
    {
        float startVal = (float) gs.stream().mapToDouble(g -> start.apply(g).doubleValue()).min().orElse(0),
                //这里end也用start（节奏主要是对比开头部分（音的起始位置））
                endVal = (float) gs.stream().mapToDouble(g -> end.apply(g).doubleValue()).max().orElse(0);

        return new float[] {startVal, endVal};
    }
}
