package com.example.weatherapp.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weatherapp.Helpers.WeatherReceiver.MeteoStatParser.DailyWeather;
import com.example.weatherapp.R;

import java.util.ArrayList;
import java.util.List;

public class pastDailyDataAdapter extends RecyclerView.Adapter<pastDailyDataAdapter.WeatherViewHolder> {

    private final List<DailyWeather> weatherList = new ArrayList<>();
    private String stationName = "";
    private String stationCountry = "";

    public void updateData(List<DailyWeather> newList, String stationName, String stationCountry) {
        this.weatherList.clear();
        if (newList != null) {
            this.weatherList.addAll(newList);
        }
        this.stationName = stationName;
        this.stationCountry = stationCountry;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public WeatherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_past_daily, parent, false);
        return new WeatherViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WeatherViewHolder holder, int position) {
        DailyWeather weather = weatherList.get(position);

        // Tarih formatını sadece YYYY-MM-DD yap (00:00 veya saat bilgisini temizle)
        String cleanDate = "";
        if (weather.date != null) {
            cleanDate = weather.date.split(" ")[0]; // "2026-08-11 00:00:00" -> "2026-08-11"
        }

        // Başlıkta sadece İstasyon Adı ve Sadece Tarih göster
        holder.tvStationName.setText(stationName + " (" + cleanDate + ")");
        holder.tvStationCountry.setText(stationCountry);

        holder.tvPrecipitation.setText(weather.precipitation != null ? weather.precipitation + " mm" : "-- mm");
        holder.tvPressure.setText(weather.pressure != null ? weather.pressure + " hPa" : "-- hPa");
        holder.tvMinTemperature.setText(weather.minTemperature != null ? weather.minTemperature + " °C" : "-- °C");
        holder.tvMaxTemperature.setText(weather.maxTemperature != null ? weather.maxTemperature + " °C" : "-- °C");
        holder.tvWindSpeed.setText(weather.windSpeed != null ? weather.windSpeed + " km/h" : "-- km/h");
    }

    @Override
    public int getItemCount() {
        return weatherList.size();
    }

    public static class WeatherViewHolder extends RecyclerView.ViewHolder {
        TextView tvStationName, tvStationCountry, tvPrecipitation, tvPressure, tvMinTemperature, tvMaxTemperature, tvWindSpeed;

        public WeatherViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStationName = itemView.findViewById(R.id.tvStationName);
            tvStationCountry = itemView.findViewById(R.id.tvStationCountry);
            tvPrecipitation = itemView.findViewById(R.id.tvPrecipitation);
            tvPressure = itemView.findViewById(R.id.tvPressure);
            tvMinTemperature = itemView.findViewById(R.id.tvMinTemperature);
            tvMaxTemperature = itemView.findViewById(R.id.tvMaxTemperature);
            tvWindSpeed = itemView.findViewById(R.id.tvWindSpeed);
        }
    }
}