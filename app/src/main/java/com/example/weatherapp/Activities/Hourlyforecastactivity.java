package com.example.weatherapp.Activities;

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

import com.example.weatherapp.Adapters.ForecastAdapter;
import com.example.weatherapp.Helpers.ForecastItem;
import com.example.weatherapp.Helpers.WeatherReceiver.WeatherRepository;
import com.example.weatherapp.Helpers.WeatherReceiver.WeatherJsonAPI;
import com.example.weatherapp.generalFeatures.PreferencesManager;
import com.example.weatherapp.generalFeatures.WeatherWidgetProvider;
import com.example.weatherapp.R;

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
    private boolean isNewSearch;
    private boolean isSaveLocation;
    private TextView lastUpdateTime;
    private String cityName;
    private String provider;
    private String vcModel;
    private View root;
    private int backGroundRes;
    private PreferencesManager preferencesManager;
    private com.example.weatherapp.Helpers.charts.FeatureTrendManager trendManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hourlyforecastactivity);

        initialVariables();
        getIntents();

        textViewTitle.setText(getString(R.string.hourly_forecast_title_label, cityName != null ? cityName : ""));
        root.setBackgroundResource(backGroundRes);

        hourlyList = new ArrayList<>();
        adapter = new ForecastAdapter(this, hourlyList, 0);
        recyclerViewHourly.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewHourly.setAdapter(adapter);

        View trendLayout = findViewById(R.id.trendSectionLayout);
        trendManager = new com.example.weatherapp.Helpers.charts.FeatureTrendManager(trendLayout);

        String lastupdateTimeStr = LastUpdateTime(isNewSearch);
        lastUpdateTime.setText(getString(R.string.last_update_label, lastupdateTimeStr, getProviderDisplayName()));

        getHourlyForecast(isNewSearch);
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

    private void initialVariables() {
        preferencesManager = PreferencesManager.getInstance(this);
        recyclerViewHourly = findViewById(R.id.recyclerViewHourly);
        progressBar = findViewById(R.id.progressBar);
        textViewTitle = findViewById(R.id.textViewTitle);
        lastUpdateTime = findViewById(R.id.textViewLastUpdate);
        root = findViewById(R.id.root);
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

    private void getHourlyForecast(boolean isNewSearch) {
        if (cityName == null || cityName.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(Hourlyforecastactivity.this, "Şehir bulunamadı", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        String cleanCity = cityName.contains("/") ? cityName.split("/")[0].trim() : cityName;

        WeatherRepository repository = new WeatherRepository(this);
        repository.getHourlyForecast(provider, vcModel, cleanCity, new WeatherRepository.ForecastDataCallback() {
            @Override
            public void onSuccess(List<ForecastItem> newHourlyList) {
                progressBar.setVisibility(View.GONE);
                hourlyList.clear();
                if (newHourlyList != null) {
                    for (ForecastItem item : newHourlyList) {
                        if (item != null && item.getDate() != null) {
                            hourlyList.add(item);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
                trendManager.setForecastData(hourlyList, true);
                preferencesManager.saveWidgetHourlyItems(hourlyList);
                WeatherWidgetProvider.updateAllWidgets(Hourlyforecastactivity.this);
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(Hourlyforecastactivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}