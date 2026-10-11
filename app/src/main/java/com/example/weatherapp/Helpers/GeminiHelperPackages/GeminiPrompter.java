package com.example.weatherapp.Helpers.GeminiHelperPackages;

import android.util.Log;

import com.example.weatherapp.BuildConfig;

import okhttp3.OkHttpClient;
import java.util.concurrent.TimeUnit;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class GeminiPrompter {
    private String TAG="AIIntegration";
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/";
    private final GeminiService service;
    private final String API_KEY = BuildConfig.GEMINI_API_KEY;

    public GeminiPrompter() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(7, TimeUnit.SECONDS)
                .readTimeout(7, TimeUnit.SECONDS)
                .writeTimeout(7, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        service = retrofit.create(GeminiService.class);
    }

    public String getWeatherAdvice(String city, double temp, String desc,String icon) {
        String dayOrnight =icon.endsWith("d")? "gündüz" : "gece";
        // AI'ya ne sormak istediğini burada kurguluyoruz (Prompt)
        String prompt = String.format(
                "%s şehrinde  şu an %s vakti ve  hava  %.1f derece ve %s. " +
                        "Bana bu şehrin kültürüne uygun olacak şekilde havayı yorumlayıp o ana göre 50 kelimeyi geçmeyecek bir öneri/tavsiye/kıyafet önerisi ver.",
                city,dayOrnight, temp, desc
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