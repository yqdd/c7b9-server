package com.ow0b.ai.client.function;

import com.google.gson.*;
import com.ow0b.ai.client.abstracted.AiClient;
import com.ow0b.ai.client.abstracted.Provider;
import com.ow0b.ai.client.message.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class ToolMethods
{
    private static final Gson gson = new Gson();
    private final AiClient client;
    private final ArrayList<MethodAndObject> list = new ArrayList<>();
    public void add(Class<?> clazz)
    {
        list.addAll(MethodAndObject.getStatic(clazz));
    }
    public void add(Object obj)
    {
        list.addAll(MethodAndObject.get(obj));
    }
    public int size()
    {
        return list.size();
    }


    public JsonArray toolsJson()
    {
        JsonArray array = new JsonArray();
        Provider provider = client.getProvider();
        list.forEach(mao ->
        {
            JsonObject object = new JsonObject(),
                    function = new JsonObject(),
                    parameters = new JsonObject(),
                    properties = new JsonObject();
            JsonArray required = new JsonArray();
            //生成function键值
            function.addProperty("name", mao.methodName);
            function.addProperty("description", mao.methodDescription);
            //生成parameters键值
            parameters.addProperty("type", "object");
            //生成properties键值
            for(Parameter parameter : mao.method.getParameters())
            {
                MethodAndObject.ParaData data = mao.paraData.get(mao.para.indexOf(parameter));
                //properties中每个键对应一个方法参数
                JsonObject paraJson = new JsonObject();
                paraJson.addProperty("type", parameterType(parameter.getType()));
                if(data.description != null)
                {
                    if(provider == Provider.LOCAL) throw new RuntimeException("本地模型方法参数不支持@Description");
                    else paraJson.addProperty("description", data.description);
                }
                if(data.enums != null)
                    paraJson.add("enum", JsonParser.parseString(gson.toJson(data.enums)));
                properties.add(data.name, paraJson);
                //加载required参数数据
                if(data.required) required.add(data.name);
            }
            parameters.add("properties", properties);
            parameters.add("required", required);
            function.add("parameters", parameters);

            //封装function call方法的jsonObject，添加进array
            object.addProperty("type", "function");
            object.add("function", function);
            array.add(object);
        });
        return array;
    }
    public MethodResult invoke(String name, String arguments) throws JsonSyntaxException
    {
        //编号\": \"94\"}null
        MethodAndObject mao = functionName(name);
        if(arguments.contains("\\")) arguments = arguments.replaceAll("\\\\", "");
        if(!arguments.startsWith("{\"")) arguments = "{\"" + arguments;
        if(!arguments.startsWith("{")) arguments = "{" + arguments;
        if(arguments.endsWith("null")) arguments = arguments.substring(0, arguments.indexOf("null"));
        if(!arguments.contains(":")) arguments = arguments.substring(0, arguments.indexOf("\"")) + "\"id\":" + arguments.substring(arguments.indexOf("\""));
        log.info(arguments);

        Map<String, JsonElement> json = JsonParser.parseString(arguments).getAsJsonObject().asMap();
        //准备方法参数
        Object[] para = new Object[mao.method.getParameters().length];
        json.forEach((k, v) ->
        {
            int i = mao.paraName.indexOf(k);
            if(i == -1) i = mao.methodName.indexOf("id");
            para[i] = parameterValue(mao.para.get(i).getType(), v);
        });
        //执行方法并返回结果
        try
        {
            Object obj = mao.method.invoke(mao.object, para);
            if(obj instanceof String str) return MethodResult.builder().content(str).build();
            if(obj instanceof MethodResult result) return result;
            throw new RuntimeException("不支持的类型：" + obj.getClass());
        }
        catch (IllegalAccessException | InvocationTargetException e)
        {
            throw new RuntimeException(e);
        }
    }

    private MethodAndObject functionName(String name)
    {
        return list.stream().filter(m -> m.methodName.equals(name)).findFirst().orElse(null);
    }
    private String parameterType(Class<?> type)
    {
        if(type == String.class) return "string";
        if(type == Integer.class || type == int.class) return "number";
        if(type == Boolean.class || type == boolean.class) return "boolean";
        throw new RuntimeException("不支持的function call方法参数格式：" + type);
    }
    private Object parameterValue(Class<?> type, JsonElement ele)
    {
        Object value;
        if(type.isAssignableFrom(Float.class))
            value = ele.getAsFloat();
        else if(type.isAssignableFrom(Double.class))
            value = ele.getAsFloat();
        else if(type.isAssignableFrom(Integer.class))
            value = ele.getAsInt();
        else if(type.isAssignableFrom(Long.class))
            value = ele.getAsLong();
        else if(type.isAssignableFrom(String.class))
            value = ele.getAsString();
        else if(type.isAssignableFrom(Boolean.class))
            value = ele.getAsBoolean();
        else throw new RuntimeException("不支持的json参数类型：" + ele.toString());
        return value;
    }
}
