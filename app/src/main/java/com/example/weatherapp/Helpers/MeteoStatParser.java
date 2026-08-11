package com.example.weatherapp.Helpers;

import com.example.weatherapp.BuildConfig;
import com.google.gson.annotations.SerializedName;

import java.util.List;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public class MeteoStatParser {

    private static final String BASE_URL =
            "https://meteostat.p.rapidapi.com/";

    private static final String HOST =
            "meteostat.p.rapidapi.com";

    private static final String API_KEY = BuildConfig.METEOSTAT_KEY;

    private final MeteostatApi api;

    public MeteoStatParser() {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(MeteostatApi.class);
    }

    public void getDailyWeather(
            String stationId,
            String start,
            String end,
            retrofit2.Callback<DailyWeatherResponse> callback
    ) {

        api.getDailyWeather(
                API_KEY,
                HOST,
                stationId,
                start,
                end
        ).enqueue(callback);
    }

    private interface MeteostatApi {

        @GET("stations/daily")
        Call<DailyWeatherResponse> getDailyWeather(
                @Header("x-rapidapi-key") String apiKey,
                @Header("x-rapidapi-host") String host,
                @Query("station") String stationId,
                @Query("start") String start,
                @Query("end") String end
        );
    }

    public static class DailyWeatherResponse {

        @SerializedName("data")
        public List<DailyWeather> data;
    }

    public static class DailyWeather {

        @SerializedName("date")
        public String date;

        @SerializedName("prcp")
        public Double precipitation;

        @SerializedName("pres")
        public Double pressure;

        @SerializedName("tmin")
        public Double minTemperature;

        @SerializedName("tmax")
        public Double maxTemperature;

        @SerializedName("wspd")
        public Double windSpeed;
    }
}