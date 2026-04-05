package com.example.weatherapp.Helpers;

import android.util.Log;

import com.example.weatherapp.BuildConfig;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class GeminiPrompter {
    private String TAG="AIIntegration";
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/";
    private final GeminiService service;
    private final String API_KEY = BuildConfig.GEMINI_API_KEY;

    public GeminiPrompter() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        service = retrofit.create(GeminiService.class);
    }

    public String getWeatherAdvice(String city, double temp, String desc) {
        // AI'ya ne sormak istediğini burada kurguluyoruz (Prompt)
        String prompt = String.format(
                "%s şehrinde hava şu an %.1f derece ve %s. " +
                        "Bana Türkçe, samimi ve kısa (max 20 kelime) bir giyim veya aktivite önerisi verir misin?",
                city, temp, desc
        );

        try {
            Response<GeminiModels.Response> response = service.getRecommendation(API_KEY, new GeminiModels.Request(prompt)).execute();
            if (response.isSuccessful() && response.body() != null) {
                return response.body().getAiText();
            } else {
                Log.e(TAG, "Gemini API Hatası! Kod: " + response.code() );
                return "Günlük yapay zeka kotamızı doldurduk. Fakirliğin gözü kör olsun. :)";
            }
        } catch (Exception e) {
            Log.e(TAG,"Hata var "+e);
            e.printStackTrace();
            return "Ups! Bir şeyler ters gitti!!";
        }
    }
}