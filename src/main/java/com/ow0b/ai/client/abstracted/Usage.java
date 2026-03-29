package com.ow0b.ai.client.abstracted;

import com.google.gson.JsonObject;

public record Usage(int promptTokens, int completionTokens, int totalTokens)
{
    public static Usage of(JsonObject response)
    {
        ApiConnection.error(response);
        JsonObject usage = response.getAsJsonObject("usage");
        return new Usage(usage.get("prompt_tokens").getAsInt(),
                usage.get("completion_tokens").getAsInt(),
                usage.get("total_tokens").getAsInt());
    }
}
