package com.ow0b.c7b9.controller.impl.user;

import com.ow0b.c7b9.controller.LoginController;
import com.ow0b.c7b9.service.UserService;
import com.ow0b.c7b9.service.exception.AuthorizationException;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class LoginControllerImpl implements LoginController
{
    @Setter(onMethod_ = @Autowired)
    private UserService userService;

    @RequestMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestParam("username") String username,
                                     @RequestParam("password") String password)
    {
        try
        {
            UserService.Authorization auth = userService.login(username, password);
            return ResponseEntity.ok(Map.of("info", "登录成功", "permit", auth.permit(), "random", auth.random()));
        }
        catch (AuthorizationException e)
        {
            return ResponseEntity.ok(Map.of("error", "登录失败，" + e.getMessage()));
        }
    }

    @RequestMapping("/registry")
    public ResponseEntity<Map<String, String>> registry(@RequestParam("username") String username,
                                        @RequestParam("password") String password)
    {
        if(userService.registry(username, password)) return ResponseEntity.ok(Map.of("info", "注册成功"));
        else return ResponseEntity.badRequest().body(Map.of("info", "用户已存在"));
    }
}
