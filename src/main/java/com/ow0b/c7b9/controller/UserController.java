package com.ow0b.c7b9.controller;

import com.ow0b.c7b9.service.database.json.Conversations;
import com.ow0b.c7b9.service.database.model.User;

import java.util.Map;

public interface UserController
{
    Map<String, String> logout(User user);

    Conversations conversations(User user);

    Map<String, Object> info(User user);
}
