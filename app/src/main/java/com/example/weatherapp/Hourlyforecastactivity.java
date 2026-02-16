package com.example.weatherapp;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Hourlyforecastactivity extends AppCompatActivity {

    private RecyclerView recyclerViewHourly;
    private ForecastAdapter adapter;
    private List<ForecastItem> hourlyList;
    private ProgressBar progressBar;
    private TextView textViewTitle;
    private TextView textViewDataSource;

    private String cityName;
    private LinearLayout root;

    // OpenWeatherMap API Key - MainActivity ile aynı olmalı
    private static final String API_KEY = URL_API.API_KEY;
    private static final String BASE_URL = URL_API.ForecastURL;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hourlyforecastactivity);
        // ActionBar'da geri butonu
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("5 Günlük Tahmin");
        }

        recyclerViewHourly = findViewById(R.id.recyclerViewHourly);
        progressBar = findViewById(R.id.progressBar);
        textViewTitle = findViewById(R.id.textViewTitle);
        root= findViewById(R.id.root);
        // Intent'ten şehir adını al
        cityName = getIntent().getStringExtra("CITY_NAME");
        int bgRes = getIntent().getIntExtra("background-res",R.drawable.background);
        root.setBackgroundResource(bgRes);
        if (cityName == null || cityName.isEmpty()) {
            Toast.makeText(this, "Şehir bilgisi bulunamadı", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // View'ları bağla

        // Başlığı ayarla
        textViewTitle.setText(cityName + " - 48 Saatlik Tahmin");

        // RecyclerView ayarları
        hourlyList = new ArrayList<>();
        adapter = new ForecastAdapter(this, hourlyList);
        recyclerViewHourly.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewHourly.setAdapter(adapter);

        // Verileri çek
        getHourlyForecast();
    }

    private void getHourlyForecast() {
        progressBar.setVisibility(View.VISIBLE);

        // OpenWeatherMap'in 5 günlük/3 saatlik tahmin API'si
        // 48 saat = 16 veri noktası (3 saatte bir)
        String url = BASE_URL + cityName + "&appid=" + API_KEY + "&units=metric&lang=tr&cnt=24";

        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        progressBar.setVisibility(View.GONE);
                        parseHourlyData(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(Hourlyforecastactivity.this,
                                "Veri alınırken hata oluştu: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );

        queue.add(jsonObjectRequest);
    }

    private void parseHourlyData(JSONObject response) {
        try {
            JSONArray list = response.getJSONArray("list");

            for (int i = 0; i < list.length(); i++) {
                double rain = 0;
                double rainprob = 0;
                JSONObject hourData = list.getJSONObject(i);
                long timestamp= hourData.getLong("dt");
                String dateHour=formatDate(timestamp);


                // Hava durumu bilgileri
                JSONObject main = hourData.getJSONObject("main");
                double temp = main.getDouble("temp");
                int humidity = main.getInt("humidity");

                JSONObject weather = hourData.getJSONArray("weather").getJSONObject(0);
                String description = weather.getString("description");
                String icon = weather.getString("icon");

                double tempMin = main.getDouble("temp_min");
                double tempMax = main.getDouble("temp_max");
                JSONObject wind = hourData.getJSONObject("wind");
                double windSpeed = wind.getDouble("speed");


                rainprob = hourData.getDouble("pop");

                if(hourData.has("rain"))
                {
                    JSONObject rainobj=hourData.getJSONObject("rain");

                    rain=rainobj.getDouble("3h");


                }
                if(hourData.has("snow"))
                {
                    JSONObject snowobj=hourData.getJSONObject(("snow"));
                    rain=snowobj.getDouble("3h");
                }
                // ForecastItem oluştur ve listeye ekle
                ForecastItem forecastItem = new ForecastItem(
                        dateHour,
                        temp,
                        description,
                        icon,
                        humidity,
                        rainprob*100,
                        rain,
                        windSpeed*3.6,
                        tempMin,
                        tempMax
                );
                hourlyList.add(forecastItem);
            }

            adapter.notifyDataSetChanged();

            if (hourlyList.isEmpty()) {
                Toast.makeText(this, "Veri bulunamadı", Toast.LENGTH_SHORT).show();
            }

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Veri işlenirken hata oluştu", Toast.LENGTH_SHORT).show();
        }
    }

    private String formatDate(long timestamp) {
        Date date = new Date(timestamp * 1000);
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM HH.mm - EE", new Locale("tr", "TR"));
        return sdf.format(date);
    }
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}