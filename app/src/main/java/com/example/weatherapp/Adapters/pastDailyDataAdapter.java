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
        if (weatherList == null || position < 0 || position >= weatherList.size()) return;
        DailyWeather weather = weatherList.get(position);
        if (weather == null) return;

        String cleanDate = "--";
        if (weather.date != null && !weather.date.trim().isEmpty()) {
            cleanDate = weather.date.contains(" ") ? weather.date.split(" ")[0] : weather.date;
            try {
                java.text.SimpleDateFormat parser = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                java.util.Date d = parser.parse(cleanDate);
                if (d != null) {
                    java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("dd MMM yyyy", new java.util.Locale("tr", "TR"));
                    cleanDate = fmt.format(d);
                }
            } catch (Exception ignored) {}
        }

        String safeStationName = stationName != null ? stationName : "";
        String safeStationCountry = stationCountry != null ? stationCountry : "";

        holder.tvStationName.setText(safeStationName + " (" + cleanDate + ")");
        holder.tvStationCountry.setText(safeStationCountry);

        holder.tvPrecipitation.setText(weather.precipitation != null ? String.format(java.util.Locale.getDefault(), "%.1f mm", weather.precipitation) : "-- mm");
        holder.tvPressure.setText(weather.pressure != null ? String.format(java.util.Locale.getDefault(), "%.0f hPa", weather.pressure) : "-- hPa");
        holder.tvMinTemperature.setText(weather.minTemperature != null ? String.format(java.util.Locale.getDefault(), "%.1f °C", weather.minTemperature) : "-- °C");
        holder.tvMaxTemperature.setText(weather.maxTemperature != null ? String.format(java.util.Locale.getDefault(), "%.1f °C", weather.maxTemperature) : "-- °C");
        holder.tvWindSpeed.setText(weather.windSpeed != null ? String.format(java.util.Locale.getDefault(), "%.1f km/h", weather.windSpeed) : "-- km/h");
    }

    @Override
    public int getItemCount() {
        return weatherList != null ? weatherList.size() : 0;
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