package com.example.weatherapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weatherapp.Helpers.WeatherJsonAPI;

import java.util.ArrayList;
import java.util.List;

public class Hourlyforecastactivity extends AppCompatActivity {

    private RecyclerView recyclerViewHourly;
    private ForecastAdapter adapter;
    private List<ForecastItem> hourlyList;
    private ProgressBar progressBar;
    private TextView textViewTitle;
    private boolean isNewSearch;
    private boolean isSaveLocation;
    private String cityName;
    private LinearLayout root;
    private int backGroundRes;
PreferencesManager preferencesManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hourlyforecastactivity);

        initialVariables();
        getIntents();


        textViewTitle.setText(cityName + " - 72 Saatlik Tahmin");
        root.setBackgroundResource(backGroundRes);

        // RecyclerView ayarları
        hourlyList = new ArrayList<>();
        adapter = new ForecastAdapter(this, hourlyList);
        recyclerViewHourly.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewHourly.setAdapter(adapter);

        getHourlyForecast(isNewSearch);
    }
    private void getIntents()
    {
        cityName = getIntent().getStringExtra("CITY_NAME");
        if (TextUtils.isEmpty(cityName)) {
            cityName = preferencesManager.getSavedCityName();
        }
        backGroundRes = getIntent().getIntExtra("background-res",R.drawable.background);
        isNewSearch=getIntent().getBooleanExtra("isNewSearch",false);
        isSaveLocation=getIntent().getBooleanExtra("isSaveLocation",false);
    }
private void initialVariables()
{
    preferencesManager=PreferencesManager.getInstance(this);
    recyclerViewHourly = findViewById(R.id.recyclerViewHourly);
    progressBar = findViewById(R.id.progressBar);
    textViewTitle = findViewById(R.id.textViewTitle);
    root= findViewById(R.id.root);
}
    private void getHourlyForecast(boolean isNewSearch) {
        progressBar.setVisibility(View.VISIBLE);
        WeatherJsonAPI hourlyData = new WeatherJsonAPI(this, URL_API.ForecastURL);
        if (!isNewSearch) {
            List<ForecastItem> cached = hourlyData.getCachedHourlyForecast();
            if (cached != null && !cached.isEmpty()) {
                hourlyList.clear();
                hourlyList.addAll(cached);
                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
                return;
            }
        }

            hourlyData.getHourlyForecast(cityName, new WeatherJsonAPI.HourlyCallback() {
                @Override
                public void onSuccess(List<ForecastItem> newHourlyList,String json) {
                    progressBar.setVisibility(View.GONE);
                    hourlyList.clear();             // Adapter’in kullandığı listeyi temizle
                    hourlyList.addAll(newHourlyList); // Yeni verileri ekle
                    adapter.notifyDataSetChanged(); // RecyclerView’i güncelle
                    if(isSaveLocation) preferencesManager.saveHourlyForecastJson(json);
                }

                @Override
                public void onError(String error) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(Hourlyforecastactivity.this, error, Toast.LENGTH_SHORT).show();
                }
            });

    }

}