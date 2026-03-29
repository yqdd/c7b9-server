package com.ow0b.c7b9;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.ow0b.ai.client.ChatClient;
import com.ow0b.ai.client.DashscopeAudioClient;
import com.ow0b.ai.client.V1Client;
import com.ow0b.midi.library.Library;
import com.ow0b.midi.library.MidiSWLibrary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import java.io.File;
import java.util.Properties;

@Slf4j
@SpringBootApplication
public class C7b9ServerApplication
{
    @Bean
    public Gson gson()
    {
        return new GsonBuilder().serializeNulls().create();
    }
    @Bean
    public Retrofit retrofit(@Value("${converter}") String url)
    {
        return new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();
    }
    @Bean
    public Library library()
    {
        return new MidiSWLibrary(new File("midis"));
    }
    @Bean
    @Scope("prototype")
    public ChatClient chatClient(@Value("${ai.chat.api.url}") String url,
                                 @Value("${ai.chat.api.key}") String key,
                                 @Value("${ai.chat.api.model}") String model)
    {
        return new ChatClient(url, key, model);
    }
    @Bean
    @Scope("prototype")
    public V1Client v1Client(@Value("${ai.rwkv.api.url}") String url,
                             @Value("${ai.rwkv.api.model}") String model)
    {
        return new V1Client(url, "", model);
    }
    @Bean
    @Scope("prototype")
    public DashscopeAudioClient dashscopeAudioClient(@Value("${ai.dashscope.api.url}") String url,
                                                     @Value("${ai.dashscope.api.key}") String key,
                                                     @Value("${ai.dashscope.api.model}") String model)
    {
        return new DashscopeAudioClient(url, key, model);
    }

    public static void main(String[] args) throws JSchException
    {
        SpringApplication.run(C7b9ServerApplication.class, args);

        JSch jsch = new JSch();
        String ip = "", password = "";
        Session session = jsch.getSession("ubuntu", ip, 22);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        String remoteBindAddress = "0.0.0.0";  // 远程服务器监听地址
        int remotePort = 6415;                 // 远程服务器端口
        String localHost = "127.0.0.1";        // 本地主机
        int localPort = 8080;                  // 本地端口
        session.setPortForwardingR(remoteBindAddress, remotePort, localHost, localPort);
    }
}
