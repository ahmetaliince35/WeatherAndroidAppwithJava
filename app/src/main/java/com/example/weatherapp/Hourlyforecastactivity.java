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
    private boolean isNewSearch;
    private boolean isSaveLocation;
    private String cityName;
    private LinearLayout root;

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
        if (TextUtils.isEmpty(cityName)) {
            cityName = new PreferencesManager(this).getSavedCityName();
        }
        int bgRes = getIntent().getIntExtra("background-res",R.drawable.background);
        isNewSearch=getIntent().getBooleanExtra("isNewSearch",false);
        isSaveLocation=getIntent().getBooleanExtra("isSaveLocation",false);
        root.setBackgroundResource(bgRes);
        if (cityName == null || cityName.isEmpty()) {
            Toast.makeText(this, "Şehir bilgisi bulunamadı", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // View'ları bağla

        // Başlığı ayarla
        textViewTitle.setText(cityName + " - 72 Saatlik Tahmin");

        // RecyclerView ayarları
        hourlyList = new ArrayList<>();
        adapter = new ForecastAdapter(this, hourlyList);
        recyclerViewHourly.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewHourly.setAdapter(adapter);

        // Verileri çek
        getHourlyForecast(isNewSearch);
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
        } else {
            hourlyData.getHourlyForecast(cityName, new WeatherJsonAPI.HourlyCallback() {
                @Override
                public void onSuccess(List<ForecastItem> newHourlyList,String json) {
                    progressBar.setVisibility(View.GONE);
                    hourlyList.clear();             // Adapter’in kullandığı listeyi temizle
                    hourlyList.addAll(newHourlyList); // Yeni verileri ekle
                    adapter.notifyDataSetChanged(); // RecyclerView’i güncelle
                    if(isSaveLocation)new PreferencesManager(getApplicationContext()).saveHourlyForecastJson(json);
                }

                @Override
                public void onError(String error) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(Hourlyforecastactivity.this, error, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}