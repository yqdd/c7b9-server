package com.ow0b.c7b9.controller;

import com.ow0b.c7b9.service.database.model.User;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;
import java.util.Map;

public interface ChatController
{
    /**
     * 调用模型api，流式返回生成内容
     * @param message 提示词
     * @param id 对话id
     * @param audios 附带的音频资源id，附带在body（通过[0, 1, ...]声明，可附带多个）
     */
    StreamingResponseBody chat(User user, boolean thinking, boolean matchMidi, String message, int id, List<Integer> audios) throws Exception;

    /**
     * 返回某轮对话的所有内容
     * @param id 对话id
     */
    Map<String, List<?>> context(User user, int id);

    /**
     * 删除某轮对话
     * @param id 对话id
     */
    Map<String, String> delete(User user, int id);

    /**
     * 重命名某轮对话的标题
     * @param id 对话id
     * @param name 新标题
     */
    Map<String, String> rename(User user, int id, String name);

    /**
     * 返回某轮对话的所有内容
     * @param id 对话id
     */
    Map<String, String> cancel(User user, int id) throws Exception;

    /**
     * 重新连接对话，继续流式获取生成内容
     * @param id 对话id
     */
    StreamingResponseBody reconnect(User user, int id) throws Exception;
}
