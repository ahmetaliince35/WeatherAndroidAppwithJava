package com.example.weatherapp;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity
{
    private EditText editTextCity;
    private Button buttonSearch;
    private Button buttonForecast;
    private Button buttonCurrentLocation;
    private Button buttonClearAll;
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
    private ImageView windDirect;
    private ScrollView root;
    private int currentbackround=R.drawable.background;

    private String currentCity = "";
    private boolean isNewSearch=false;
    private boolean isSaveLocation;
    private LocationGetter locationGetter;
    private PreferencesManager preferencesManager;
    private WeatherJsonAPI.WeatherData tempData;
    private ActivityResultLauncher<String> notificationPermissionLauncher;

    private static final int Location_Permission_Request=100;
    private static final  String TAG="Main Activity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationGetter=new LocationGetter(this);
        preferencesManager=new PreferencesManager(this);
        initViews();

        setupAutoUpdate();

        loadSavedWeather();

        buttonClicked();

        notificationSettings();
    }


    private void notificationSettings()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher = registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) {
                            Log.d("MAINACTIVITY", "Bildirim izni verildi");
                            Toast.makeText(this, "Bildirimler aktif", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.d("MAINACTIVITY", "Bildirim izni reddedildi");
                            Toast.makeText(this, "Bildirimler kapalı", Toast.LENGTH_SHORT).show();
                        }
                    }
            );

            requestNotificationPermission();
        }

    }
    private void buttonClicked() {
        buttonSearch.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String city = editTextCity.getText().toString().trim();
            if (!city.isEmpty()) {
                currentCity = city;
                showSaveLocationDialog(currentCity);
                getWeatherData(currentCity);
                buttonClearAll.setEnabled(true);
                isNewSearch=true;
            } else {
                Toast.makeText(MainActivity.this, "Lütfen şehir adı girin", Toast.LENGTH_SHORT).show();
            }
        }
    });

        buttonForecast.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!currentCity.isEmpty()) {
                Intent intent = new Intent(MainActivity.this, Forecastactivity.class);
                intent.putExtra("CITY_NAME", currentCity);
                intent.putExtra("background-res",currentbackround);
                intent.putExtra("isNewSearch",isNewSearch);
                intent.putExtra("isSaveLocation",isSaveLocation);
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
                intent.putExtra("isNewSearch",isNewSearch);
                intent.putExtra("isSaveLocation",isSaveLocation);
                startActivity(intent);
            } else {
                Toast.makeText(MainActivity.this, "Önce bir şehir arayın", Toast.LENGTH_SHORT).show();
            }
        }
    });
        buttonClearAll.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            preferencesManager.clearAll();

            currentCity = "";
            isSaveLocation = false;

            // UI'yi güvenli şekilde temizle
            textViewCityName.setText("Şehir yok");
            textViewTemperature.setText("--°C");
            textViewDescription.setText("---");
            textViewHumidity.setText("Nem: --%");
            textViewWindSpeed.setText("Rüzgar hızı: -- km/s");
            textViewFeelsLike.setText("---");
            textViewPressure.setText("Basınç: -- hPa");
            textViewLastUpdate.setText("Son güncelleme: --");
            imageViewWeatherIcon.setImageResource(R.drawable.icon_sunny);
            root.setBackgroundResource(R.drawable.background);
            windDirect.setRotation(0);
        }
    });

        buttonCurrentLocation.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            getPermission();
            }
        });
    }

