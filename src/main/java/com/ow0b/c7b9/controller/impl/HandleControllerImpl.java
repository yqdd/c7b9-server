package com.ow0b.c7b9.controller.impl;

import jakarta.servlet.http.HttpServletResponse;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.DispatcherServlet;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/// 这个换成tcp的ClientSocket（调其他path的部分参考下）
@Slf4j
//@Controller
@Deprecated
public class HandleControllerImpl
{
    @Setter(onMethod_ = @Autowired)
    DispatcherServlet dispatcher;

    @RequestMapping("/handle")
    public void handle(@RequestHeader Map<String, String> headers,
                       @RequestHeader("Uri") String uriStr,
                       @RequestBody Optional<byte[]> body,
                       HttpMethod method,
                       HttpServletResponse resp) throws Exception
    {
        URI uri = new URI(uriStr);
        dispatcher.service(new MockHttpServletRequest(method.name(), uri.getPath())
        {{
            body.ifPresent(this::setContent);
            headers.forEach(this::addHeader);
            //Spring的MockHttpServletRequest只会解析路径（会把url参数当成路径），url参数需要手动解析
            Map<String, ?> parameters = Arrays.stream(uri.getQuery().split("&"))
                    .map(s ->
                    {
                        String[] kv = s.split("=");
                        return Map.entry(URLDecoder.decode(kv[0], StandardCharsets.UTF_8), URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
                    })
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            setParameters(parameters);
        }}, resp);
    }
}
