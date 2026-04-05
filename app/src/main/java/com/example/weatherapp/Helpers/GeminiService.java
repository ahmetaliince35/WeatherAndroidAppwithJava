package com.example.weatherapp.Helpers;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface GeminiService {
    // 2.5 veya 1.5 Flash sürümüne göre endpoint'i buraya yazıyoruz
    @POST("v1beta/models/gemini-2.5-flash-lite:generateContent")
    Call<GeminiModels.Response> getRecommendation(
            @Query("key") String apiKey,
            @Body GeminiModels.Request request
    );
}