private void initViews()
    {
        editTextCity = findViewById(R.id.editTextCity);

        buttonSearch = findViewById(R.id.buttonSearch);
        buttonForecast = findViewById(R.id.buttonForecast);
        buttonHourlyForecast = findViewById(R.id.buttonHourlyForecast);
        buttonCurrentLocation=findViewById(R.id.buttonCurrentLocation);
        buttonClearAll=findViewById(R.id.buttonClearAll);

        textViewCityName = findViewById(R.id.textViewCityName);
        textViewTemperature = findViewById(R.id.textViewTemperature);
        textViewDescription = findViewById(R.id.textViewDescription);
        textViewHumidity = findViewById(R.id.textViewHumidity);
        textViewWindSpeed = findViewById(R.id.textViewWindSpeed);
        textViewFeelsLike = findViewById(R.id.textViewFeelsLike);
        textViewPressure = findViewById(R.id.textViewPressure);
        textViewLastUpdate=findViewById(R.id.textViewLastUpdate);
        windDirect=findViewById(R.id.imageViewWindTurbine);
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
                isNewSearch= !currentCity.equals(preferencesManager.getSavedCityName());
                getWeatherData(currentCity);

                Toast.makeText(MainActivity.this,"Konum: "+cityName,Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onLocationError(String error)
            {

                Toast.makeText(MainActivity.this,error,Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getWeatherData(String city)
    {
        WeatherJsonAPI repo = new WeatherJsonAPI(this,URL_API.CurrentURL);
        repo.getWeather(city, new WeatherJsonAPI.WeatherCallback() {
            @Override
            public void onSuccess(WeatherJsonAPI.WeatherData data) {

                updateUI(data.cityName, data.temp, data.description, data.humidity, data.windSpeed*3.6,
                        data.feelsLike, data.pressure, data.icon,data.windDirection);

                tempData=data;
                buttonForecast.setEnabled(true);
                buttonHourlyForecast.setEnabled(true);
                updateBackgroundByWeather(data.icon);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
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
                        isSaveLocation=true;
                        if(tempData!=null)
                        {
                            preferencesManager.saveWeatherData(
                                    tempData.cityName,tempData.temp,tempData.description,
                                    tempData.humidity,tempData.windSpeed*3.6,tempData.feelsLike,
                                    tempData.pressure,tempData.icon,tempData.windDirection);
                            updateUI(tempData.cityName,tempData.temp,tempData.description,
                                    tempData.humidity,tempData.windSpeed*3.6,tempData.feelsLike,
                                    tempData.pressure,tempData.icon,tempData.windDirection);
                        }
                        Toast.makeText(MainActivity.this,
                                cityName + " varsayılan konum olarak kaydedildi",
                                Toast.LENGTH_LONG).show();

                    }
                })
                .setNegativeButton("Hayır", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(MainActivity.this,
                                "Konum kaydedilmedi. İstediğiniz zaman tekrar alabilirsiniz.",
                                Toast.LENGTH_SHORT).show();
                        isSaveLocation=false;
                    }

                })
                .setCancelable(false)
                .show();
    }


    /**
     * Otomatik güncellemeyi ayarla (günde 2 kere: 08:00 ve 20:00)
     */
    private void setupAutoUpdate() {
        PeriodicWorkRequest weatherUpdateRequest = new PeriodicWorkRequest.Builder(
                WeatherUpdater.class,
                1,
                TimeUnit.HOURS
        ).build();

        // WorkManager ile görevi planla
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "WeatherAutoUpdate",
                ExistingPeriodicWorkPolicy.KEEP, // Varsa devam ettir
                weatherUpdateRequest
        );

    }
    //Worker a ait görevleri denemek için oluşturuldu
    /*public void setupAutoUpdateNow() {
        // Worker için OneTimeWorkRequest oluştur
        OneTimeWorkRequest weatherWorkRequest = new OneTimeWorkRequest.Builder(WeatherUpdater.class)
                .build();

        // Worker'ı kuyruğa ekle
        WorkManager.getInstance(this)
                .enqueue(weatherWorkRequest);

        Log.d("WeatherWorkerLauncher", "WeatherWorker kuyruğa eklendi");
    }*/
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Bildirim izni isteniyor...");
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }
    private void loadSavedWeather() {

        if (preferencesManager.getSavedCityName()==null) return;
        if(preferencesManager.getIcon()!= null)
        {
            updateBackgroundByWeather(preferencesManager.getIcon());
        }
        currentCity = preferencesManager.getSavedCityName();
        buttonForecast.setEnabled(true);
        buttonHourlyForecast.setEnabled(true);
        updateUI(
                preferencesManager.getSavedCityName(),
                preferencesManager.getTemp(),
                preferencesManager.getDesc(),
                preferencesManager.getHumidity(),
                preferencesManager.getWind(),
                preferencesManager.getFeels(),
                preferencesManager.getPressure(),
                preferencesManager.getIcon(),
                preferencesManager.getWindDegree()
        );
    }

    private void updateUI(String cityName, double temperature, String description,
                          int humidity, double windSpeed, double feelsLike, int pressure, String icon,float winddirection) {
        textViewCityName.setText(cityName !=null ? cityName:" --");
        textViewTemperature.setText(String.format("%.1f°C", temperature));
        textViewDescription.setText(description != null ? description.substring(0, 1).toUpperCase() + description.substring(1): "--");
        textViewHumidity.setText("Nem: " + humidity + "%");
        textViewWindSpeed.setText((String.format(Locale.getDefault(), "Rüzgar hızı: %.0f km/s", windSpeed)));
        textViewPressure.setText("Basınç: " + pressure + " hPa");
        if(winddirection==0) textViewFeelsLike.setText("Kuzey → Güney");
        else if(winddirection==90) textViewFeelsLike.setText("Doğu → Batı ");
        else if(winddirection==180) textViewFeelsLike.setText("Güney → Kuzey");
        else if(winddirection==270) textViewFeelsLike.setText("Batı → Doğu");
        else if(winddirection>0 && winddirection<90) textViewFeelsLike.setText("KuzeyDoğu → GüneyBatı");
        else if(winddirection>=90 && winddirection<180) textViewFeelsLike.setText("GüneyDoğu → KuzeyBatı");
        else if(winddirection>=180 && winddirection<270) textViewFeelsLike.setText("GüneyBatı → KuzeyDoğu");
        else if(winddirection>=270 && winddirection<360) textViewFeelsLike.setText("KuzeyBatı → GüneyDoğu");
        windDirect.setRotation(winddirection+180);

        long lastUpdate = preferencesManager.getLastUpdateTime();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM HH:mm", new Locale("tr", "TR"));
        String updateTime = sdf.format(new Date(lastUpdate));
        textViewLastUpdate.setText("Son güncelleme: " + updateTime);

        // Hava durumu ikonunu ayarlama
        if(icon != null) setWeatherIcon(icon);

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