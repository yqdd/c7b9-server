package com.ow0b.midi.analyzer.group;

import com.ow0b.midi.Note;

import java.util.ArrayList;
import java.util.List;

public class TestNoteGroup extends NoteGroup
{
    public TestNoteGroup(int offset, int... pitches)
    {
        super(toNotes(offset, pitches));
    }

    private static List<Note> toNotes(int offset, int[] pitches)
    {
        ArrayList<Note> notes = new ArrayList<>();
        for(int i = 0; i < pitches.length; i++)
            notes.add(new Note(pitches[i], offset + i, offset + i + 0.01f, 127));
        return notes;
    }
}
