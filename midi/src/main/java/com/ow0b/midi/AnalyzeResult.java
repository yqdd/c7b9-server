package com.ow0b.midi;

import lombok.Builder;
import lombok.ToString;

import java.util.List;

@Builder
@ToString
public class AnalyzeResult
{
    public String name;
    public float similarity;
    public float totalTime;
    public float startTime;
    public float endTime;

    /// 索引对应的 用户音频 的时间位置
    public List<Float> indexesUserTimes;
    /// 速度，整体与示例演奏的速度比（大于1为弹慢了，小于1为弹快了）
    public float speed;
    /// 力度，每个音组与示例演奏对应音组的力度比
    public List<Float> forces;
    /// 上传音频的力度（取组平均值的浮点，最大值仍是127）
    public List<Float> thisForces;
    /// 参考音频的力度（取组平均值的浮点，最大值仍是127）
    public List<Float> refForces;
    /// 节奏，每个音组与上一个音组间隔 与 示例演奏与上一个音组间隔 的比（起始索引默认为1，大于1为弹慢了，小于1为弹快了）
    public List<Float> rhythm;
    /// 错音，每个音组的错音情况
    public List<List<Note>> mistakes;
    /// 踏板，应在哪个时间踩或放踏板
    public record Reverb(float time, boolean rev) {}
    public List<Reverb> reverb;
}
