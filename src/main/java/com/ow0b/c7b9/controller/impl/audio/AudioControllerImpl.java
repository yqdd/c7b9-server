package com.ow0b.c7b9.controller.impl.audio;

import com.google.gson.Gson;
import com.ow0b.c7b9.annotation.LoginRequired;
import com.ow0b.c7b9.controller.AudioController;
import com.ow0b.c7b9.service.AudioService;
import com.ow0b.c7b9.service.database.json.Practice;
import com.ow0b.c7b9.service.database.model.Audio;
import com.ow0b.c7b9.service.database.model.User;
import com.ow0b.midi.AnalyzeResult;
import com.ow0b.midi.MidiImpl;
import com.ow0b.midi.Note;
import com.ow0b.midi.analyzer.Analyzer;
import com.ow0b.midi.library.Library;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@LoginRequired
@RestController
@RequestMapping
public class AudioControllerImpl implements AudioController
{
    @Setter(onMethod_ = @Autowired)
    private Gson gson;
    @Setter(onMethod_ = @Autowired)
    private AudioService audioService;
    @Setter(onMethod_ = @Autowired)
    private Library library;

    @Override
    @RequestMapping("/audio/upload")
    public Map<String, String> upload(User user, @RequestParam("type") String type, @RequestBody byte[] data)
    {
        data = Base64.getDecoder().decode(data);
        int aid;
        switch (type)
        {
            case "m4a" -> aid = audioService.saveMp3(user.getUid(), data);
            case "midi" -> aid = audioService.saveMid(user.getUid(), gson.fromJson(new InputStreamReader(new ByteArrayInputStream(data)), MidiImpl.class));
            default -> { return Map.of("error", "上传失败，不存在资源类型：" + type); }
        }
        return Map.of("info", "上传成功", "id", String.valueOf(aid));
    }

    @Override
    @RequestMapping("/audio/access")
    public Map<String, String> access(User user,
                                      @RequestParam("id") int aid,
                                      @RequestParam(value = "time", defaultValue = "60") int time)
    {
        return Map.of("secret", audioService.access(user.getUid(), aid, time));
    }

    @Override
    @RequestMapping("/audio")
    public byte[] audio(User user,
                        @RequestParam(value = "secret", required = false) String secret,
                        @RequestParam(value = "id", required = false) Integer aid)
    {
        Audio audio;
        if(secret != null) audio = audioService.get(secret);
        else if(aid != null) audio = audioService.get(user.getUid(), aid);
        else throw new IllegalArgumentException("至少需要指定secret或id的其中之一");

        return Objects.requireNonNullElse(audio.getM4a(), audio.getMid());
    }

    @Override
    @RequestMapping("/audio/forces")
    public Map<String, Object> forces(User user, @RequestParam(value = "id") int aid)
    {
        Audio audio = audioService.get(user.getUid(), aid);
        AnalyzeResult analysis = gson.fromJson(audio.getAnalysis(), AnalyzeResult.class);
        if(analysis != null) return Map.of("data", analysis.thisForces, "ref", analysis.refForces);
        else return Map.of("data", List.of(), "ref", List.of());
    }
    @Override
    @RequestMapping("/audio/practice")
    public Map<String, Object> practice(User user, @RequestParam(value = "id") int aid)
    {
        Audio audio = audioService.get(user.getUid(), aid);
        AnalyzeResult analysis = gson.fromJson(audio.getAnalysis(), AnalyzeResult.class);
        if(analysis != null)
        {
            Practice practice = audioService.getPractice(user.getUid());
            return Map.of("data", practice.speed.get(analysis.name));
        }
        else return Map.of("data", List.of());
    }
    @Override
    @RequestMapping("/audio/practice/delete")
    public Map<String, String> deletePractice(User user,
                               @RequestParam(value = "id") int aid,
                               @RequestParam(value = "amount", required = false) Integer amount)
    {
        Audio audio = audioService.get(user.getUid(), aid);
        Practice practice = audioService.getPractice(user.getUid());
        if(practice != null)
        {
            String name = gson.fromJson(audio.getAnalysis(), AnalyzeResult.class).name;
            List<Float> speeds = practice.speed.get(name);
            if(amount == null) speeds.clear();
            else if (speeds.size() > Math.max(0, speeds.size() - amount))
            {
                speeds.subList(Math.max(0, speeds.size() - amount), speeds.size()).clear();
            }
            practice.speed.put(name, speeds);
            audioService.setPractice(user.getUid(), practice);
            return Map.of("info", "删除成功");
        }
        else return Map.of("info", "删除失败，数据不存在");
    }
    @Override
    @RequestMapping("/audio/rhythms")
    public Map<String, Object> rhythms(User user, @RequestParam(value = "id") int aid)
    {
        Audio audio = audioService.get(user.getUid(), aid);
        AnalyzeResult analysis = gson.fromJson(audio.getAnalysis(), AnalyzeResult.class);
        if(analysis != null) return Map.of("data", analysis.rhythm.stream().map(f -> Math.min(1 / f, 2)).toList());
        else return Map.of("data", List.of());
    }
    @Override
    @RequestMapping("/audio/midi")
    public Map<String, Object> midi(User user, @RequestParam(value = "id") int aid)
    {
        Audio audio = audioService.get(user.getUid(), aid);
        if(audio.getAnalysis() != null)
        {
            AnalyzeResult analysis = gson.fromJson(audio.getAnalysis(), AnalyzeResult.class);
            List<Note> wrongs = analysis.mistakes.stream()
                    .<Note> mapMulti(Iterable::forEach)
                    //不是参考音频的group不用偏移
                    //.filter(n -> n.start >= analysis.startTime && n.end <= analysis.endTime)
                    //.map(n -> new Note(n.pitch, n.start - analysis.startTime, n.end - analysis.startTime, n.force))
                    .toList();
            return Map.of("data", new MidiImpl("音频midi" + aid, audio.getMid()).notes, "wrongs", wrongs);
        }
        else if(audio.getMid() != null) return Map.of("data", new MidiImpl("音频midi" + aid, audio.getMid()).notes);
        else return Map.of("data", new MidiImpl("音频midi" + aid, List.of()));

    }
    @Override
    @RequestMapping("/audio/ref")
    public Map<String, Object> refMidi(User user, @RequestParam(value = "id") int aid)
    {
        Audio audio = audioService.get(user.getUid(), aid);
        if(audio.getM4a() == null)
        {
            if(audio.getMid() != null) return Map.of("data", new MidiImpl("音频midi" + aid, audio.getMid()).notes);
            else return Map.of("data", new MidiImpl("音频midi" + aid, List.of()));
        }
        else
        {
            if(audio.getAnalysis() != null)
            {
                AnalyzeResult analysis = gson.fromJson(audio.getAnalysis(), AnalyzeResult.class);
                Analyzer analyzer = library.findFromName(analysis.name);
                List<Note> notes = analyzer.getMidi().getNotes().stream()
                        .filter(n -> n.start >= analysis.startTime && n.end <= analysis.endTime)
                        .map(n -> new Note(n.pitch, n.start - analysis.startTime, n.end - analysis.startTime, n.force))
                        .toList();
                return Map.of("data", notes);
            }
            else return Map.of("data", List.of(), "info", "无匹配数据");
        }
    }

    @ExceptionHandler({AudioAccessDeniedException.class})
    public Map<String, String> resourceAccessDenied()
    {
        return Map.of("error", "没有访问权限");
    }
}
