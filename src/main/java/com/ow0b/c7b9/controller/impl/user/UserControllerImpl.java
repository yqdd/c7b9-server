package com.ow0b.c7b9.controller.impl.user;

import com.ow0b.c7b9.annotation.LoginRequired;
import com.ow0b.c7b9.controller.UserController;
import com.ow0b.c7b9.service.ChatService;
import com.ow0b.c7b9.service.UserService;
import com.ow0b.c7b9.service.database.json.Conversations;
import com.ow0b.c7b9.service.database.model.User;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@LoginRequired
@RestController
public class UserControllerImpl implements UserController
{
    @Setter(onMethod_ = @Autowired)
    private UserService userService;
    @Setter(onMethod_ = @Autowired)
    private ChatService chatService;

    @Override
    @RequestMapping("/logout")
    public Map<String, String> logout(User user)
    {
        userService.logout(user.getUid());
        return Map.of("info", "已退出登录");
    }

    @Override
    @RequestMapping("/conversations")
    public Conversations conversations(User user)
    {
        return chatService.getConversations(user.getUid());
    }

    @Override
    @RequestMapping("/user/info")
    public Map<String, Object> info(User user)
    {
        return Map.of("uid", user.getUid(), "name", user.getUsername(), "token", user.getToken());
    }

    /*
    @ExceptionHandler({AuthNotFoundException.class, AuthErrorException.class})
    public Map<String, String> authError()
    {
        return Map.of("info", "当前还未登录");
    }
     */
}
