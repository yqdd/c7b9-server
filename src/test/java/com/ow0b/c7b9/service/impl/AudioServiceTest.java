package com.ow0b.c7b9.service.impl;

import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

class AudioServiceTest
{
    @Test
    void mp3Time()
    {
        File file = new File("temp/temp-14901625472859322934.mp3");
        try(FileInputStream input = new FileInputStream(file))
        {
            // 获取音频文件信息
            AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(file);
            // mp3spi 会在 properties 中保存 MP3 的相关属性
            Map<?, ?> properties = fileFormat.properties();
            // duration 的单位通常是微秒（microseconds）
            Long microseconds = (Long) properties.get("duration");
            if (microseconds != null)
            {
                System.out.println("MP3 文件时长（秒）： " + microseconds / 1000f / 1000f);
            }
            else
            {
                System.out.println("无法读取 MP3 文件的时长信息。");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}