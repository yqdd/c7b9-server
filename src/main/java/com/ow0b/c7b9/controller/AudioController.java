package com.ow0b.c7b9.controller;

import com.ow0b.c7b9.service.database.model.User;

import java.util.Map;

public interface AudioController
{
    /**
     * 上传音频资源，可上传音频或Midi类对象
     * @param type 资源类型（为m4a或mid）
     */
    Map<String, String> upload(User user, String type, byte[] data);

    /**
     * 允许音频资源在指定的时间内可被外部密钥访问，返回结果中包含密钥
     * @param aid 音频资源id
     * @param time 可被外部密钥访问的有效时间
     */
    Map<String, String> access(User user, int aid, int time);

    /**
     * 访问音频资源（需指定密钥或id的其中一个）
     * @param secret 音频资源密钥
     * @param aid 音频资源id
     */
    byte[] audio(User user, String secret, Integer aid);

    /**
     * 获取某段音频的力度和匹配到音频的力度
     * @param aid 音频资源id
     */
    Map<String, Object> forces(User user, int aid);
    /**
     * 获取某段音频与匹配到音频每个音组的速度（节奏）比
     * @param aid 音频资源id
     */
    Map<String, Object> rhythms(User user, int aid);
    /**
     * 获取某段音频的历史速度数据
     * @param aid 音频资源id
     */
    Map<String, Object> practice(User user, int aid);
    /**
     * 删除用户某首曲目的历史练习数据
     */
    Map<String, String> deletePractice(User user, int aid, Integer amount);

    /**
     * 获取某段音频的midi数据
     * @param aid 音频资源id
     */
    Map<String, Object> midi(User user, int aid);
    /**
     * 获取某段音频匹配到的示例音频midi数据
     * @param aid 音频资源id
     */
    Map<String, Object> refMidi(User user, int aid);
}
