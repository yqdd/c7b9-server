package com.ow0b.midi.analyzer;

import com.ow0b.midi.AnalyzeResult;
import com.ow0b.midi.Midi;
import com.ow0b.midi.Note;
import com.ow0b.midi.analyzer.group.NoteGroup;
import com.ow0b.midi.library.Library;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter(onMethod_ = @Override)
public class AnalyzerImpl implements Analyzer
{
    private final Midi midi;
    private final List<NoteGroup> groups;
    private final List<Float> forces;

    public AnalyzerImpl(Midi midi, List<NoteGroup> groups)
    {
        this.midi = midi;
        this.groups = groups;
        float[] near = new float[1];
        this.forces = groups.stream()
                .map(set -> near[0] = (set.notes.isEmpty() ? near[0] : set.force()))
                .toList();
    }

    @Override
    public AnalyzeResult analyze(Library.FindItem item)
    {
        return AnalyzeResult.builder().name(item.name).similarity(item.range.value)
                .totalTime(item.analyzer.getMidi().getNotes().get(item.analyzer.getMidi().getNotes().size() - 1).end)
                .startTime(item.range.startTime).endTime(item.range.endTime)
                .indexesUserTimes(groups.stream().map(NoteGroup::start).toList())
                .speed(speed(item))
                .forces(forces(item))
                .thisForces(item.analyzer.getGroups().stream().map(NoteGroup::force).toList())
                .refForces(item.analyzer.getGroups().stream().map(NoteGroup::force).toList())
                .rhythm(rhythm(item))
                .mistakes(mistakes(item))
                .reverb(List.of())
                .build();
    }

    private float speed(Library.FindItem item)
    {
        float[] range = NoteGroup.rangeOf(groups, NoteGroup::start, NoteGroup::start);
        return (range[1] - range[0]) / (item.range.endTime - item.range.startTime);
    }
    private List<Float> forces(Library.FindItem item)
    {
        List<Float> forces = new ArrayList<>();
        for(int i = 0; i < groups.size(); i ++)
        {
            if(groups.get(i).notes.isEmpty() || item.range.groups.get(i).notes.isEmpty())
            {
                if(i == 0) forces.add(0f);
                else forces.add(forces.get(i - 1));
            }
            else forces.add(groups.get(i).force() / item.range.groups.get(i).force());
        }
        return forces;
    }
    private List<Float> rhythm(Library.FindItem item)
    {
        List<Float> rhythms = new ArrayList<>();
        rhythms.add(1f);
        for(int i = 1; i < groups.size(); i ++)
        {
            if(groups.get(i).notes.isEmpty() || item.range.groups.get(i).notes.isEmpty())
                rhythms.add(rhythms.get(i - 1));
            else
            {
                float value = (groups.get(i).start() - groups.get(i - 1).start()) /
                        (item.range.groups.get(i).start() - item.range.groups.get(i - 1).start()) * speed(item);
                if(value < 0) rhythms.add(rhythms.get(i - 1));
                else rhythms.add(value);
            }

        }
        return rhythms;
    }
    private List<List<Note>> mistakes(Library.FindItem item)
    {
        List<List<Note>> forces = new ArrayList<>();
        for(int i = 0; i < groups.size(); i ++)
        {
            forces.add(groups.get(i).mistake(List.of(item.range.groups.get(i))));
        }
        return forces;
    }
}
