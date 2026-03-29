package com.ow0b.c7b9;

import com.google.gson.*;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class ChatWriter extends BufferedWriter
{
    private final Gson gson = new GsonBuilder().serializeNulls().create();
    private final CopyOnWriteArraySet<Writer> writers = new CopyOnWriteArraySet<>();
    private final StringBuffer content = new StringBuffer();

    public ChatWriter(@NotNull Writer out)
    {
        super(out);
        writers.add(out);
    }
    public void addMirror(Writer out) throws IOException
    {
        out.write(content.toString());
        writers.add(out);
    }

    public record Datum(String key, Object value) {}
    public void sendStreamJson(String type, Datum... data) throws IOException
    {
        JsonObject json = new JsonObject();
        json.addProperty("type", type);
        for(Datum datum : data)
        {
            if(datum.value != null)
            {
                if(datum.value instanceof String v) json.addProperty(datum.key, v);
                else if(datum.value instanceof Number v) json.addProperty(datum.key, v);
                else if(datum.value instanceof Boolean v) json.addProperty(datum.key, v);
                else if(datum.value instanceof Character v) json.addProperty(datum.key, v);
                else if(datum.value instanceof JsonElement v) json.add(datum.key, v);
                else json.add(datum.key, JsonParser.parseString(gson.toJson(datum.value)));
            }
        }
        for(Writer writer : writers)
        {
            writer.write(json.toString());
            writer.write("\n\n");
            try { writer.flush(); }
            catch (IOException e) { writers.remove(writer); }
        }
    }
    public void sendStreamJson(String type, String key, Object value) throws IOException
    {
        sendStreamJson(type, new Datum(key, value));
    }

    private interface Act
    {
        void accept(Writer writer) throws IOException;
    }
    private void multiAct(Act act)
    {
        for(Writer writer : Set.copyOf(writers))
        {
            try { act.accept(writer); }
            catch (IOException ex) { writers.remove(writer); }
        }
    }

    @Override
    public void write(@NotNull String str)
    {
        multiAct(writer -> writer.write(str));
        content.append(str);
    }
    @Override
    public void flush()
    {
        multiAct(Writer::flush);
    }
    @Override
    public void close() throws IOException
    {
        multiAct(Writer::close);
        super.close();
    }
}