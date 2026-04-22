package com.example.weatherapp;

import static android.view.View.VISIBLE;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.example.weatherapp.Helpers.AutoUpdateConfig;
import com.example.weatherapp.Helpers.CitySearchDB;
import com.example.weatherapp.Helpers.FavoriCities;
import com.example.weatherapp.Helpers.UIUpdate;
import com.example.weatherapp.Helpers.URL_API;
import com.example.weatherapp.Helpers.WeatherJsonAPI;
import com.example.weatherapp.data.AppDatabase;
import com.example.weatherapp.data.WeatherEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity
{
    private AutoCompleteTextView editTextCity;
    private Button buttonSearch;
    private Button buttonForecast;
    private Button buttonCurrentLocation;
    private Button buttonClearAll;
    private Button buttonHourlyForecast;
    private Button saveFavorites;
    private Button deleteFavories;
    private TextView textViewCityName;
    private TextView textViewTemperature;
    private TextView textViewDescription;
    private TextView textViewHumidity;
    private TextView textViewWindSpeed;
    private TextView textViewPressure;
    private TextView textViewLastUpdate;
    private TextView textViewCloudiness;
    private TextView textViewAIAdvice;
    private TextView textViewopenFavoriCities;
    private ImageView imageViewWeatherIcon;
    private ImageView windDirect;
    private ScrollView root;
    private ProgressBar progressBar;
    private DrawerLayout drawerLayout;
    private RecyclerView  recyclerFavorites;
    private List<WeatherEntity> favoriteList;
    private int currentbackround=R.drawable.background;

    private String currentCity = "";
    private boolean isNewSearch=false;
    private boolean isSaveLocation;
    private LocationGetter locationGetter;
    private PreferencesManager preferencesManager;
    private WeatherJsonAPI.WeatherData tempData;
    private ActivityResultLauncher<String> notificationPermissionLauncher;
    private AppDatabase citydb;
    private AppDatabase favoritesdb;
    FavoriCityAdapter favoriCityAdapter;
    private static final int Location_Permission_Request=100;
    private static final  String TAG="Main Activity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        connectionDatabase();
        setupRecyclerView();

        locationGetter=new LocationGetter(this);
        preferencesManager=PreferencesManager.getInstance(this);

        AutoUpdateConfig.setupAutoUpdate(getApplicationContext());
        AutoUpdateConfig.setupAutoUpdateFavoriCities(getApplicationContext());

        textViewopenFavoriCities.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawerLayout.openDrawer(Gravity.START);
            }
        });
        setupAutoComplete();
        loadSavedWeather();
        buttonClicked();
        notificationSettings();

    }
    private void initViews()
    {
        editTextCity = findViewById(R.id.editTextCity);

        buttonSearch = findViewById(R.id.buttonSearch);
        buttonForecast = findViewById(R.id.buttonForecast);
        buttonHourlyForecast = findViewById(R.id.buttonHourlyForecast);
        buttonCurrentLocation=findViewById(R.id.buttonCurrentLocation);
        buttonClearAll=findViewById(R.id.buttonClearAll);
        textViewCloudiness=findViewById(R.id.textViewCloudiness);
        textViewCityName = findViewById(R.id.textViewCityName);
        textViewTemperature = findViewById(R.id.textViewTemperature);
        textViewDescription = findViewById(R.id.textViewDescription);
        textViewHumidity = findViewById(R.id.textViewHumidity);
        textViewWindSpeed = findViewById(R.id.textViewWindSpeed);
        textViewPressure = findViewById(R.id.textViewPressure);
        textViewLastUpdate=findViewById(R.id.textViewLastUpdate);
        textViewAIAdvice=findViewById(R.id.TextViewAIAdvice);
        textViewopenFavoriCities=findViewById(R.id.openFavoriCities);
        progressBar=findViewById(R.id.progressBar);
        windDirect=findViewById(R.id.imageViewWindTurbine);
        imageViewWeatherIcon = findViewById(R.id.imageViewWeatherIcon);
        drawerLayout=findViewById(R.id.drawerlayout);
        recyclerFavorites=findViewById(R.id.recyclerFavorites);
        root = findViewById(R.id.rootScroll);
        saveFavorites=findViewById(R.id.buttonAddFavori);
        deleteFavories=findViewById(R.id.buttondeleteALlFavori);
    }

