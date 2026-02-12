package com.example.weatherapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder> {

    private Context context;
    private List<ForecastItem> forecastList;

    public ForecastAdapter(Context context, List<ForecastItem> forecastList) {
        this.context = context;
        this.forecastList = forecastList;
    }

    @NonNull
    @Override
    public ForecastViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_forecast, parent, false);
        return new ForecastViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ForecastViewHolder holder, int position) {
        ForecastItem item = forecastList.get(position);

        holder.textViewDate.setText(item.getDate());
        holder.textViewTemp.setText(String.format("%.0f°C", item.getTemperature()));
        holder.textViewTempRange.setText(String.format("Min: %.0f°C / Max: %.0f°C",
                item.getTempMin(), item.getTempMax()));

        String description = item.getDescription();
        holder.textViewDescription.setText(description.substring(0, 1).toUpperCase() +
                description.substring(1));

        holder.textViewHumidity.setText(String.format(Locale.getDefault(), "Nem:%d%%",item.getHumidity()));
        holder.textViewWind.setText(String.format(Locale.getDefault(), "Rüzgar hızı: %.0f km/s", item.getWindSpeed()));
        holder.raindaily.setText(String.format("🌧 Yağış: %.2f mm", item.getPreprainy()));
        // Yağış ihtimali
            holder.rainprobability.setVisibility(View.VISIBLE);
            holder.rainprobability.setText(String.format("☔ İhtimal: %.0f%%", item.getProbability()));

            int colorRes;
            // Yüksek ihtimalde vurgula
            if (item.getProbability() >= 70) {
                colorRes=R.color.prob_high;
            } else if (item.getProbability() >= 40) {
                colorRes=R.color.prob_medium;
            } else if (item.getProbability()>0) {
                colorRes=R.color.prob_low;
            }
            else{
                colorRes=R.color.prob_none;
            }
            holder.rainprobability.setTextColor(ContextCompat.getColor(context,colorRes));
        // İkon ayarlama
        setWeatherIcon(holder.imageViewIcon, item.getIcon());
    }

    @Override
    public int getItemCount() {
        return forecastList.size();
    }

    private void setWeatherIcon(ImageView imageView, String icon) {
        int iconResource;
        switch (icon) {
            case "01d":
                iconResource = R.drawable.icon_sunny;
                break;
            case "01n":
                iconResource=R.drawable.icon_moon;
                break;
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
        imageView.setImageResource(iconResource);
    }

    static class ForecastViewHolder extends RecyclerView.ViewHolder {
        TextView textViewDate;
        TextView textViewTemp;
        TextView textViewTempRange;
        TextView textViewDescription;
        TextView textViewHumidity;
        TextView textViewWind;
        ImageView imageViewIcon;
        TextView raindaily;

        TextView rainprobability;

        public ForecastViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewDate = itemView.findViewById(R.id.textViewDate);
            textViewTemp = itemView.findViewById(R.id.textViewTemp);
            textViewTempRange = itemView.findViewById(R.id.textViewTempRange);
            textViewDescription = itemView.findViewById(R.id.textViewDescription);
            textViewHumidity = itemView.findViewById(R.id.textViewHumidity);
            textViewWind = itemView.findViewById(R.id.textViewWind);
            imageViewIcon = itemView.findViewById(R.id.imageViewIcon);
            raindaily=itemView.findViewById(R.id.textViewRain);
            rainprobability=itemView.findViewById(R.id.textViewRainProbability);
        }
    }
}
