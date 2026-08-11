package com.example.weatherapp.Activities;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weatherapp.Helpers.WeatherReceiver.MeteoStatParser;
import com.example.weatherapp.R;
import com.example.weatherapp.data.Databases.StationDatabase;
import com.example.weatherapp.data.Entities.StationEntity;
import com.example.weatherapp.Adapters.pastDailyDataAdapter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class pastDailyData extends AppCompatActivity {

    private AutoCompleteTextView search;
    private RecyclerView rvWeatherData;
    private pastDailyDataAdapter weatherAdapter;

    private StationDatabase database;
    private ArrayAdapter<String> adapter;
    private boolean isSelecting = false;

    private List<StationEntity> stations = new ArrayList<>();
    private StationEntity selectedStation = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.past_daily_data);

        // 1. Root Layout'u bul ve MainActivity'den gelen arka planı ata
        LinearLayout rootLayout = findViewById(R.id.rootLayout);
        int backgroundRes = getIntent().getIntExtra("background-res", R.drawable.background);
        if (rootLayout != null) {
            rootLayout.setBackgroundResource(backgroundRes);
        }
        database = StationDatabase.getInstance(this);
        new Thread(() -> {
            database.stationDao().fixQuestionMarksToI();
        }).start();
        search = findViewById(R.id.etSearchStation);
        rvWeatherData = findViewById(R.id.rvWeatherData);

        // RecyclerView Kurulumu
        rvWeatherData.setLayoutManager(new LinearLayoutManager(this));
        weatherAdapter = new pastDailyDataAdapter();
        rvWeatherData.setAdapter(weatherAdapter);


        search.setThreshold(2);

        // İSTASYON SEÇİLDİĞİNDE
        search.setOnItemClickListener((parent, view, position, id) -> {
            isSelecting = true;

            selectedStation = stations.get(position);
            search.setText(selectedStation.name);
            search.dismissDropDown();

            // Klavyeyi kapat
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(search.getWindowToken(), 0);
            }

            // Hava durumu verisini getir
            getWeatherData(selectedStation);
        });

        search.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                search.dismissDropDown();
            }
        });

        // Kullanıcı yazdıkça DB'den ara
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Kullanıcı tekrar yazmaya başladıysa seçim bayrağını sıfırla
                if (isSelecting) {
                    isSelecting = false;
                    return;
                }

                String query = s.toString().trim();

                if (query.length() >= 2) {
                    searchStations(query);
                } else {
                    stations.clear();
                    if (adapter != null) {
                        adapter.clear();
                        adapter.notifyDataSetChanged();
                    }
                    search.dismissDropDown();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void searchStations(String query) {
        new Thread(() -> {
            List<StationEntity> result = database.stationDao().searchStations("%" + query + "%");

            runOnUiThread(() -> {
                if (isSelecting) return;

                stations.clear();
                stations.addAll(result);

                List<String> stationNames = new ArrayList<>();
                for (StationEntity station : stations) {
                    stationNames.add(station.name);
                }

                adapter = new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        stationNames
                );

                search.setAdapter(adapter);

                if (!stationNames.isEmpty()) {
                    search.showDropDown();
                }
            });
        }).start();
    }

    private void getWeatherData(StationEntity station) {
        MeteoStatParser parser = new MeteoStatParser();
        LocalDate today = LocalDate.now();

// 7 gün öncesi
        LocalDate sevenDaysAgo = today.minusDays(7);
        LocalDate oneDaysAgo = today.minusDays(1);


// Meteostat API'nin istediği formata dönüştürme (YYYY-MM-DD)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        String endDate = oneDaysAgo.format(formatter);        // Örn: "2026-08-11"
        String startDate = sevenDaysAgo.format(formatter); // Örn: "2026-08-04"
        // Örnek olarak tarih aralığı girilmiştir
        parser.getDailyWeather(
                station.id,
                startDate,
                endDate,
                new retrofit2.Callback<MeteoStatParser.DailyWeatherResponse>() {
                    @Override
                    public void onResponse(
                            retrofit2.Call<MeteoStatParser.DailyWeatherResponse> call,
                            retrofit2.Response<MeteoStatParser.DailyWeatherResponse> response) {

                        // Logcat'e HTTP Kodunu ve Hata Mesajını Yazdırın
                        System.out.println("HTTP KODU: " + response.code());

                        if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                            if (response.body().data.isEmpty()) {
                                Toast.makeText(pastDailyData.this, "Bu istasyon için belirtilen tarihte veri yok.", Toast.LENGTH_SHORT).show();
                            } else {
                                List<MeteoStatParser.DailyWeather> weatherList = response.body().data;
                                java.util.Collections.reverse(weatherList);
                                weatherAdapter.updateData(weatherList, station.name, station.country);
                            }
                        } else {
                            try {
                                // Hata detayını görebilmek için errorBody'yi yazdırın
                                String error = response.errorBody() != null ? response.errorBody().string() : "Bilinmeyen hata";
                                System.out.println("API HATASI: " + error);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            Toast.makeText(pastDailyData.this, "Veri bulunamadı. HTTP: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(
                            retrofit2.Call<MeteoStatParser.DailyWeatherResponse> call,
                            Throwable t) {
                        Toast.makeText(pastDailyData.this, "Hata oluştu: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }
}