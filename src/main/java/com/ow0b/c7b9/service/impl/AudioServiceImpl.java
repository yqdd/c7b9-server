package com.ow0b.c7b9.service.impl;

import com.google.gson.Gson;
import com.ow0b.c7b9.controller.impl.audio.AudioAccessDeniedException;
import com.ow0b.c7b9.service.AudioService;
import com.ow0b.c7b9.service.converter.ConverterService;
import com.ow0b.c7b9.service.database.json.Practice;
import com.ow0b.c7b9.service.database.mapper.AudioMapper;
import com.ow0b.c7b9.service.database.mapper.UserMapper;
import com.ow0b.c7b9.service.database.model.Audio;
import com.ow0b.midi.AnalyzeResult;
import com.ow0b.midi.Midi;
import jakarta.annotation.PostConstruct;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import retrofit2.Retrofit;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Random;

@Slf4j
@Service
public class AudioServiceImpl implements AudioService, TempDir
{
    @Setter(onMethod_ = @Autowired)
    private Gson gson;
    @Setter(onMethod_ = @Autowired)
    private AudioMapper audioMapper;
    @Setter(onMethod_ = @Autowired)
    private Retrofit retrofit;
    @Setter(onMethod_ = @Autowired)
    private UserMapper userMapper;

    private ConverterService converterService;
    @PostConstruct
    public void init()
    {
        converterService = retrofit.create(ConverterService.class);
    }

    @Override
    public int saveMp3(int uid, byte @NotNull [] m4a)
    {
        try(ResponseBody body = converterService.m4aToMp3(RequestBody.create(m4a)).execute().body())
        {
            byte[] mp3 = Objects.requireNonNull(body).bytes();
            Audio audio = new Audio(uid, m4a, mp3);
            audioMapper.insert(audio);
            return audio.getAid();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
    @Override
    public int saveMid(int uid, byte @NotNull [] mid)
    {
        Audio audio = new Audio(uid, null, null);
        audioMapper.insert(audio);
        audioMapper.setMid(audio.getAid(), mid);
        return audio.getAid();
    }
    @Override
    public int saveMid(int uid, @NotNull Midi midi)
    {
        try
        {
            File file = File.createTempFile("temp-", ".mid", temp.toFile());
            midi.save(file);
            try(FileInputStream input = new FileInputStream(file))
            {
                Audio audio = new Audio(uid, null, null);
                audioMapper.insert(audio);
                audioMapper.setMid(audio.getAid(), input.readAllBytes());
                return audio.getAid();
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Audio get(@NotNull String secret) throws AudioAccessDeniedException
    {
        return audioMapper.getBySecret(secret);
    }
    @Override
    public Audio get(int uid, int aid) throws AudioAccessDeniedException
    {
        Audio audio = audioMapper.getByAid(aid);
        if(audio == null || audio.getUid() != uid) throw new AudioAccessDeniedException(aid);
        return audio;
    }
    @Override
    public void delete(int uid, int aid) throws AudioAccessDeniedException
    {
        Audio audio = audioMapper.getByAid(aid);
        if(audio == null || audio.getUid() != uid) throw new AudioAccessDeniedException(aid);

    }

    @Override
    public String access(int uid, int aid, int seconds) throws AudioAccessDeniedException
    {
        if(audioMapper.getByAid(aid).getUid() != uid) throw new AudioAccessDeniedException(aid);
        String secret;      //避免secret重复
        do { secret = Long.toHexString(new Random().nextLong(Long.MAX_VALUE)); }
        while(audioMapper.getSecretAid(secret) != null);

        audioMapper.setAccess(aid, Timestamp.from(Instant.now().plus(seconds, ChronoUnit.SECONDS)), secret);
        return secret;
    }
    @Override
    public void setMidData(int uid, int aid, byte @NotNull [] data) throws AudioAccessDeniedException
    {
        Audio audio = audioMapper.getByAid(aid);
        if(audio == null || audio.getUid() != uid) throw new AudioAccessDeniedException(aid);
        audioMapper.setMid(aid, data);
    }
    @Override
    public void setAudioContentData(int uid, int aid, @Nullable String content) throws AudioAccessDeniedException
    {
        Audio audio = audioMapper.getByAid(aid);
        if(audio == null || audio.getUid() != uid) throw new AudioAccessDeniedException(aid);
        audioMapper.setContent(aid, content);
    }
    @Override
    public void setAnalyzeData(int uid, int aid, @Nullable AnalyzeResult result) throws AudioAccessDeniedException
    {
        Audio audio = audioMapper.getByAid(aid);
        if(audio == null || audio.getUid() != uid) throw new AudioAccessDeniedException(aid);
        audioMapper.setAnalysis(aid, gson.toJson(result));
    }

    @Override
    public float mp3Time(byte[] mp3)
    {
        try
        {
            File file = File.createTempFile("temp-", ".mp3", temp.toFile());
            try(FileOutputStream output = new FileOutputStream(file)) { output.write(mp3); }
            catch (IOException e) { throw new RuntimeException(e); }

            AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(file);
            // duration 的单位通常是微秒（microseconds）
            Long microseconds = (Long) fileFormat.properties().get("duration");
            deleteCache(file);
            if (microseconds != null)
                return microseconds / 1000f / 1000f;
            else
                throw new RuntimeException("无法获取MP3时长信息");
        }
        catch (UnsupportedAudioFileException | IOException e)
        {
            throw new RuntimeException(e);
        }
    }
    @Override
    public Practice getPractice(int uid)
    {
        return gson.fromJson(userMapper.getByUid(uid).getPractice(), Practice.class);
    }
    @Override
    public void setPractice(int uid, @NotNull Practice practice)
    {
        userMapper.setPractice(uid, gson.toJson(practice));
    }


    @Override
    public byte[] toMidi(int uid, int aid)
    {
        try(ResponseBody body = converterService.audioToMidi(RequestBody.create(audioMapper.getByAid(aid).getM4a())).execute().body())
        {
            byte[] mid = Objects.requireNonNull(body).bytes();
            audioMapper.setMid(aid, mid);
            return mid;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
    @Override
    public String encodeTokenizer(@NotNull Midi midi)
    {
        try
        {
            File file = File.createTempFile("temp-", ".mid", temp.toFile());
            midi.save(file);
            try(FileInputStream input = new FileInputStream(file);
                ResponseBody body = converterService.midToText(RequestBody.create(input.readAllBytes())).execute().body())
            {
                log.error("midi转llm文本出错");
                return body == null ? null : body.string();
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
    @Override
    public byte[] decodeTokenizer(@NotNull String tokenizer)
    {
        try(ResponseBody body = converterService.textToMidi(RequestBody.create(tokenizer, MediaType.parse("text/plain"))).execute().body())
        {
            return Objects.requireNonNull(body).bytes();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
