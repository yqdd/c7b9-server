package com.ow0b.ai.client.abstracted;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Slf4j
@RequiredArgsConstructor
public class ApiConnection implements AutoCloseable
{
    public final Reader reader;
    //public final Writer writer;

    public void close() throws Exception
    {
        reader.close();
        //writer.close();
    }
    public String data()
    {
        try
        {
            StringBuilder builder = new StringBuilder();
            int c;
            while((c = reader.read()) != -1 && !Thread.currentThread().isInterrupted())
            {
                if((char) c == '\n')
                {
                    String line = builder.toString().replaceAll("\r", "").replaceAll("\n", "");
                    String json = builder.toString().replace("data: ", "");
                    try
                    {
                        JsonParser.parseString(json);
                        if(!line.isEmpty() && !json.startsWith("[DONE]"))
                            return json;
                    }
                    catch (JsonSyntaxException e)
                    {
                        System.out.println("无法解析内容：" + line);
                        builder.delete(0, builder.length() - 1);
                    }
                }
                builder.append((char) c);
            }
            return null;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
    public String body()
    {
        try
        {
            StringBuilder builder = new StringBuilder();
            int c;
            while((c = reader.read()) != -1) builder.append((char) c);
            return builder.toString();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static ApiConnection create(AiClient client, String body)
    {
        HttpURLConnection connection = null;
        try
        {
            URL url = new URL(client.apiUrl);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            if(client.apiKey != null) connection.setRequestProperty("Authorization", "Bearer " + client.apiKey);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent","Mozilla/5.0 ( compatible ) ");
            connection.setRequestProperty("Accept","*/*");
            connection.setDoOutput(true);
            try(PrintStream printer = new PrintStream(connection.getOutputStream(), true, StandardCharsets.UTF_8))
            {
                printer.println(body);
                printer.flush();
            }
            return new ApiConnection(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
        }
        catch (IOException e)
        {
            if(connection != null)
            {
                try
                {
                    if(body != null) System.err.println(body);
                    if(connection.getErrorStream() != null) System.err.println(new String(connection.getErrorStream().readAllBytes()));
                }
                catch (IOException ex) { throw new RuntimeException(ex); }
            }
            throw new RuntimeException(e);
        }
    }
    public static void error(JsonObject response)
    {
        if(response.get("error") != null)
        {
            JsonObject error = response.get("error").getAsJsonObject();
            ApiException exception = new ApiException(error.get("message").getAsString());
            exception.type = error.get("type").getAsString();
            exception.code = error.get("code").getAsString();
            throw exception;
        }
    }
}
