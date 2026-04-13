package com.ow0b.c7b9.controller.impl.user;

import com.ow0b.c7b9.annotation.LoginRequired;
import com.ow0b.c7b9.controller.EnvironmentAdvice;
import com.ow0b.c7b9.service.UserService;
import com.ow0b.c7b9.service.database.model.User;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice(annotations = {LoginRequired.class})
public class EnvironmentAdviceImpl implements EnvironmentAdvice
{
    @Setter(onMethod_ = @Autowired)
    private UserService userService;

    @Override
    @ModelAttribute("user")
    public User user(@RequestHeader Map<String, String> headers)
    {
        //header参数名会自动小写
        if(!headers.containsKey("permit")) throw new AuthNotFoundException("未找到请求头参数：permit");
        if(!headers.containsKey("random")) throw new AuthNotFoundException("未找到请求头参数：random");

        return userService.get(new UserService.Authorization(
                headers.getOrDefault("permit", null),
                headers.getOrDefault("random", null)));
    }

    @ExceptionHandler(AuthNotFoundException.class)
    public Map<String, String> keyNotFound(AuthNotFoundException e)
    {
        return Map.of("error", e.getMessage());
    }
    @ExceptionHandler(AuthErrorException.class)
    public Map<String, String> authError()
    {
        return Map.of("error", "permit错误");
    }
}
