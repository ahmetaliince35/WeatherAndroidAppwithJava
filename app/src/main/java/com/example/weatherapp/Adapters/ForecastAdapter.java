package com.example.weatherapp.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weatherapp.Helpers.ForecastItem;
import com.example.weatherapp.Helpers.UIUpdate;
import com.example.weatherapp.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder> {

    private static final int hourlyactivitymode = 0;
    private static final int dailyactivitymode = 1;
    private int mode;
    private Context context;
    private List<ForecastItem> forecastList;

    public ForecastAdapter(Context context, List<ForecastItem> forecastList, int mode) {
        this.context = context;
        this.forecastList = forecastList;
        this.mode = mode;
    }

    @NonNull
    @Override
    public ForecastViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_forecast, parent, false);
        return new ForecastViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ForecastViewHolder holder, int position) {
        if (forecastList == null || position < 0 || position >= forecastList.size()) return;
        ForecastItem item = forecastList.get(position);
        if (item == null) return;

        holder.textViewDate.setText(formatForecastDate(item.getDate(), mode));
        holder.textViewTemp.setText(context.getString(R.string.temperature_label, item.getTemperature()));
        holder.textViewTempRange.setText(context.getString(R.string.temp_range_label, item.getTempMin(), item.getTempMax()));

        String description = item.getDescription();
        if (description == null || description.trim().isEmpty()) {
            description = "Parçalı Bulutlu";
        }

        String icon = item.getIcon() != null ? item.getIcon() : "";

        String formattedDesc;
        if (description.length() > 1) {
            formattedDesc = description.substring(0, 1).toUpperCase(new Locale("tr", "TR")) +
                    description.substring(1).toLowerCase(new Locale("tr", "TR"));
        } else {
            formattedDesc = description.toUpperCase(new Locale("tr", "TR"));
        }
        holder.textViewDescription.setText(formattedDesc);

        holder.textViewHumidity.setText(context.getString(R.string.humidity_label, item.getHumidity()));
        holder.textViewWind.setText(context.getString(R.string.wind_speed_label, item.getWindSpeed()));
        if (holder.textViewPressure != null) {
            holder.textViewPressure.setText(context.getString(R.string.pressure_label, item.getPressure()));
        }
        if (holder.textViewCloudiness != null) {
            holder.textViewCloudiness.setText(context.getString(R.string.cloudiness_label, item.getCloudiness()));
        }

        String descLower = description.toLowerCase(new Locale("tr", "TR"));
        if (descLower.contains("kar") || item.getTemperature() <= 0) {
            holder.raindaily.setText(context.getString(R.string.rain_cm_label, item.getPreprainy()));
        } else {
            holder.raindaily.setText(context.getString(R.string.rain_mm_label, item.getPreprainy()));
        }

        holder.rainprobability.setVisibility(View.VISIBLE);
        holder.rainprobability.setText(context.getString(R.string.probability_label, item.getProbability()));

        if (holder.layoutDetails != null) {
            ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
            if (lp != null && lp.width > 0 && lp.width < 500) {
                 holder.layoutDetails.setVisibility(View.GONE);
            } else {
                 holder.layoutDetails.setVisibility(View.VISIBLE);
            }
        }

        setTextViewsColor(holder, item);
        setWeatherIcon(holder.imageViewIcon, icon, description);
        if (holder.imageViewCardBackground != null) {
            int bgRes = UIUpdate.updateBackgroundByWeather(icon, description);
            holder.imageViewCardBackground.setImageResource(bgRes);
        }
    }

    @Override
    public int getItemCount() {
        return forecastList != null ? forecastList.size() : 0;
    }

    public void setWeatherIcon(ImageView imageView, String icon, String description) {
        if (imageView == null) return;
        int iconResource = UIUpdate.setWeatherIcon(icon, description);
        imageView.setImageResource(iconResource);
    }

    static class ForecastViewHolder extends RecyclerView.ViewHolder {
        TextView textViewDate;
        TextView textViewTemp;
        TextView textViewTempRange;
        TextView textViewDescription;
        TextView textViewHumidity;
        TextView textViewWind;
        TextView textViewPressure;
        TextView textViewCloudiness;
        ImageView imageViewIcon;
        ImageView imageViewCardBackground;
        TextView raindaily;
        TextView rainprobability;
        View layoutDetails;

        public ForecastViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewDate = itemView.findViewById(R.id.textViewDate);
            textViewTemp = itemView.findViewById(R.id.textViewTemp);
            textViewTempRange = itemView.findViewById(R.id.textViewTempRange);
            textViewDescription = itemView.findViewById(R.id.textViewDescription);
            textViewHumidity = itemView.findViewById(R.id.textViewHumidity);
            textViewWind = itemView.findViewById(R.id.textViewWind);
            textViewPressure = itemView.findViewById(R.id.textViewPressure);
            textViewCloudiness = itemView.findViewById(R.id.textViewCloudiness);
            imageViewIcon = itemView.findViewById(R.id.imageViewIcon);
            imageViewCardBackground = itemView.findViewById(R.id.imageViewCardBackground);
            raindaily = itemView.findViewById(R.id.textViewRain);
            rainprobability = itemView.findViewById(R.id.textViewRainProbability);
            layoutDetails = itemView.findViewById(R.id.layoutDetails);
        }
    }

    public void setTextViewsColor(ForecastViewHolder holder, ForecastItem item) {
        if (holder == null || item == null || context == null) return;
        double rainProbability = item.getProbability();
        double temperature = item.getTemperature();
        double humidity = item.getHumidity();
        double totalRain = item.getPreprainy();
        double windSpeed = item.getWindSpeed();

        if (mode == dailyactivitymode) {
            holder.raindaily.setTextColor(ContextCompat.getColor(context,
                    setLevelColor(totalRain, 25, 5, 0, false)));
        } else if (mode == hourlyactivitymode) {
            holder.textViewWind.setTextColor(ContextCompat.getColor(context,
                    setLevelColor(totalRain, 5, 2, 0, false)));
        }
        holder.rainprobability.setTextColor(ContextCompat.getColor(context,
                setLevelColor(rainProbability, 70, 40, 0, false)));

        holder.textViewWind.setTextColor(ContextCompat.getColor(context,
                setLevelColor(windSpeed, 25, 15, 0, false)));

        holder.textViewHumidity.setTextColor(ContextCompat.getColor(context,
                setLevelColor(humidity, 90, 60, 30, false)));

        holder.textViewTemp.setTextColor(ContextCompat.getColor(context,
                setLevelColor(temperature, 30, 15, 0, true)));
    }

    public int setLevelColor(double value, double max_value, double medium_value, double min_value, boolean istemperature) {
        int colorRes;
        if (!istemperature) {
            if (value > max_value) {
                colorRes = R.color.prob_high;
            } else if (value > medium_value) {
                colorRes = R.color.prob_medium;
            } else if (value > min_value) {
                colorRes = R.color.prob_low;
            } else {
                colorRes = R.color.prob_none;
            }
        } else {
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

    private String formatForecastDate(String rawDate, int mode) {
        if (rawDate == null || rawDate.trim().isEmpty()) return "--";
        try {
            if (mode == hourlyactivitymode) {
                String clean = rawDate;
                if (clean.contains("T")) {
                    clean = clean.replace("T", " ");
                }
                if (clean.matches(".*\\d{4}-\\d{2}-\\d{2}.*")) {
                    String[] parts = clean.split(" ");
                    String datePart = parts[0];
                    String timePart = parts.length > 1 ? parts[1] : "";
                    if (timePart.length() > 5) timePart = timePart.substring(0, 5);

                    SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Date d = parser.parse(datePart);
                    if (d != null) {
                        SimpleDateFormat formatter = new SimpleDateFormat("dd MMMM - EEEE", new Locale("tr", "TR"));
                        String dateFormatted = formatter.format(d);
                        if (!timePart.isEmpty()) {
                            if (dateFormatted.contains(" - ")) {
                                String[] dateParts = dateFormatted.split(" - ");
                                return dateParts[0] + " " + timePart + " - " + dateParts[1];
                            }
                            return dateFormatted + " " + timePart;
                        }
                        return dateFormatted;
                    }
                } else if (clean.contains(".")) {
                    return clean.replace(".", ":");
                }
                return rawDate;
            } else {
                if (rawDate.matches(".*\\d{4}-\\d{2}-\\d{2}.*")) {
                    String cleanDate = rawDate.contains(" ") ? rawDate.split(" ")[0] : rawDate;
                    SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Date d = parser.parse(cleanDate);
                    if (d != null) {
                        SimpleDateFormat formatter = new SimpleDateFormat("dd MMMM - EEEE", new Locale("tr", "TR"));
                        return formatter.format(d);
                    }
                } else if (rawDate.contains(" - ")) {
                    String[] parts = rawDate.split(" - ");
                    String firstPart = parts[0];
                    String dayName = parts[1];
                    String cleanFirst = firstPart.replaceAll("\\s+\\d{2}[.:]\\d{2}", "");
                    return cleanFirst + " - " + dayName;
                }
            }
        } catch (Exception ignored) {}
        return rawDate;
    }
}