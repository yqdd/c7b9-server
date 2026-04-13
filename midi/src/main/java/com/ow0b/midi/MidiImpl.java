package com.ow0b.midi;

import com.ow0b.midi.analyzer.Analyzer;
import com.ow0b.midi.analyzer.AnalyzerImpl;
import com.ow0b.midi.analyzer.group.NoteGroup;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.sound.midi.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

@Getter
public class MidiImpl implements Midi
{
    public final float internal = 0.2f;
    public final String name;
    public final byte[] data;
    public final LinkedList<Note> notes = new LinkedList<>();
    public MidiImpl(String name, List<Note> notes)
    {
        this.name = name;
        this.data = null;
        this.notes.addAll(notes);
    }
    public MidiImpl(String name, byte[] data)
    {
        this.name = name;
        this.data = data;
        try
        {
            //激活的音符列表
            HashMap<Integer, Note> actives = new HashMap<>();
            Sequence sequence = MidiSystem.getSequence(new ByteArrayInputStream(data));
            //this.time = mp3Time <= 0 ? sequence.getMicrosecondLength() / 1000000f : mp3Time;
            float timeUnit = 60f / getTempoInBpm(sequence) / sequence.getResolution();
            for(Track track : sequence.getTracks())
            {
                for(int i = 0; i < track.size(); i++)
                {
                    MidiEvent event = track.get(i);
                    if(event.getMessage() instanceof ShortMessage message)
                    {
                        int pitch = message.getData1();
                        int force = message.getData2();
                        //NOTE_ON事件后音符放到激活列表
                        if(message.getCommand() == ShortMessage.NOTE_ON && force > 0)
                        {
                            Note note = new Note(pitch, event.getTick() * timeUnit, 0, force);
                            actives.put(pitch, note);
                            notes.add(note);
                        }
                        else if((message.getCommand() == ShortMessage.NOTE_ON && force == 0) ||
                                message.getCommand() == ShortMessage.NOTE_OFF)
                        {
                            Note note = actives.get(pitch);
                            if(note != null)
                            {
                                note.end = event.getTick() * timeUnit;
                                notes.add(note);
                                actives.remove(pitch);
                            }
                        }
                    }
                }
            }
            notes.sort((n1, n2) -> Float.compare(n1.start, n2.start));
        }
        catch (InvalidMidiDataException | IOException e)
        {
            throw new RuntimeException(e);
        }
    }
    public float getTime()
    {
        return (float) notes.stream().mapToDouble(n -> n.end).max().orElse(0);
    }
    @Override
    public @NotNull MidiImpl clip(float start, float end)
    {
        List<Note> list = this.notes.stream()
                .filter(n -> n.start >= start && n.start <= end)
                .toList();
        return new MidiImpl(name + "_clip", list);
    }

    private float getTempoInBpm(Sequence sequence)
    {
        for (Track track : sequence.getTracks())
        {
            for (int i = 0; i < track.size(); i++)
            {
                MidiEvent event = track.get(i);
                MidiMessage message = event.getMessage();
                if (message instanceof MetaMessage meta)
                {
                    if (meta.getType() == 0x51)
                    {
                        byte[] data = meta.getData();
                        int microsecondsPerBeat = (data[0] & 0xFF) << 16
                                | (data[1] & 0xFF) << 8
                                | (data[2] & 0xFF);
                        return 60000000f / microsecondsPerBeat;
                    }
                }
            }
        }
        return 120f; // 默认值
    }
    private void setTempo(Track track, int bpm, long tick) throws InvalidMidiDataException
    {
        MetaMessage tempoMessage = new MetaMessage();
        int mpq = 60000000 / bpm; // 微秒每四分音符
        byte[] tempoData = {
                (byte) ((mpq >> 16) & 0xFF),
                (byte) ((mpq >> 8) & 0xFF),
                (byte) (mpq & 0xFF)
        };
        tempoMessage.setMessage(0x51, tempoData, tempoData.length);
        track.add(new MidiEvent(tempoMessage, tick));
    }

    @Override
    public void save(File file)
    {
        try
        {
            int resolution = 480,  // 每四分音符的tick数
                    bpm = 128;
            Sequence sequence = new Sequence(Sequence.PPQ, resolution);
            Track track = sequence.createTrack();
            setTempo(track, bpm, 0);
            // 设置乐器
            ShortMessage programChange = new ShortMessage();
            programChange.setMessage(ShortMessage.PROGRAM_CHANGE, 0, 0, 0);     //通道0，钢琴音色
            track.add(new MidiEvent(programChange, 0));
            // 添加音符事件
            for (Note note : notes)
            {
                // 音符开始事件
                ShortMessage noteOn = new ShortMessage();
                noteOn.setMessage(ShortMessage.NOTE_ON, 0, note.pitch, note.force);
                track.add(new MidiEvent(noteOn, (long) (note.start / (60f / bpm / resolution))));   //这里算的是tick
                // 音符结束事件
                ShortMessage noteOff = new ShortMessage();
                noteOff.setMessage(ShortMessage.NOTE_OFF, 0, note.pitch, 0);
                track.add(new MidiEvent(noteOff, (long) (note.end / (60f / bpm / resolution))));
            }
            // 写入文件
            MidiSystem.write(sequence, 1, file);
        }
        catch (InvalidMidiDataException | IOException e)
        {
            throw new RuntimeException(e);
        }
    }
    @Override
    public @NotNull Analyzer analyzer()
    {
        List<NoteGroup> result = new ArrayList<>();
        NoteGroup.Builder builder = NoteGroup.builder();
        for(Note note : notes)
        {
            if(!result.isEmpty())
            {
                float min = builder.stream()
                        .min((n1, n2) -> Float.compare(n1.start, n2.start))
                        .orElseGet(() -> new Note(0, 0, 0, 0)).start,
                        max = builder.stream()
                                .max((n1, n2) -> Float.compare(n1.start, n2.start))
                                .orElseGet(() -> new Note(0, 0, 0, 0)).start;
                if((builder.size() < 10 || Math.abs(min - note.start) < internal) && max - min < 0.5)
                {
                    builder.add(note);
                    continue;
                }
            }
            result.add(builder.build());
            builder = NoteGroup.builder();
        }
        return new AnalyzerImpl(this, result);
    }
}
