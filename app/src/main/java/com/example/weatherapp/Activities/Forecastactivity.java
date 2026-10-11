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

import android.text.TextUtils;
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
    private View root;
    private boolean isNewSearch;
    private boolean isSaveLocation;
    private String cityName;
    private String provider;
    private String vcModel;
    private int backGroundRes;
    private PreferencesManager preferencesManager;
    private com.example.weatherapp.Helpers.charts.FeatureTrendManager trendManager;

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

        View trendLayout = findViewById(R.id.trendSectionLayout);
        trendManager = new com.example.weatherapp.Helpers.charts.FeatureTrendManager(trendLayout);

        String updateTime = LastUpdateTime(isNewSearch);

        getForecastData(cityName, isNewSearch);

        root.setBackgroundResource(backGroundRes);
        textViewCityTitle.setText(getString(R.string.forecast_title_label, cityName != null ? cityName : ""));
        textViewLastUpdate.setText(getString(R.string.last_update_label, updateTime, getProviderDisplayName()));
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
        if (TextUtils.isEmpty(cityName)) {
            cityName = preferencesManager.getSavedCityName();
        }
        provider = getIntent().getStringExtra("PROVIDER");
        if (provider == null || provider.isEmpty()) {
            provider = preferencesManager.getSelectedProvider();
        }
        vcModel = getIntent().getStringExtra("VC_MODEL");
        if (vcModel == null) {
            vcModel = preferencesManager.getVisualCrossingModel();
        }
        backGroundRes = getIntent().getIntExtra("background-res", R.drawable.background);
        isNewSearch = getIntent().getBooleanExtra("isNewSearch", false);
        isSaveLocation = getIntent().getBooleanExtra("isSaveLocation", false);
    }

    private String getProviderDisplayName() {
        if ("OWM".equalsIgnoreCase(provider)) return "OpenWeatherMap";
        if ("WEATHER_API".equalsIgnoreCase(provider)) return "WeatherAPI";
        if ("VISUAL_CROSSING".equalsIgnoreCase(provider)) return "Visual Crossing";
        return "Open-Meteo";
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
        if (city == null || city.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(Forecastactivity.this, "Şehir bulunamadı", Toast.LENGTH_SHORT).show();
            return;
        }

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
                trendManager.setForecastData(forecastList, false);
                progressBar.setVisibility(View.GONE);
                return;
            }
        }


        WeatherRepository repository = new WeatherRepository(this);
        repository.getDailyForecast(provider, vcModel, cleanCity, new WeatherRepository.ForecastDataCallback() {
            @Override
            public void onSuccess(List<ForecastItem> newforecastList) {
                progressBar.setVisibility(View.GONE);
                forecastList.clear();
                if (newforecastList != null) {
                    for (ForecastItem item : newforecastList) {
                        if (item != null && item.getDate() != null) {
                            forecastList.add(item);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
                trendManager.setForecastData(forecastList, false);
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(Forecastactivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}