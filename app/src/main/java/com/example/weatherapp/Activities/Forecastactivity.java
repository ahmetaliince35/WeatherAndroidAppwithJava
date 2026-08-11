package com.example.weatherapp.Activities;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weatherapp.Adapters.ForecastAdapter;
import com.example.weatherapp.Helpers.ForecastItem;
import com.example.weatherapp.Helpers.WeatherReceiver.WeatherRepository;
import com.example.weatherapp.Helpers.WeatherReceiver.WeatherJsonAPI;
import com.example.weatherapp.generalFeatures.PreferencesManager;
import com.example.weatherapp.R;

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
    private String provider;
    private int backGroundRes;
    private PreferencesManager preferencesManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forecastactivity);
        initialVariables();
        getIntents();

        recyclerViewForecast.setLayoutManager(new LinearLayoutManager(this));
        forecastList = new ArrayList<>();
        adapter = new ForecastAdapter(this, forecastList, 1);
        recyclerViewForecast.setAdapter(adapter);

        String updateTime = LastUpdateTime(isNewSearch);

        getForecastData(cityName, isNewSearch);

        root.setBackgroundResource(backGroundRes);
        textViewCityTitle.setText(cityName + " - 5 Günlük Tahmin");
        textViewLastUpdate.setText("Son Güncelleme: " + updateTime);
    }

    private void initialVariables() {
        preferencesManager = PreferencesManager.getInstance(this);
        textViewCityTitle = findViewById(R.id.textViewCityTitle);
        recyclerViewForecast = findViewById(R.id.recyclerViewForecast);
        textViewLastUpdate = findViewById(R.id.textViewLastUpdate);
        progressBar = findViewById(R.id.progressBar);
        root = findViewById(R.id.root);
    }

    private void getIntents() {
        cityName = getIntent().getStringExtra("CITY_NAME");
        provider = getIntent().getStringExtra("PROVIDER");
        if (provider == null || provider.isEmpty()) {
            provider = "OWM";
        }
        backGroundRes = getIntent().getIntExtra("background-res", R.drawable.background);
        isNewSearch = getIntent().getBooleanExtra("isNewSearch", false);
        isSaveLocation = getIntent().getBooleanExtra("isSaveLocation", false);
    }

    private String LastUpdateTime(boolean isNewSearch) {
        long lastUpdate;
        if (!isNewSearch) {
            lastUpdate = preferencesManager.getLastUpdateTime();
        } else {
            lastUpdate = System.currentTimeMillis();
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM HH:mm - EE", new Locale("tr", "TR"));
        return sdf.format(new Date(lastUpdate));
    }

    private void getForecastData(String city, boolean isNewSearch) {
        progressBar.setVisibility(View.VISIBLE);
        String cleanCity = city.contains("/") ? city.split("/")[0].trim() : city;

        // SADECE OWM SEÇİLİYSE VE YENİ ARAMA DEĞİLSE CACHE'TEN OKU
        if (!isNewSearch && "OWM".equalsIgnoreCase(provider)) {
            WeatherJsonAPI dailyData = new WeatherJsonAPI(this);
            List<ForecastItem> cached = dailyData.getCachedDailyForecast();
            if (cached != null && !cached.isEmpty()) {
                forecastList.clear();
                forecastList.addAll(cached);
                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
                return;
            }
        }

        // OPEN-METEO VEYA DİĞER SERVİSLER İÇİN DOĞRUDAN REPOSITORY'DEN ÇEK
        WeatherRepository repository = new WeatherRepository(this);
        repository.getDailyForecast(provider, cleanCity, new WeatherRepository.ForecastDataCallback() {
            @Override
            public void onSuccess(List<ForecastItem> newforecastList) {
                progressBar.setVisibility(View.GONE);
                forecastList.clear();
                forecastList.addAll(newforecastList);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(Forecastactivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}