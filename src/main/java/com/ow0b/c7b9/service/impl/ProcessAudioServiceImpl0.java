package com.ow0b.c7b9.service.impl;

import com.google.gson.Gson;
import com.ow0b.c7b9.controller.impl.audio.AudioAccessDeniedException;
import com.ow0b.c7b9.service.AudioService;
import com.ow0b.c7b9.service.database.json.Practice;
import com.ow0b.c7b9.service.database.mapper.AudioMapper;
import com.ow0b.c7b9.service.database.model.Audio;
import com.ow0b.midi.AnalyzeResult;
import com.ow0b.midi.Midi;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.function.Consumer;

@Slf4j
@Deprecated
public class ProcessAudioServiceImpl0 implements AudioService, TempDir
{
    @Setter(onMethod_ = @Autowired)
    private Gson gson;
    @Setter(onMethod_ = @Autowired)
    private AudioMapper audioMapper;

    @Override
    public int saveMp3(int uid, byte @NotNull [] m4a)
    {
        //m4a转mp3
        try
        {
            File m4aFile = File.createTempFile("temp-", ".m4a", temp.toFile()),
                    mp3File = File.createTempFile("temp-", ".mp3", temp.toFile());
            saveCache(m4aFile, m4a);
            //执行转换
            Process process = Runtime.getRuntime().exec(String.format("ffmpeg -i %s -y -acodec libmp3lame -aq 0 %s", m4aFile, mp3File));
            processCallback(process, log::info);
            deleteCache(m4aFile);
            try(FileInputStream stream = new FileInputStream(mp3File))
            {
                byte[] mp3 = stream.readAllBytes();
                Audio audio = new Audio(uid, m4a, mp3);
                audioMapper.insert(audio);
                if(mp3File.delete()) return audio.getAid();   //保存
                else throw new RuntimeException("缓存文件删除失败");
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
    @Override
    public int saveMid(int uid, byte @NotNull [] mid)
    {
        return 0;
    }
    @Override
    public int saveMid(int uid, @NotNull Midi midi)
    {
        return 0;
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
        if(audio.getUid() != uid) throw new AudioAccessDeniedException(aid);
        return audio;
    }
    @Override
    public void delete(int uid, int aid) throws AudioAccessDeniedException
    {

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
        if(audioMapper.getByAid(aid).getUid() != uid) throw new AudioAccessDeniedException(aid);
        audioMapper.setMid(aid, data);
    }
    @Override
    public void setAudioContentData(int uid, int aid, @Nullable String content) throws AudioAccessDeniedException
    {
        audioMapper.setAnalysis(aid, content);
    }
    @Override
    public void setAnalyzeData(int uid, int aid, @Nullable AnalyzeResult result) throws AudioAccessDeniedException
    {
        audioMapper.setAnalysis(aid, gson.toJson(result));
    }

    @Override
    public Practice getPractice(int uid)
    {
        return null;
    }

    @Override
    public void setPractice(int uid, @NotNull Practice practice)
    {

    }

    @Override
    public float mp3Time(byte[] mp3)
    {
        return 0;
    }


    private Process translateProcess = null;
    private BufferedReader translateIn, translateErr;

    @PostConstruct
    public void init()
    {
        File temp = TempDir.temp.toFile().getAbsoluteFile();
        if(!temp.exists() && !temp.mkdirs()) throw new RuntimeException("创建临时存储目录失败");
        try
        {
            //判断创建translate.py
            File translate = new File("translate.py");
            if(!translate.delete()) log.info("重新刷新 translate.py");
            if(!translate.exists())
            {
                if(translate.createNewFile())
                {
                    try(FileOutputStream output = new FileOutputStream(translate);
                        InputStream input = new ClassPathResource("translate.py").getInputStream())
                    {
                        output.write(input.readAllBytes());
                    }
                }
                else throw new RuntimeException("创建translate.py失败");
            }
            translateProcess = Runtime.getRuntime().exec(String.format("C:\\ProgramData\\miniconda3\\envs\\py37\\python.exe -u %1$s", translate));
            translateIn = translateProcess.inputReader();
            translateErr = translateProcess.errorReader();
            new Thread(() ->
            {
                log.info("开始加载模型");
                //要多次读取读取的流不能关（只能开一次，写入的可以开很多次）
                processCallback(translateIn, translateErr, "加载完成", log::info);
                log.info("模型加载完成");
            }).start();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
    @PreDestroy
    public void close()
    {
        if(translateProcess != null) translateProcess.destroy();
    }
    private void processCallback(Process process, Consumer<String> callback)
    {
        try(BufferedReader input = process.inputReader(); BufferedReader error = process.errorReader())
        {
            processCallback(input, error, null, callback);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
    private void processCallback(BufferedReader inputReader, BufferedReader errorReader, String breakStr, Consumer<String> callback)
    {
        try
        {
            String inputLine, errLine = null;
            while((inputLine = inputReader.readLine()) != null || (errLine = errorReader.readLine()) != null)
            {
                if(errLine != null) callback.accept(errLine);
                if(inputLine != null)
                {
                    callback.accept(inputLine);
                    if(inputLine.equals(breakStr)) break;
                }
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] toMidi(int uid, int aid)
    {
        return toMidi(uid, aid, str -> {});
    }
    public byte[] toMidi(int uid, int aid, Consumer<String> callback)
    {
        if(!translateProcess.isAlive()) throw new RuntimeException("模型转换进程已退出");
        Audio audio = get(uid, aid);
        if(audio.getMp3() == null || audio.getMid() != null) return get(uid, aid).getMid();
        try
        {
            File audioFile = File.createTempFile("temp-", ".mp3", temp.toFile()),
                    midiFile = File.createTempFile("temp-", ".mid", temp.toFile());
            saveCache(audioFile, audio.getMp3());
            //执行转换
            try(BufferedWriter writer = translateProcess.outputWriter())
            {
                writer.write(audioFile + " " + midiFile + "\n");
            }
            processCallback(translateIn, translateErr, "转换完成", callback);

            //midi保存到数据库
            try(FileInputStream input = new FileInputStream(midiFile))
            {
                byte[] data = input.readAllBytes();
                deleteCache(audioFile);
                return data;
            }
            finally { deleteCache(midiFile); }
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
            File file = File.createTempFile("rwkv-", ".mid", temp.toFile());
            midi.save(file);
            Process process = Runtime.getRuntime().exec(String.format("""
                    python MIDI-LLM-tokenizer/midi_to_str.py temp/%s\
                         --vocab_config MIDI-LLM-tokenizer/vocab_config.json --filter_config MIDI-LLM-tokenizer/filter_config.json
                    """, file.getName()));
            String[] result = new String[1];
            processCallback(process, str -> result[0] = str);
            deleteCache(file);
            return result[0].replace("<start> ", "").replace(" <end>", "");
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
    @Override
    public byte[] decodeTokenizer(@NotNull String tokenizer)
    {
        try
        {
            File file = File.createTempFile("rwkv-", ".mid", temp.toFile());
            Process process = Runtime.getRuntime().exec(String.format("""
                    python MIDI-LLM-tokenizer/str_to_midi.py "<start> %s <end>" --output temp/%s\
                         --vocab_config MIDI-LLM-tokenizer/vocab_config.json --filter_config MIDI-LLM-tokenizer/filter_config.json
                    """, tokenizer, file.getName()));
            processCallback(process, str -> {});
            try(InputStream stream = new FileInputStream(file))
            {
                byte[] result = stream.readAllBytes();
                deleteCache(file);
                return result;
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
