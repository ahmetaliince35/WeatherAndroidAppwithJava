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

import com.example.weatherapp.Helpers.UIUpdate;

import java.util.List;
import java.util.Locale;

public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder> {

    private static final int hourlyactivitymode=0;
    private static final int dailyactivitymode=1;
    private int mode;
    private Context context;
    private List<ForecastItem> forecastList;

    public ForecastAdapter(Context context, List<ForecastItem> forecastList,int mode) {
        this.context = context;
        this.forecastList = forecastList;
        this.mode=mode;
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
        String icon=item.getIcon();
        holder.textViewDescription.setText(description.substring(0, 1).toUpperCase() +
                description.substring(1));

        holder.textViewHumidity.setText(String.format(Locale.getDefault(), "Nem: %% %.0f",item.getHumidity()));
        holder.textViewWind.setText(String.format(Locale.getDefault(), "Rüzgar hızı: %.0fkm/s", item.getWindSpeed()));
        if(description.contains("kar") || item.getTemperature()<=0)
        {
            holder.raindaily.setText(String.format("Yağış: %.2f cm", item.getPreprainy()));
        }
        else
        {
            holder.raindaily.setText(String.format("Yağış: %.2f mm", item.getPreprainy()));
        }
        // Yağış ihtimali
            holder.rainprobability.setVisibility(View.VISIBLE);
            holder.rainprobability.setText(String.format("İhtimal: %.0f%%", item.getProbability()));

        // İkon ayarlama
        setTextViewsColor(holder,item);
        setWeatherIcon(holder.imageViewIcon,icon,description );
    }

    @Override
    public int getItemCount() {
        return forecastList.size();
    }

    public void setWeatherIcon(ImageView imageView, String icon,String description) {
        int iconResource=UIUpdate.setWeatherIcon(icon,description);
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

    public void setTextViewsColor(ForecastViewHolder holder,ForecastItem item )
    {
        double rainProbability=item.getProbability();
        double temperature= item.getTemperature();
        double humidity= item.getHumidity();
        double totalRain= item.getPreprainy();
        double windSpeed= item.getWindSpeed();
        //Yağış olasılığı için renk skalası oluşturuldu.
        // Günlük ve 3 saatlik yağış miktarları için renk skalası oluşturuldu.
        if(mode==dailyactivitymode)
        {
            holder.raindaily.setTextColor(ContextCompat.getColor(context,
                    setLevelColor(totalRain,25,5,0,false)));
        } else if (mode==hourlyactivitymode) {
            holder.textViewWind.setTextColor(ContextCompat.getColor(context,
                    setLevelColor(totalRain,5,2,0,false)));
        }
        holder.rainprobability.setTextColor(ContextCompat.getColor(context,
                setLevelColor(rainProbability,70,40,0,false)));

        holder.textViewWind.setTextColor(ContextCompat.getColor(context,
                setLevelColor(windSpeed,25,15,0,false)));
        // Nem değerleri için renk skalası oluşturuldu.
        holder.textViewHumidity.setTextColor(ContextCompat.getColor(context,
                setLevelColor(humidity,90,60,30,false)));
        // Sıcaklık değerleri için renk skalası oluşturuldu.
        holder.textViewTemp.setTextColor(ContextCompat.getColor(context,
                setLevelColor(temperature,30,15,0,true)));
    }
    public int setLevelColor( double value,double max_value,double medium_value,double min_value,boolean istemperature)
    {
        int colorRes;
        if(istemperature==false)
        {
            if (value > max_value) {
                colorRes = R.color.prob_high;
            } else if (value > medium_value) {
                colorRes = R.color.prob_medium;
            } else if (value > min_value) {
                colorRes = R.color.prob_low;
            } else {
                colorRes = R.color.prob_none;
            }
        }
        else
        {
            if (value > max_value) {
                colorRes = R.color.prob_high;
            } else if (value > medium_value) {
                colorRes = R.color.prob_medium;
            } else if (value > min_value) {
                colorRes = R.color.prob_none;
            } else {
                colorRes = R.color.prob_low;
            }
        }

        return colorRes;
    }
}
