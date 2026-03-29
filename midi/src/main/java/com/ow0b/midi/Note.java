package com.ow0b.midi;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Note
{
    public String note;
    public byte pitch;
    public float start;
    public float end;
    public int force;
    public Note(int pitch, float start, float end, int force)
    {
        this(getName(pitch), (byte) pitch, start, end, force);
    }

    @Override
    public String toString()
    {
        return note;
    }

    public static String getName(int pitch)
    {
        if(pitch < 0) return null;
        String[] notes = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        int octave = (pitch / 12) - 1;   //计算八度数
        int noteInOctave = pitch % 12;   //获取八度内的音符位置
        return notes[noteInOctave] + octave;
    }
}
