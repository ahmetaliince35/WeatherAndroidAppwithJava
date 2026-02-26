package com.example.weatherapp;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.Permission;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity
{

    private static final String TAG = "MainActivity";

    private EditText editTextCity;
    private Button buttonSearch;
    private Button buttonForecast;
    private Button buttonCurrentLocation;
    private Button buttonSearchLocation;
    private Button buttonHourlyForecast;
    private TextView textViewCityName;
    private TextView textViewTemperature;
    private TextView textViewDescription;
    private TextView textViewHumidity;
    private TextView textViewWindSpeed;
    private TextView textViewFeelsLike;
    private TextView textViewPressure;
    private TextView textViewLastUpdate;
    private ImageView imageViewWeatherIcon;
    private ScrollView root;
    private int currentbackround=R.drawable.background;

    private String currentCity = "";
    private LocationGetter locationGetter;
    private PreferencesManager preferencesManager;


    private static final int Location_Permission_Request=100;

    // OpenWeatherMap API Key - Kendi API key'inizi buraya ekleyin
    private static final String API_KEY = URL_API.API_KEY;
    private static final String BASE_URL = URL_API.CurrentURL;


    private SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        locationGetter=new LocationGetter(this);
        preferencesManager=new PreferencesManager(this);
        sharedPreferences=getSharedPreferences("WeatherAppPrefs",MODE_PRIVATE);
        initViews();

        setupAutoUpdate();

        loadSavedWeather();
        // Arama butonu
        buttonSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String city = editTextCity.getText().toString().trim();
                if (!city.isEmpty()) {
                    currentCity = city;
                    getWeatherData(city);
                } else {
                    Toast.makeText(MainActivity.this, "Lütfen şehir adı girin", Toast.LENGTH_SHORT).show();
                }
                buttonSearchLocation.setEnabled(true);
            }
        });

        // 5 Günlük Tahmin butonu
        buttonForecast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!currentCity.isEmpty()) {
                    Intent intent = new Intent(MainActivity.this, Forecastactivity.class);
                    intent.putExtra("CITY_NAME", currentCity);
                    intent.putExtra("background-res",currentbackround);
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "Önce bir şehir arayın", Toast.LENGTH_SHORT).show();
                }
            }
        });
        buttonHourlyForecast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!currentCity.isEmpty()) {
                    Intent intent = new Intent(MainActivity.this, Hourlyforecastactivity.class);
                    intent.putExtra("CITY_NAME", currentCity);
                    intent.putExtra("background-res",currentbackround);
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "Önce bir şehir arayın", Toast.LENGTH_SHORT).show();
                }
            }
        });
        buttonSearchLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String searchedCity= editTextCity.getText().toString().trim();
                newLocationSaved(searchedCity);
            }
        });

        buttonCurrentLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getPermission();
            }
        });

        preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                // Eğer değişen veri sıcaklık, şehir veya son güncelleme zamanıysa arayüzü yenile
                if (key.equals("city")) {
                    loadSavedWeather();
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ekran açıldığında dinlemeye başla
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Ekran kapandığında dinlemeyi bırak (pil/hafıza tasarrufu için önemli)
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
    }



    private void initViews()
    {
        // View'ları bağlama
        editTextCity = findViewById(R.id.editTextCity);

        buttonSearch = findViewById(R.id.buttonSearch);
        buttonForecast = findViewById(R.id.buttonForecast);
        buttonHourlyForecast = findViewById(R.id.buttonHourlyForecast);
        buttonCurrentLocation=findViewById(R.id.buttonCurrentLocation);
        buttonSearchLocation=findViewById(R.id.buttonSearchLocation);
        textViewCityName = findViewById(R.id.textViewCityName);
        textViewTemperature = findViewById(R.id.textViewTemperature);
        textViewDescription = findViewById(R.id.textViewDescription);
        textViewHumidity = findViewById(R.id.textViewHumidity);
        textViewWindSpeed = findViewById(R.id.textViewWindSpeed);
        textViewFeelsLike = findViewById(R.id.textViewFeelsLike);
        textViewPressure = findViewById(R.id.textViewPressure);
        textViewLastUpdate=findViewById(R.id.textViewLastUpdate);

        imageViewWeatherIcon = findViewById(R.id.imageViewWeatherIcon);

        root = findViewById(R.id.rootScroll);


    }

    private void getPermission()
    {

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED&&
        ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},
                    Location_Permission_Request);
            return;
        }
        else
        {
            getCurrentLocation();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == Location_Permission_Request) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // İzin verildi, konumu al
                getCurrentLocation();
            } else {

                Toast.makeText(this, "Konum izni reddedildi", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getCurrentLocation()
    {
        Toast.makeText(this,"Konum Alınıyor",Toast.LENGTH_SHORT).show();
        locationGetter.getCurrentLocation(new LocationGetter.LocationCallback() {
            @Override
            public void onLocationReceived(String cityName) {
                currentCity=cityName;

                showSaveLocationDialog(cityName);

                getWeatherData(cityName);

                Toast.makeText(MainActivity.this,"Konum: "+cityName,Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onLocationError(String error)
            {

                Toast.makeText(MainActivity.this,error,Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void newLocationSaved(String searchedCity)
    {

        currentCity=searchedCity;
        if(preferencesManager.getSavedCityName()== null)
        {
            preferencesManager.saveLocation(searchedCity);
            updateLastUpdateText();
        }
        else
        {
            showSaveLocationDialog(searchedCity);
        }
    }
    private void getWeatherData(String city)
    {
        String url = BASE_URL + city + "&appid=" + API_KEY + "&units=metric&lang=tr";

        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            // Şehir adı
                            String cityName = response.getString("name");

                            // Ana hava durumu bilgileri
                            JSONObject main = response.getJSONObject("main");
                            double temperature = main.getDouble("temp");
                            int humidity = main.getInt("humidity");
                            double feelsLike = main.getDouble("feels_like");
                            int pressure = main.getInt("pressure");

                            // Hava durumu açıklaması
                            JSONObject weather = response.getJSONArray("weather").getJSONObject(0);
                            String description = weather.getString("description");

                            String icon = weather.getString("icon");
                            updateBackgroundByWeather(icon);



                            // Rüzgar hızı
                            JSONObject wind = response.getJSONObject("wind");
                            double windSpeed = wind.getDouble("speed");

                            // UI'ı güncelleme
                            updateUI(cityName, temperature, description, humidity, windSpeed*3.6,
                                    feelsLike, pressure, icon);

                            preferencesManager.saveWeatherData(cityName, temperature, description, humidity, windSpeed * 3.6,
                                    feelsLike, pressure, icon
                            );

                            // 5 Günlük Tahmin butonunu aktif et
                            buttonForecast.setEnabled(true);
                            buttonHourlyForecast.setEnabled(true);


                        } catch (JSONException e) {
                            Toast.makeText(MainActivity.this, "Veri işlenirken hata oluştu", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(MainActivity.this, "Şehir bulunamadı", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        queue.add(jsonObjectRequest);
    }

    private void showSaveLocationDialog(final String cityName) {
        new AlertDialog.Builder(this)
                .setTitle("Konum Kaydet")
                .setMessage(cityName + " konumunu varsayılan olarak kaydetmek ister misiniz?\n\n" +
                        "Kaydederseniz:\n" +
                        "• Uygulama açıldığında otomatik yüklenecek\n" +
                        "• Günde 2 kere otomatik güncellenecek")
                .setPositiveButton("Evet, Kaydet", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Konumu kaydet
                        preferencesManager.saveLocation(cityName);
                        updateLastUpdateText();
                        Toast.makeText(MainActivity.this,
                                cityName + " varsayılan konum olarak kaydedildi",
                                Toast.LENGTH_LONG).show();

                    }
                })
                .setNegativeButton("Hayır", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        preferencesManager.clearAll();
                        Toast.makeText(MainActivity.this,
                                "Konum kaydedilmedi. İstediğiniz zaman tekrar alabilirsiniz.",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void updateLastUpdateText() {
        long lastUpdate = preferencesManager.getLastUpdateTime();

        if (lastUpdate > 0)
        {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM HH:mm", new Locale("tr", "TR"));
            String updateTime = sdf.format(new Date(lastUpdate));
            textViewLastUpdate.setText("Son güncelleme: " + updateTime);
            textViewLastUpdate.setVisibility(View.VISIBLE);

        }
    }

    /**
     * Otomatik güncellemeyi ayarla (günde 2 kere: 08:00 ve 20:00)
     */
    private void setupAutoUpdate() {
        PeriodicWorkRequest weatherUpdateRequest = new PeriodicWorkRequest.Builder(
                WeatherUpdater.class,
                15,
                TimeUnit.MINUTES
        ).build();

        // WorkManager ile görevi planla
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "WeatherAutoUpdate",
                ExistingPeriodicWorkPolicy.KEEP, // Varsa devam ettir
                weatherUpdateRequest
        );

    }
    private void loadSavedWeather() {

        if (preferencesManager.getSavedCityName()==null) return;
        if(preferencesManager.getIcon()!= null)
        {
            updateBackgroundByWeather(preferencesManager.getIcon());
        }
        updateUI(
                preferencesManager.getSavedCityName(),
                preferencesManager.getTemp(),
                preferencesManager.getDesc(),
                preferencesManager.getHumidity(),
                preferencesManager.getWind(),
                preferencesManager.getFeels(),
                preferencesManager.getPressure(),
                preferencesManager.getIcon()
        );

        updateLastUpdateText();
    }
    private void updateUI(String cityName, double temperature, String description,
                          int humidity, double windSpeed, double feelsLike, int pressure, String icon) {
        textViewCityName.setText(cityName);
        textViewTemperature.setText(String.format("%.1f°C", temperature));
        textViewDescription.setText(description.substring(0, 1).toUpperCase() + description.substring(1));
        textViewHumidity.setText("Nem: " + humidity + "%");
        textViewWindSpeed.setText((String.format(Locale.getDefault(), "Rüzgar hızı: %.0f km/s", windSpeed)));
        textViewFeelsLike.setText("Hissedilen: " + String.format("%.1f°C", feelsLike));
        textViewPressure.setText("Basınç: " + pressure + " hPa");

        // Hava durumu ikonunu ayarlama
        setWeatherIcon(icon);
    }

    private void setWeatherIcon(String icon) {
        // İkon koduna göre drawable kaynak ayarlama
        int iconResource;
        switch (icon) {
            case "01d":
                iconResource = R.drawable.icon_sunny;
                break;
            case "01n":
                iconResource=R.drawable.icon_moon;
            case "02d":
            case "03d":
                iconResource = R.drawable.icon_partlycloudy;
                break;
            case "02n":
            case "03n":
                iconResource=R.drawable.icon_partlycloudy_night;
            case "04d":
            case "04n":
                iconResource = R.drawable.icon_cloudy;
                break;
            case "09d":
            case "09n":
            case "10d":
            case "10n":
                iconResource = R.drawable.icon_rainy;
                break;
            case "11d":
            case "11n":
                iconResource = R.drawable.icon_thunderstorm;
                break;
            case "13d":
            case "13n":
                iconResource = R.drawable.icon_snowy;
                break;
            default:
                iconResource = R.drawable.icon_sunny;
                break;
        }
        imageViewWeatherIcon.setImageResource(iconResource);
    }
    private void updateBackgroundByWeather(String iconCode) {
        int backgroundRes;

        if (iconCode.startsWith("01")) {
            backgroundRes = iconCode.endsWith("d")
                    ? R.drawable.sun
                    : R.drawable.moon;

        }
        else if (iconCode.startsWith("02") || iconCode.startsWith("03"))
        {
            backgroundRes=R.drawable.partlycloud;
        }
        else if (iconCode.startsWith("04"))
        {
            backgroundRes = R.drawable.very_cloud;

        }
        else if (iconCode.startsWith("09") || iconCode.startsWith("10"))
        {
            backgroundRes = R.drawable.rain;

        }
        else if(iconCode.startsWith("11"))
        {
                backgroundRes= R.drawable.thunder;

        }
        else if (iconCode.startsWith("13"))
        {
            backgroundRes = R.drawable.snow;

        }
        else
        {
            backgroundRes = R.drawable.sun;
        }
            currentbackround=backgroundRes;
            root.setBackgroundResource(currentbackround);
    }
}