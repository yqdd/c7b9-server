package com.ow0b.midi;

import com.ow0b.midi.analyzer.Analyzer;
import com.ow0b.midi.analyzer.AnalyzerImpl;
import com.ow0b.midi.library.Library;
import com.ow0b.midi.library.MidiSWLibrary;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

class MidiTest
{
    @Test
    void test1()
    {
        try(FileInputStream stream = new FileInputStream(new File("D:\\c7b9-server\\midis2\\112223.mid")))
        {
            MidiSWLibrary lib = new MidiSWLibrary(new File("D:\\c7b9-server\\midis"));      //2\examples
            Analyzer analyzer = new MidiImpl("测试", stream.readAllBytes()).analyzer();
            System.out.println("groups数：" + analyzer.getGroups().size());
            List<Library.FindItem> result = lib.findAll(analyzer, 0.4f, 0f, System.out::println);
            for(int i = 0; i < Math.min(result.size(), 20); i ++)
            {
                Library.FindItem item = result.get(i);
                System.out.println(item.name + "  sim:" + item.range.sim + "  smooth:" + item.range.smooth + "    " + item.range.startTime + " ~ " + item.range.endTime);
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void saveMidiTest()
    {
        try(FileInputStream stream = new FileInputStream(new File("D:\\c7b9-server\\temp\\audios-104.mid")))
        {
            Midi midi = new MidiImpl("test", stream.readAllBytes());
            midi.save(new File("D:\\c7b9-server\\temp\\test-audio.mid"));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}