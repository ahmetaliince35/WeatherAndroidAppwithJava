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
import java.util.ArrayList;
import java.util.List;


public class Forecastactivity extends AppCompatActivity {

    private TextView textViewCityTitle;
    private RecyclerView recyclerViewForecast;
    private ProgressBar progressBar;
    private ForecastAdapter adapter;
    private List<ForecastItem> forecastList;
    private LinearLayout root;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forecastactivity);

        // ActionBar'da geri butonu
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("5 Günlük Tahmin");
        }

        // View'ları bağlama
        textViewCityTitle = findViewById(R.id.textViewCityTitle);
        recyclerViewForecast = findViewById(R.id.recyclerViewForecast);
        progressBar = findViewById(R.id.progressBar);
        root = findViewById(R.id.root);
        // RecyclerView kurulumu
        recyclerViewForecast.setLayoutManager(new LinearLayoutManager(this));
        forecastList = new ArrayList<>();
        adapter = new ForecastAdapter(this, forecastList);
        recyclerViewForecast.setAdapter(adapter);

        // Intent'ten şehir adını al
        String cityName = getIntent().getStringExtra("CITY_NAME");
        int bgRes = getIntent().getIntExtra("background-res", R.drawable.background);
        root.setBackgroundResource(bgRes);
        if (cityName != null) {
            textViewCityTitle.setText(cityName + " - 5 Günlük Tahmin");
            getForecastData(cityName);
        } else {
            Toast.makeText(this, "Şehir bilgisi alınamadı", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void getForecastData(String city) {
        progressBar.setVisibility(View.VISIBLE);
        WeatherJsonAPI dailyData = new WeatherJsonAPI(this, URL_API.ForecastURL);
        dailyData.getDailyForecast(city, new WeatherJsonAPI.ForecastCallback() {
            @Override
            public void onSuccess(List<ForecastItem> newforecastList) {
                progressBar.setVisibility(View.GONE);
                forecastList.clear();             // Adapter’in kullandığı listeyi temizle
                forecastList.addAll(newforecastList); // Yeni verileri ekle
                adapter.notifyDataSetChanged(); // veya adapter.notifyDataSetChanged() ile güncelle
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(Forecastactivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}