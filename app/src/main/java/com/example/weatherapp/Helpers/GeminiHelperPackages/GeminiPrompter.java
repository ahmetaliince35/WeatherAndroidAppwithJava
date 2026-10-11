package com.example.weatherapp.Helpers.GeminiHelperPackages;

import android.content.Context;
import android.util.Log;

import com.example.weatherapp.BuildConfig;
import com.example.weatherapp.network.ApiClient;

import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class GeminiPrompter {
    private static final String TAG = "GeminiPrompter";
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/";
    private final GeminiService service;
    private final String API_KEY = BuildConfig.GEMINI_API_KEY;

    public GeminiPrompter() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        service = retrofit.create(GeminiService.class);
    }

    public GeminiPrompter(Context context) {
        OkHttpClient client = ApiClient.getInstance(context).getOkHttpClient();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        service = retrofit.create(GeminiService.class);
    }

    public String getWeatherAdvice(String city, double temp, String desc, String icon) {
        if (API_KEY == null || API_KEY.trim().isEmpty()) {
            return "Gemini API anahtarı tanımlanmamış.";
        }

        String safeCity = (city != null && !city.trim().isEmpty()) ? city.trim() : "Bölgeniz";
        String safeDesc = (desc != null && !desc.trim().isEmpty()) ? desc.trim() : "mevcut hava koşulları";
        String timeOfDay = determineTimeOfDay(icon);

        String prompt = String.format(Locale.getDefault(),
                "Sen samimi bir hava durumu asistanısın.\n" +
                "Konum: %s\n" +
                "Zaman: %s\n" +
                "Sıcaklık: %.1f°C\n" +
                "Hava Durumu: %s\n\n" +
                "Görev: Kullanıcıya bu hava koşullarına uygun, giyim ve günün akışına yönelik pratik bir tavsiye ver.\n" +
                "Kurallar:\n" +
                "- Kesinlikle selamlama veya giriş cümlesi (örneğin 'Merhaba', 'Tabii ki', 'İşte önerim') KULLANMA. Doğrudan tavsiyeyle başla.\n" +
                "- Markdown başlığı veya liste imi kullanma, tek bir akıcı metin olsun.\n" +
                "- En fazla 40 kelime olsun, net ve öz olsun.\n" +
                "- Sağlayıcı veya teknik detaylardan asla bahsetme.",
                safeCity, timeOfDay, temp, safeDesc
        );

        try {
            Response<GeminiModels.Response> response = service.getRecommendation(API_KEY, new GeminiModels.Request(prompt)).execute();
            if (response.isSuccessful() && response.body() != null) {
                String aiText = response.body().getAiText();
                return cleanOutput(aiText);
            } else {
                Log.e(TAG, "Gemini API Hatası! Kod: " + response.code());
                return "Kullanım limitine ulaştık, fakirliğin gözü kör olsun :). Camdan bakarak havaya uygun giyinebilirsiniz :D.";
            }
        } catch (Exception e) {
            Log.e(TAG, "Gemini isteği başarısız: " + e.getMessage());
            return "Ups! Bir şeyler ters gitti. Daha sonra tekrar deneyin. ";
        }
    }

    private String determineTimeOfDay(String icon) {
        if (icon != null) {
            String lower = icon.toLowerCase(Locale.ROOT);
            if (lower.endsWith("d") || lower.contains("day") || lower.contains("sun")) {
                return "gündüz";
            }
            if (lower.endsWith("n") || lower.contains("night") || lower.contains("moon")) {
                return "gece";
            }
        }
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return (hour >= 6 && hour < 20) ? "gündüz" : "gece";
    }

    private String cleanOutput(String text) {
        if (text == null) return "";
        String cleaned = text.trim();
        // Markdown kalınlık işaretlerini temizle
        cleaned = cleaned.replace("**", "").replace("*", "");
        // Tırnak işaretlerini temizle
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length() > 2) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }
        // Olası selamlama başlangıçlarını temizle
        if (cleaned.toLowerCase(Locale.ROOT).startsWith("merhaba")) {
            int commaIdx = cleaned.indexOf(",");
            if (commaIdx != -1 && commaIdx < 15) {
                cleaned = cleaned.substring(commaIdx + 1).trim();
            } else {
                int dotIdx = cleaned.indexOf(".");
                if (dotIdx != -1 && dotIdx < 20) {
                    cleaned = cleaned.substring(dotIdx + 1).trim();
                }
            }
        }
        return cleaned;
    }
}