private void connectionDatabase()
{
    citydb= Room.databaseBuilder(getApplicationContext(),
                    AppDatabase.class,"weather_db.db")
            .fallbackToDestructiveMigration()
            .createFromAsset("weather.db").build();

favoritesdb=AppDatabase.getInstance(getApplicationContext());
}
private void setupRecyclerView()
{
    favoriteList = new ArrayList<>();

    favoriCityAdapter = new FavoriCityAdapter(favoriteList, new FavoriCityAdapter.OnCityAction() {
        @Override
        public void onClick(WeatherEntity city) {
         currentCity=city.cityName;
         isNewSearch=true;
         isSaveLocation=false;
         getWeatherData(currentCity);
         drawerLayout.closeDrawer(Gravity.START);
        }

        @Override
        public void onDelete(WeatherEntity city) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Şehri Sil")
                    .setMessage(city.cityName + " favorilerden silinsin mi?")
                    .setPositiveButton("Evet", (dialog, which) -> {
                        Executors.newSingleThreadExecutor().execute(() -> {
                            // Veritabanından sil
                            favoritesdb.weatherDao().deleteByCity(city.cityName);
                        });
                    })
                    .setNegativeButton("Hayır", null)
                    .show();
        }

    });
    recyclerFavorites.setLayoutManager(new LinearLayoutManager(this));
    recyclerFavorites.setAdapter(favoriCityAdapter);
    favoritesdb.weatherDao().getAllFavorites().observe(this, newList -> {
        if (newList != null) {
            favoriteList.clear();
            favoriteList.addAll(newList);
            favoriCityAdapter.notifyDataSetChanged();
            Log.d("LIVE_DATA", "Liste otomatik güncellendi. Eleman sayısı: " + newList.size());
        }
    });
}

    private void setupAutoComplete()
    {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        editTextCity.setAdapter(adapter);
        editTextCity.setOnItemClickListener((parent, view, position, id) -> {
            String selected=adapter.getItem(position);
            adapter.clear();
            adapter.notifyDataSetChanged();
            editTextCity.setText(selected.split(",")[0]);
            editTextCity.dismissDropDown();
        });
        editTextCity.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                final String input = s.toString();
                Executors.newSingleThreadExecutor().execute(() -> {
                    List<String> cityNames=CitySearchDB.GetCityList(input,citydb);
                    runOnUiThread(() -> {
                        adapter.clear();
                        adapter.addAll(cityNames);
                        adapter.notifyDataSetChanged();
                        editTextCity.showDropDown();
                    });
                });
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

    }
    private void loadSavedWeather() {

        if (preferencesManager.getSavedCityName()==null) return;
        if(preferencesManager.getIcon()!= null)
        {
            updateBackgroundByWeather(preferencesManager.getIcon(),preferencesManager.getDesc());
        }
        currentCity = preferencesManager.getSavedCityName();
        buttonForecast.setEnabled(true);
        buttonHourlyForecast.setEnabled(true);
        updateUI(
                preferencesManager.getAIAdvice(),
                preferencesManager.getSavedCityName(),
                preferencesManager.getTemp(),
                preferencesManager.getDesc(),
                preferencesManager.getHumidity(),
                preferencesManager.getWind(),
                preferencesManager.getFeels(),
                preferencesManager.getPressure(),
                preferencesManager.getIcon(),
                preferencesManager.getWindDegree(),
                preferencesManager.getLastUpdateTime(),
                preferencesManager.getCloudiness()
        );
    }
    private void buttonClicked()
    {
        buttonSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String city = editTextCity.getText().toString().trim();
                if (!city.isEmpty()) {
                    currentCity = city;
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
                textViewAIAdvice.setText("Verilebilecek bir hava durumu tavsiyesi yok");
                textViewCityName.setText("Şehir yok");
                textViewTemperature.setText("--°C");
                textViewDescription.setText("---");
                textViewHumidity.setText("Nem: --%");
                textViewWindSpeed.setText("Rüzgar hızı: -- km/s");
                textViewPressure.setText("Basınç: -- hPa");
                textViewLastUpdate.setText("Son güncelleme: --");
                textViewCloudiness.setText("Bulut Oranı: --%");
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

        saveFavorites.setOnClickListener(new View.OnClickListener() {
            FavoriCities favoriCities = new FavoriCities(favoritesdb);
            @Override
            public void onClick(View v) {
                if (tempData == null)
                {
                    Toast.makeText(MainActivity.this, "Önce bir şehir araması yapmalısın!", Toast.LENGTH_SHORT).show();
                    return;
                }

                Executors.newSingleThreadExecutor().execute(() -> {

                    boolean isAdded = favoriCities.saveToFavorites(
                            currentCity,
                            tempData.temp,
                            tempData.description,
                            tempData.humidity,
                            tempData.windSpeed,
                            tempData.icon);

                    if (isAdded) {

                        runOnUiThread(() -> {
                            drawerLayout.openDrawer(Gravity.START);
                        });

                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(getApplicationContext(), "Bu şehir zaten kayıtlı", Toast.LENGTH_SHORT).show();
                            drawerLayout.openDrawer(Gravity.START);
                        });
                    }
                });
            }
        });
        deleteFavories.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Hepsini Sil")
                        .setMessage("Tüm favori şehirleriniz silinecek. Emin misiniz?")
                        .setPositiveButton("Evet, Sil", (dialog, which) -> {
                            Executors.newSingleThreadExecutor().execute(() -> {
                                favoritesdb.weatherDao().deleteallcity();
                            });
                        })
                        .setNegativeButton("Vazgeç", null)
                        .show();
            }
        });
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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Bildirim izni isteniyor...");
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
                }
            }

        }

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
        progressBar.setVisibility(VISIBLE);
        locationGetter.getCurrentLocation(new LocationGetter.LocationCallback() {
            @Override
            public void onLocationReceived(String cityName) {
                currentCity=cityName;
                isNewSearch= true;
                getWeatherData(currentCity);

                Toast.makeText(MainActivity.this,"Konum: "+cityName,Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onLocationError(String error)
            {
                progressBar.setVisibility(View.INVISIBLE);
                Toast.makeText(MainActivity.this,error,Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getWeatherData(String city)
    {
        progressBar.setVisibility(VISIBLE);
        WeatherJsonAPI repo = new WeatherJsonAPI(this, URL_API.CurrentURL);
        repo.getWeather(city,true, new WeatherJsonAPI.WeatherCallback() {
            @Override
            public void onSuccess(WeatherJsonAPI.WeatherData data) {
                progressBar.setVisibility(View.INVISIBLE);
                updateUI(data.AIAdvice,data.cityName, data.temp, data.description, data.humidity, data.windSpeed*3.6,
                        data.feelsLike, data.pressure, data.icon,data.windDirection,System.currentTimeMillis(),data.cloudiness);

                tempData=data;
                showSaveLocationDialog(city);
                buttonForecast.setEnabled(true);
                buttonHourlyForecast.setEnabled(true);
                updateBackgroundByWeather(data.icon,data.description);
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.INVISIBLE);
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
                        preferencesManager.updateLastUpdateTime();
                        isSaveLocation=true;
                        if(tempData!=null)
                        {
                            preferencesManager.saveWeatherData(
                                    tempData.AIAdvice,tempData.cityName,tempData.temp,tempData.description,
                                    tempData.humidity,tempData.windSpeed*3.6,tempData.feelsLike,
                                    tempData.pressure,tempData.icon,tempData.windDirection,tempData.cloudiness);
                            updateUI(tempData.AIAdvice,tempData.cityName,tempData.temp,tempData.description,
                                    tempData.humidity,tempData.windSpeed*3.6,tempData.feelsLike,
                                    tempData.pressure,tempData.icon,tempData.windDirection,preferencesManager.getLastUpdateTime(),tempData.cloudiness);
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




    private void updateUI(String AIAdvice,String cityName, double temperature, String description,
                          double humidity, double windSpeed, double feelsLike, int pressure, String icon,float winddirection,long timestamp,int cloudiness) {
        Log.d("MAİNDENEME","Gelen veri:"+AIAdvice);
        if (AIAdvice != null && !AIAdvice.isEmpty()) {
            textViewAIAdvice.setText(AIAdvice);
        } else {
            // Eğer her iki tarafta boşsa bir placeholder yaz
            textViewAIAdvice.setText("AI önerisi bekleniyor...");
        }
        textViewCityName.setText(cityName !=null ? cityName:" --");
        textViewTemperature.setText(String.format("%.1f°C", temperature));
        textViewDescription.setText(description != null ? description.substring(0, 1).toUpperCase() + description.substring(1): "--");
        textViewHumidity.setText("Nem: " + humidity + "%");
        textViewWindSpeed.setText((String.format(Locale.getDefault(), "Rüzgar hızı: %.0f km/s", windSpeed)));
        textViewPressure.setText("Basınç: " + pressure + " hPa");
        windDirect.setRotation(winddirection+180);
        textViewCloudiness.setText("Bulut Oranı: %"+ cloudiness);
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM HH:mm", new Locale("tr", "TR"));
        String updateTime = sdf.format(new Date(timestamp));
        textViewLastUpdate.setText("Son güncelleme: " + updateTime);

        // Hava durumu ikonunu ayarlama
        if(icon != null) setWeatherIcon(icon,description);

    }

    private void setWeatherIcon(String icon,String description) {
        // İkon koduna göre drawable kaynak ayarlama
        int iconResource= UIUpdate.setWeatherIcon(icon,description);
        imageViewWeatherIcon.setImageResource(iconResource);
    }
    private void updateBackgroundByWeather(String iconCode,String description) {
     int backgroundRes= UIUpdate.updateBackgroundByWeather(iconCode,description);
            currentbackround=backgroundRes;
            root.setBackgroundResource(currentbackround);
            recyclerFavorites.setBackgroundResource(R.drawable.general_component_background);
    }
}