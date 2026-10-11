package com.example.weatherapp.network;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class ApiClient {

    private static volatile ApiClient instance;
    private final RequestQueue requestQueue;
    private final OkHttpClient okHttpClient;

    private ApiClient(Context context) {
        this.requestQueue = Volley.newRequestQueue(context.getApplicationContext());
        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    public static ApiClient getInstance(Context context) {
        if (instance == null) {
            synchronized (ApiClient.class) {
                if (instance == null) {
                    instance = new ApiClient(context);
                }
            }
        }
        return instance;
    }

    public RequestQueue getRequestQueue() {
        return requestQueue;
    }

    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }
}
