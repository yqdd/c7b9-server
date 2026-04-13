package com.ow0b.c7b9.service.converter;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ConverterService
{
    @POST("/m4a_to_midi")
    Call<ResponseBody> audioToMidi(@Body RequestBody m4a);

    @POST("/m4a_to_mp3")
    Call<ResponseBody> m4aToMp3(@Body RequestBody m4a);

    @POST("/mid_to_text")
    Call<ResponseBody> midToText(@Body RequestBody mid);

    @POST("/text_to_midi")
    Call<ResponseBody> textToMidi(@Body RequestBody text);

    @POST("/midi_llm_produce")
    Call<ResponseBody> midiLLMProduce(@Body RequestBody text);
}