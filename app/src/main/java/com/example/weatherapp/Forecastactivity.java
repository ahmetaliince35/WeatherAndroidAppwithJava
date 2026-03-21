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
    private String cityName;
    private String updateTime;
    private int backGroundRes;
    PreferencesManager preferencesManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forecastactivity);

        String updateTime= LastUpdateTime();

        recyclerViewForecast.setLayoutManager(new LinearLayoutManager(this));
        forecastList = new ArrayList<>();
        adapter = new ForecastAdapter(this, forecastList);
        recyclerViewForecast.setAdapter(adapter);

        textViewLastUpdate.setText("Son güncelleme: " + updateTime);
        root.setBackgroundResource(backGroundRes);
        textViewCityTitle.setText(cityName + " - 5 Günlük Tahmin");

        initialVariables();
        getIntents();
        getForecastData(cityName,isNewSearch);
    }
    private void  initialVariables()
    {
        preferencesManager=PreferencesManager.getInstance(this);
        textViewCityTitle = findViewById(R.id.textViewCityTitle);
        recyclerViewForecast = findViewById(R.id.recyclerViewForecast);
        textViewLastUpdate=findViewById(R.id.textViewLastUpdate);
        progressBar = findViewById(R.id.progressBar);
        root = findViewById(R.id.root);
    }
    private void  getIntents()
    {
        cityName = getIntent().getStringExtra("CITY_NAME");
        backGroundRes = getIntent().getIntExtra("background-res", R.drawable.background);
        isNewSearch = getIntent().getBooleanExtra("isNewSearch",false);
        isSaveLocation=getIntent().getBooleanExtra("isSaveLocation",false);
    }
private String LastUpdateTime()
{
    long lastUpdate= preferencesManager.getLastUpdateTime();
    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM HH:mm", new Locale("tr", "TR"));
    return  sdf.format(new Date(lastUpdate));
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
    }
}