package com.ow0b.c7b9.controller.impl;

import com.ow0b.c7b9.controller.ServerController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServerControllerImpl implements ServerController
{
    @Override
    @RequestMapping("/alive")
    public ResponseEntity<Void> alive()
    {
        return ResponseEntity.ok().build();
    }
}
