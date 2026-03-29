package com.ow0b.ai.client.function;

import lombok.Builder;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MethodAndObject
{
    public final Object object;
    public final Method method;
    public final String methodName;
    public final String methodDescription;
    public final List<Parameter> para;
    public final List<ParaData> paraData;
    public final List<String> paraName;
    @Builder
    public static class ParaData
    {
        public String name;
        public String description;
        public String[] enums;
        public boolean required;
    }

    private MethodAndObject(Object object, Method method)
    {
        this.object = object;
        this.method = method;
        this.methodName = method.isAnnotationPresent(Name.class) ? method.getAnnotation(Name.class).value() : method.getName();
        this.methodDescription = method.isAnnotationPresent(Description.class) ? method.getAnnotation(Description.class).value() : method.getName();
        this.para = Arrays.asList(method.getParameters());
        this.paraData = para.stream().map(p ->
                ParaData.builder()
                        .name(p.isAnnotationPresent(Name.class) ? p.getAnnotation(Name.class).value() : p.getName())
                        .description(p.isAnnotationPresent(Description.class) ? p.getAnnotation(Description.class).value() : null)
                        .enums(p.isAnnotationPresent(Enums.class) ? p.getAnnotation(Enums.class).value() : null)
                        .required(p.isAnnotationPresent(Required.class))
                        .build())
                .toList();
        this.paraName = paraData.stream().map(d -> d.name).toList();
    }

    public static List<MethodAndObject> get(Object object)
    {
        ArrayList<MethodAndObject> list = new ArrayList<>();
        for(Method m : object.getClass().getDeclaredMethods())
        {
            if(m.isAnnotationPresent(Description.class))
            {
                m.setAccessible(true);
                list.add(new MethodAndObject(object, m));
            }
        }
        return list;
    }
    public static List<MethodAndObject> getStatic(Class<?> clazz)
    {
        ArrayList<MethodAndObject> list = new ArrayList<>();
        for(Method m : clazz.getDeclaredMethods())
        {
            if(m.isAnnotationPresent(Description.class) && Modifier.isStatic(m.getModifiers()))
            {
                m.setAccessible(true);
                list.add(new MethodAndObject(null, m));
            }
        }
        return list;
    }

    @Override
    public boolean equals(Object obj)
    {
        if(obj instanceof MethodAndObject mao) return mao.method.equals(method);
        else return super.equals(obj);
    }
    @Override
    public int hashCode()
    {
        return object.hashCode() * method.hashCode();
    }
}
