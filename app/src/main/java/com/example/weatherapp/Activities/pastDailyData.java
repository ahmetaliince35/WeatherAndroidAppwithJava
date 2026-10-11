package com.example.weatherapp.Activities;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
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
    private com.example.weatherapp.Helpers.charts.FeatureTrendManager trendManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.past_daily_data);

        View rootLayout = findViewById(R.id.rootLayout);
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

        View trendLayout = findViewById(R.id.trendSectionLayout);
        trendManager = new com.example.weatherapp.Helpers.charts.FeatureTrendManager(trendLayout);

        rvWeatherData.setLayoutManager(new LinearLayoutManager(this));
        weatherAdapter = new pastDailyDataAdapter();
        rvWeatherData.setAdapter(weatherAdapter);


        search.setThreshold(2);

        search.setOnItemClickListener((parent, view, position, id) -> {
            isSelecting = true;
            String clickedName = null;
            if (parent != null && parent.getItemAtPosition(position) != null) {
                clickedName = parent.getItemAtPosition(position).toString();
            }

            selectedStation = null;
            if (clickedName != null) {
                for (StationEntity s : stations) {
                    if (s != null && clickedName.equals(s.name)) {
                        selectedStation = s;
                        break;
                    }
                }
            }
            if (selectedStation == null && position >= 0 && position < stations.size()) {
                selectedStation = stations.get(position);
            }

            if (selectedStation != null) {
                search.setText(selectedStation.name);
                search.dismissDropDown();

                // Klavyeyi kapat
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(search.getWindowToken(), 0);
                }

                getWeatherData(selectedStation);
            }
        });

        search.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                search.dismissDropDown();
            }
        });

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


        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        String endDate = oneDaysAgo.format(formatter);
        String startDate = sevenDaysAgo.format(formatter);
        parser.getDailyWeather(
                station.id,
                startDate,
                endDate,
                new retrofit2.Callback<MeteoStatParser.DailyWeatherResponse>() {
                    @Override
                    public void onResponse(
                            retrofit2.Call<MeteoStatParser.DailyWeatherResponse> call,
                            retrofit2.Response<MeteoStatParser.DailyWeatherResponse> response) {

                        System.out.println("HTTP KODU: " + response.code());

                        if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                            List<MeteoStatParser.DailyWeather> rawList = response.body().data;
                            List<MeteoStatParser.DailyWeather> validList = new ArrayList<>();
                            for (MeteoStatParser.DailyWeather item : rawList) {
                                if (item != null && item.date != null && !item.date.trim().isEmpty()) {
                                    validList.add(item);
                                }
                            }
                            if (validList.isEmpty()) {
                                Toast.makeText(pastDailyData.this, "Bu istasyon için belirtilen tarihte veri yok.", Toast.LENGTH_SHORT).show();
                            } else {
                                java.util.Collections.reverse(validList);
                                String sName = (station != null && station.name != null) ? station.name : "";
                                String sCountry = (station != null && station.country != null) ? station.country : "";
                                weatherAdapter.updateData(validList, sName, sCountry);
                                trendManager.setPastStationData(validList);
                            }
                        } else {
                            try {
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