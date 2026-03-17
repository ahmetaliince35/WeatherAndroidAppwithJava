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

import com.example.weatherapp.Helpers.WeatherJsonAPI;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class Forecastactivity extends AppCompatActivity {

    private TextView textViewCityTitle;
    private TextView textViewLastUpdate;
    private RecyclerView recyclerViewForecast;
    private ProgressBar progressBar;
    private ForecastAdapter adapter;
    private List<ForecastItem> forecastList;
    private LinearLayout root;
    private boolean isNewSearch;
    private boolean isSaveLocation;
    PreferencesManager preferencesManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forecastactivity);

        // ActionBar'da geri butonu
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("5 Günlük Tahmin");
        }
        preferencesManager=PreferencesManager.getInstance(this);
        // View'ları bağlama
        textViewCityTitle = findViewById(R.id.textViewCityTitle);
        recyclerViewForecast = findViewById(R.id.recyclerViewForecast);
        textViewLastUpdate=findViewById(R.id.textViewLastUpdate);
        progressBar = findViewById(R.id.progressBar);
        root = findViewById(R.id.root);
        // RecyclerView kurulumu
        recyclerViewForecast.setLayoutManager(new LinearLayoutManager(this));
        forecastList = new ArrayList<>();
        adapter = new ForecastAdapter(this, forecastList);
        recyclerViewForecast.setAdapter(adapter);
        long lastUpdate=System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM HH:mm", new Locale("tr", "TR"));
        String updateTime = sdf.format(new Date(lastUpdate));
        textViewLastUpdate.setText("Son güncelleme: " + updateTime);
        // Intent'ten şehir adını al
        String cityName = getIntent().getStringExtra("CITY_NAME");
        int bgRes = getIntent().getIntExtra("background-res", R.drawable.background);
        isNewSearch = getIntent().getBooleanExtra("isNewSearch",false);
        isSaveLocation=getIntent().getBooleanExtra("isSaveLocation",false);
        root.setBackgroundResource(bgRes);
        if (cityName != null) {
            textViewCityTitle.setText(cityName + " - 5 Günlük Tahmin");
            getForecastData(cityName,isNewSearch);
        } else {
            Toast.makeText(this, "Şehir bilgisi alınamadı", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    

    private void getForecastData(String city,boolean isNewSearch) {
        progressBar.setVisibility(View.VISIBLE);
        WeatherJsonAPI dailyData = new WeatherJsonAPI(this, URL_API.ForecastURL);
        if (!isNewSearch) {
            List<ForecastItem> cached = dailyData.getCachedDailyForecast();
            if (cached != null && !cached.isEmpty()) {
                forecastList.clear();
                forecastList.addAll(cached);
                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
                return;
            }
        }
        else
        {
            dailyData.getDailyForecast(city, new WeatherJsonAPI.ForecastCallback() {
                @Override
                public void onSuccess(List<ForecastItem> newforecastList,String json) {
                    progressBar.setVisibility(View.GONE);
                    forecastList.clear();             // Adapter’in kullandığı listeyi temizle
                    forecastList.addAll(newforecastList); // Yeni verileri ekle
                    adapter.notifyDataSetChanged(); // veya adapter.notifyDataSetChanged() ile güncelle
                    if(isSaveLocation) preferencesManager.saveDailyForecastJson(json);
                }

                @Override
                public void onError(String error) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(Forecastactivity.this, error, Toast.LENGTH_SHORT).show();
                }
            });

        }
        ;

    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}