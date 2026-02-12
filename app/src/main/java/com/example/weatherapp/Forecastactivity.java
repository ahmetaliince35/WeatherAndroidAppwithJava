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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.TimeZone;

public class Forecastactivity extends AppCompatActivity {

    private TextView textViewCityTitle;
    private RecyclerView recyclerViewForecast;
    private ProgressBar progressBar;
    private ForecastAdapter adapter;
    private List<ForecastItem> forecastList;
    private LinearLayout root;

    private static final String API_KEY = URL_API.API_KEY;
    private static final String FORECAST_URL = URL_API.ForecastURL;

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
        root=findViewById(R.id.root);
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
        String url = FORECAST_URL + city + "&appid=" + API_KEY + "&units=metric&lang=tr";

        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray list = response.getJSONArray("list");
                            forecastList.clear();

                            // Bugünün tarihini al (sadece gün)
                            Calendar today = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                            today.set(Calendar.HOUR_OF_DAY, 0);
                            today.set(Calendar.MINUTE, 0);
                            today.set(Calendar.SECOND, 0);
                            today.set(Calendar.MILLISECOND, 0);
                            String todayKey = formatDateKey(today.getTimeInMillis() / 1000);

                            // Günlük verileri gruplamak için Map
                            Map<String, DailyData> dailyDataMap = new HashMap<>();

                            // Tüm 3 saatlik verileri işle ve günlere göre grupla
                            for (int i = 0; i < list.length(); i++) {
                                JSONObject item = list.getJSONObject(i);

                                String dateTime = item.getString("dt_txt");
                                long timestamp = item.getLong("dt");
                                String dayKey = formatDateKey(timestamp);
                                String displayDate = formatDate(timestamp);

                                // Hava durumu bilgileri
                                JSONObject main = item.getJSONObject("main");
                                double temp = main.getDouble("temp");
                                double tempMin = main.getDouble("temp_min");
                                double tempMax = main.getDouble("temp_max");
                                int humidity = main.getInt("humidity");

                                // Hava durumu açıklaması
                                JSONObject weather = item.getJSONArray("weather").getJSONObject(0);
                                String description = weather.getString("description");
                                String icon = weather.getString("icon");

                                // Rüzgar
                                JSONObject wind = item.getJSONObject("wind");
                                double windSpeed = wind.getDouble("speed");

                                // Yağış olasılığı
                                double pop = 0;
                                if (item.has("pop")) {
                                    pop = item.getDouble("pop") * 100;
                                }

                                // Yağış miktarı (3 saatlik)
                                double rainVolume = 0;
                                if (item.has("rain")) {
                                    JSONObject rain = item.getJSONObject("rain");
                                    if (rain.has("3h")) {
                                        rainVolume = rain.getDouble("3h");
                                    }
                                }

                                // Kar yağışı
                                if (item.has("snow")) {
                                    JSONObject snow = item.getJSONObject("snow");
                                    if (snow.has("3h")) {
                                        rainVolume += snow.getDouble("3h");
                                    }
                                }

                                // Günlük veriyi oluştur veya güncelle
                                if (!dailyDataMap.containsKey(dayKey)) {
                                    dailyDataMap.put(dayKey, new DailyData(displayDate, dayKey));
                                }

                                DailyData dailyData = dailyDataMap.get(dayKey);

                                // 12:00 verisini kaydet (ikon, açıklama, sıcaklık için)
                                if (dateTime.contains("12:00:00")) {
                                    dailyData.noonTemp = temp;
                                    dailyData.noonIcon = icon;
                                    dailyData.noonDescription = description;
                                }

                                // Günlük min/max sıcaklık
                                dailyData.minTemp = Math.min(dailyData.minTemp, tempMin);
                                dailyData.maxTemp = Math.max(dailyData.maxTemp, tempMax);

                                // Nem ve rüzgar ortalaması için topla
                                dailyData.totalHumidity += humidity;
                                dailyData.totalWindSpeed += windSpeed;

                                // Yağış bilgileri
                                dailyData.totalRainVolume += rainVolume;
                                dailyData.maxRainProbability = Math.max(dailyData.maxRainProbability, pop);

                                dailyData.count++;
                            }

                            // Map'ten listeye dönüştür - SIRALAMA ÖNEMLİ!
                            List<DailyData> sortedDailyData = new ArrayList<>(dailyDataMap.values());

                            // Tarihe göre sırala
                            sortedDailyData.sort((a, b) -> a.dayKey.compareTo(b.dayKey));

                            // ForecastItem oluştur
                            for (DailyData dailyData : sortedDailyData) {
                                // Bugünü atla
                                if (dailyData.dayKey.equals(todayKey)) {
                                    continue;
                                }

                                // 12:00 verisi yoksa bu günü atla
                                if (dailyData.noonIcon == null || dailyData.noonDescription == null) {
                                    continue;
                                }

                                ForecastItem forecastItem = new ForecastItem(
                                        dailyData.displayDate,
                                        dailyData.noonTemp,              // 12:00 sıcaklığı
                                        dailyData.noonDescription,       // 12:00 açıklaması
                                        dailyData.noonIcon,              // 12:00 ikonu
                                        dailyData.getAverageHumidity(),  // Günlük nem ortalaması
                                        dailyData.maxRainProbability,     // Günlük en yüksek yağış ihtimali
                                        dailyData.totalRainVolume,       // Günlük toplam yağış
                                        dailyData.getAverageWindSpeed()*3.6, // Günlük rüzgar ortalaması
                                        dailyData.minTemp,               // Günlük en düşük
                                        dailyData.maxTemp               // Günlük en yüksek
                                );
                                forecastList.add(forecastItem);

                                // İlk 5 günü al
                                if (forecastList.size() >= 5) {
                                    break;
                                }
                            }
                            adapter.notifyDataSetChanged();
                            progressBar.setVisibility(View.GONE);

                            if (forecastList.isEmpty()) {
                                Toast.makeText(Forecastactivity.this,
                                        "Tahmin verisi bulunamadı", Toast.LENGTH_SHORT).show();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(Forecastactivity.this,
                                    "Veri işlenirken hata oluştu", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(Forecastactivity.this,
                                "Tahmin verisi alınamadı", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        queue.add(jsonObjectRequest);
    }



    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
    private String formatDate(long timestamp) {
        Date date = new Date(timestamp * 1000);
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM - EEEE", new Locale("tr", "TR"));
        return sdf.format(date);
    }
    private String formatDateKey(long timestamp) {
        Date date = new Date(timestamp * 1000);
        SimpleDateFormat sdf = new SimpleDateFormat("yy-MM-dd", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }

    /**
     * Günlük veri toplayıcı sınıf
     */
    private static class DailyData
    {
        String displayDate;
        String dayKey;  // Sıralama için (yyyy-MM-dd)

        // 12:00 verisi
        String noonIcon = null;
        String noonDescription = null;
        double noonTemp = 0;

        // Günlük min/max (8 veriden)
        double minTemp = Double.MAX_VALUE;
        double maxTemp = Double.MIN_VALUE;

        // Ortalama için toplam değerler
        int totalHumidity = 0;
        double totalWindSpeed = 0;

        // Yağış bilgileri
        double totalRainVolume = 0;
        double maxRainProbability = 0;

        int count = 0;

        DailyData(String displayDate, String dayKey)
        {
            this.displayDate = displayDate;
            this.dayKey = dayKey;
        }

        int getAverageHumidity()
        {
            return count > 0 ? totalHumidity / count : 0;
        }

        double getAverageWindSpeed()
        {
            return count > 0 ? totalWindSpeed / count : 0;
        }
    }
}