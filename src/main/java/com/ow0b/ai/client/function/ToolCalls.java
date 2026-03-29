package com.ow0b.ai.client.function;

import lombok.AllArgsConstructor;

import java.util.ArrayList;

public class ToolCalls extends ArrayList<ToolCalls.ToolCall>
{
    @AllArgsConstructor
    public static class ToolCall
    {
        public final String type = "function";
        public String id;
        public Function function;
    }
    public static class Function
    {
        public String name;
        public StringBuilder arguments = new StringBuilder();
        public Function(String name, String arguments)
        {
            this.name = name;
            this.arguments.append(arguments);
        }
    }
}
