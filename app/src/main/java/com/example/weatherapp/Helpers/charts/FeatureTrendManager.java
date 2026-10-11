package com.example.weatherapp.Helpers.charts;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.example.weatherapp.Helpers.ForecastItem;
import com.example.weatherapp.Helpers.WeatherReceiver.MeteoStatParser;
import com.example.weatherapp.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FeatureTrendManager {

    private final View rootView;

    private CardView cardMetricTemp;
    private CardView cardMetricRain;
    private CardView cardMetricHumidity;
    private CardView cardMetricWind;
    private CardView cardMetricPressure;

    private TextView tvCardTempValue;
    private TextView tvCardRainValue;
    private TextView tvCardHumidityValue;
    private TextView tvCardWindValue;
    private TextView tvCardPressureValue;

    private TextView tvCardTempAction;
    private TextView tvCardRainAction;
    private TextView tvCardHumidityAction;
    private TextView tvCardWindAction;
    private TextView tvCardPressureAction;

    private CardView cardChartContainer;
    private TextView tvChartTitle;
    private TextView tvChartSummary;
    private TextView btnCloseChart;
    private PointTrendChartView pointTrendChartView;

    private String currentActiveMetric = null;

    private List<ForecastItem> forecastItems = null;
    private boolean isHourlyForecast = false;
    private List<MeteoStatParser.DailyWeather> pastDailyItems = null;

    public FeatureTrendManager(View container) {
        this.rootView = container;
        if (container != null) {
            initViews();
            setupListeners();
        }
    }

    private void initViews() {
        if (rootView == null) return;
        cardMetricTemp = rootView.findViewById(R.id.cardMetricTemp);
        cardMetricRain = rootView.findViewById(R.id.cardMetricRain);
        cardMetricHumidity = rootView.findViewById(R.id.cardMetricHumidity);
        cardMetricWind = rootView.findViewById(R.id.cardMetricWind);
        cardMetricPressure = rootView.findViewById(R.id.cardMetricPressure);

        tvCardTempValue = rootView.findViewById(R.id.tvCardTempValue);
        tvCardRainValue = rootView.findViewById(R.id.tvCardRainValue);
        tvCardHumidityValue = rootView.findViewById(R.id.tvCardHumidityValue);
        tvCardWindValue = rootView.findViewById(R.id.tvCardWindValue);
        tvCardPressureValue = rootView.findViewById(R.id.tvCardPressureValue);

        tvCardTempAction = rootView.findViewById(R.id.tvCardTempAction);
        tvCardRainAction = rootView.findViewById(R.id.tvCardRainAction);
        tvCardHumidityAction = rootView.findViewById(R.id.tvCardHumidityAction);
        tvCardWindAction = rootView.findViewById(R.id.tvCardWindAction);
        tvCardPressureAction = rootView.findViewById(R.id.tvCardPressureAction);

        cardChartContainer = rootView.findViewById(R.id.cardChartContainer);
        tvChartTitle = rootView.findViewById(R.id.tvChartTitle);
        tvChartSummary = rootView.findViewById(R.id.tvChartSummary);
        btnCloseChart = rootView.findViewById(R.id.btnCloseChart);
        pointTrendChartView = rootView.findViewById(R.id.pointTrendChartView);
    }

    private void setupListeners() {
        if (cardMetricTemp != null) {
            cardMetricTemp.setOnClickListener(v -> onMetricCardClicked("TEMP"));
        }
        if (cardMetricRain != null) {
            cardMetricRain.setOnClickListener(v -> onMetricCardClicked("RAIN"));
        }
        if (cardMetricHumidity != null) {
            cardMetricHumidity.setOnClickListener(v -> onMetricCardClicked("HUMIDITY"));
        }
        if (cardMetricWind != null) {
            cardMetricWind.setOnClickListener(v -> onMetricCardClicked("WIND"));
        }
        if (cardMetricPressure != null) {
            cardMetricPressure.setOnClickListener(v -> onMetricCardClicked("PRESSURE"));
        }
        if (btnCloseChart != null) {
            btnCloseChart.setOnClickListener(v -> closeChart());
        }
    }

    public void setForecastData(List<ForecastItem> items, boolean isHourly) {
        this.forecastItems = items;
        this.isHourlyForecast = isHourly;
        this.pastDailyItems = null;

        if (rootView == null) return;
        if (items == null || items.isEmpty()) {
            rootView.setVisibility(View.GONE);
            return;
        }

        double avgTemp = 0, maxRain = 0, avgHumidity = 0, avgWind = 0;
        int validCount = 0;
        for (ForecastItem item : items) {
            if (item == null || item.getDate() == null) continue;
            double t = item.getTemperature();
            if (Double.isNaN(t) || Double.isInfinite(t)) continue;

            avgTemp += t;

            double rainVol = item.getPreprainy();
            if (!Double.isNaN(rainVol) && !Double.isInfinite(rainVol) && rainVol > maxRain) {
                maxRain = rainVol;
            }
            avgHumidity += item.getHumidity();
            avgWind += item.getWindSpeed();
            validCount++;
        }

        if (validCount == 0) {
            rootView.setVisibility(View.GONE);
            return;
        }
        rootView.setVisibility(View.VISIBLE);

        avgTemp /= validCount;
        avgHumidity /= validCount;
        avgWind /= validCount;

        if (tvCardTempValue != null) {
            tvCardTempValue.setVisibility(View.GONE);
        }
        if (tvCardRainValue != null) {
            tvCardRainValue.setVisibility(View.GONE);
        }
        if (cardMetricHumidity != null) {
            cardMetricHumidity.setVisibility(View.VISIBLE);
        }
        if (tvCardHumidityValue != null) {
            tvCardHumidityValue.setVisibility(View.GONE);
        }
        if (tvCardWindValue != null) {
            tvCardWindValue.setVisibility(View.GONE);
        }
        if (cardMetricPressure != null) {
            cardMetricPressure.setVisibility(View.GONE);
        }

        highlightActiveCard(currentActiveMetric);

        if (currentActiveMetric != null) {
            showChartForMetric(currentActiveMetric);
        }
    }

    public void setPastStationData(List<MeteoStatParser.DailyWeather> items) {
        if (rootView == null) return;
        if (items == null || items.isEmpty()) {
            rootView.setVisibility(View.GONE);
            return;
        }

        List<MeteoStatParser.DailyWeather> cleanList = new ArrayList<>();
        for (MeteoStatParser.DailyWeather item : items) {
            if (item != null && item.date != null && !item.date.trim().isEmpty()) {
                cleanList.add(item);
            }
        }
        cleanList.sort((a, b) -> a.date.compareTo(b.date));
        this.pastDailyItems = cleanList;
        this.forecastItems = null;

        double totalTemp = 0, totalRain = 0, totalWind = 0, totalPres = 0;
        int validTempCount = 0;
        int validRainCount = 0;
        int validWindCount = 0;
        int validPresCount = 0;

        for (MeteoStatParser.DailyWeather item : cleanList) {
            if (item.maxTemperature != null && item.minTemperature != null) {
                totalTemp += (item.maxTemperature + item.minTemperature) / 2.0;
                validTempCount++;
            } else if (item.maxTemperature != null) {
                totalTemp += item.maxTemperature;
                validTempCount++;
            } else if (item.minTemperature != null) {
                totalTemp += item.minTemperature;
                validTempCount++;
            }

            if (item.precipitation != null) {
                totalRain += item.precipitation;
                validRainCount++;
            }
            if (item.windSpeed != null) {
                totalWind += item.windSpeed;
                validWindCount++;
            }
            if (item.pressure != null) {
                totalPres += item.pressure;
                validPresCount++;
            }
        }

        if (validTempCount == 0 && validRainCount == 0 && validWindCount == 0 && validPresCount == 0) {
            rootView.setVisibility(View.GONE);
            return;
        }
        rootView.setVisibility(View.VISIBLE);

        if (tvCardTempValue != null) {
            tvCardTempValue.setVisibility(View.GONE);
        }
        if (tvCardRainValue != null) {
            tvCardRainValue.setVisibility(View.GONE);
        }
        if (cardMetricHumidity != null) {
            cardMetricHumidity.setVisibility(View.GONE);
        }
        if (tvCardWindValue != null) {
            tvCardWindValue.setVisibility(View.GONE);
        }
        if (cardMetricPressure != null) {
            cardMetricPressure.setVisibility(View.VISIBLE);
            if (tvCardPressureValue != null) {
                tvCardPressureValue.setVisibility(View.GONE);
            }
        }

        highlightActiveCard(currentActiveMetric);

        if (currentActiveMetric != null) {
            showChartForMetric(currentActiveMetric);
        }
    }

    private void onMetricCardClicked(String metric) {
        if (metric == null) return;
        if (metric.equals(currentActiveMetric)) {
            closeChart();
        } else {
            showChartForMetric(metric);
        }
    }

    private void showChartForMetric(String metric) {
        if (metric == null) return;
        currentActiveMetric = metric;
        highlightActiveCard(metric);

        List<ChartDataPoint> points = new ArrayList<>();
        int color = Color.parseColor("#38BDF8");
        String title = "";
        String unit = "";

        if (forecastItems != null && !forecastItems.isEmpty()) {
            switch (metric) {
                case "TEMP":
                    color = Color.parseColor("#FB923C");
                    title = isHourlyForecast ? "Saatlik Sıcaklık Gidişatı (°C)" : "Günlük Sıcaklık Gidişatı (°C)";
                    unit = "°C";
                    for (ForecastItem item : forecastItems) {
                        if (item == null || item.getDate() == null || item.getDate().trim().isEmpty()) continue;
                        double val = item.getTemperature();
                        if (Double.isNaN(val) || Double.isInfinite(val)) continue;

                        String label = formatItemLabel(item.getDate(), isHourlyForecast);
                        points.add(new ChartDataPoint(label, (float) val, String.format(Locale.getDefault(), "%.0f°", val)));
                    }
                    break;
                case "RAIN":
                    color = Color.parseColor("#38BDF8"); // Açık mavi
                    title = isHourlyForecast ? "Saatlik Yağış Miktarı (mm)" : "Günlük Yağış Miktarı (mm)";
                    unit = " mm";
                    for (ForecastItem item : forecastItems) {
                        if (item == null || item.getDate() == null || item.getDate().trim().isEmpty()) continue;
                        double rainVol = item.getPreprainy();
                        double rainProb = item.getProbability();


                        float val = (float) (Double.isNaN(rainVol) ? 0 : rainVol);

                        String label = formatItemLabel(item.getDate(), isHourlyForecast);

                        String disp;
                        if (!Double.isNaN(rainVol) && rainVol > 0) {
                            disp = String.format(Locale.getDefault(), "%.1f mm", rainVol);
                        } else {
                            disp = String.format(Locale.getDefault(), "%.0f%%", Math.max(0, rainProb));
                        }
                        points.add(new ChartDataPoint(label, val, disp));
                    }
                    break;
                case "HUMIDITY":
                    color = Color.parseColor("#34D399");
                    title = "Bağıl Nem Değişimi (%)";
                    unit = "%";
                    for (ForecastItem item : forecastItems) {
                        if (item == null || item.getDate() == null || item.getDate().trim().isEmpty()) continue;
                        double val = item.getHumidity();
                        if (Double.isNaN(val) || Double.isInfinite(val)) continue;

                        String label = formatItemLabel(item.getDate(), isHourlyForecast);
                        points.add(new ChartDataPoint(label, (float) val, String.format(Locale.getDefault(), "%.0f%%", val)));
                    }
                    break;
                case "WIND":
                    color = Color.parseColor("#C084FC");
                    title = "Rüzgar Hızı Gidişatı (km/h)";
                    unit = " km/h";
                    for (ForecastItem item : forecastItems) {
                        if (item == null || item.getDate() == null || item.getDate().trim().isEmpty()) continue;
                        double val = item.getWindSpeed();
                        if (Double.isNaN(val) || Double.isInfinite(val)) continue;

                        String label = formatItemLabel(item.getDate(), isHourlyForecast);
                        points.add(new ChartDataPoint(label, (float) val, String.format(Locale.getDefault(), "%.0f km/h", val)));
                    }
                    break;
            }
        } else if (pastDailyItems != null && !pastDailyItems.isEmpty()) {
            switch (metric) {
                case "TEMP":
                    color = Color.parseColor("#FB923C");
                    title = "Geçmiş 7 Günlük Sıcaklık (°C)";
                    unit = "°C";
                    for (MeteoStatParser.DailyWeather item : pastDailyItems) {
                        if (item == null || item.date == null || item.date.trim().isEmpty()) continue;

                        if (item.maxTemperature == null && item.minTemperature == null) continue;

                        String label = formatPastDateLabel(item.date);
                        double val = (item.maxTemperature != null && item.minTemperature != null)
                                ? (item.maxTemperature + item.minTemperature) / 2.0
                                : (item.maxTemperature != null ? item.maxTemperature : item.minTemperature);
                        if (Double.isNaN(val) || Double.isInfinite(val)) continue;

                        points.add(new ChartDataPoint(label, (float) val, String.format(Locale.getDefault(), "%.0f°", val)));
                    }
                    break;
                case "RAIN":
                    color = Color.parseColor("#38BDF8");
                    title = "Geçmiş Yağış Miktarı (mm)";
                    unit = " mm";
                    for (MeteoStatParser.DailyWeather item : pastDailyItems) {
                        if (item == null || item.date == null || item.date.trim().isEmpty()) continue;

                        if (item.precipitation == null) continue;
                        if (Double.isNaN(item.precipitation) || Double.isInfinite(item.precipitation)) continue;

                        String label = formatPastDateLabel(item.date);
                        double val = item.precipitation;
                        points.add(new ChartDataPoint(label, (float) val, String.format(Locale.getDefault(), "%.1f mm", val)));
                    }
                    break;
                case "WIND":
                    color = Color.parseColor("#C084FC");
                    title = "Geçmiş Rüzgar Hızı (km/h)";
                    unit = " km/h";
                    for (MeteoStatParser.DailyWeather item : pastDailyItems) {
                        if (item == null || item.date == null || item.date.trim().isEmpty()) continue;

                        if (item.windSpeed == null) continue;
                        if (Double.isNaN(item.windSpeed) || Double.isInfinite(item.windSpeed)) continue;

                        String label = formatPastDateLabel(item.date);
                        double val = item.windSpeed;
                        points.add(new ChartDataPoint(label, (float) val, String.format(Locale.getDefault(), "%.0f km/h", val)));
                    }
                    break;
                case "PRESSURE":
                    color = Color.parseColor("#FBBF24");
                    title = "Geçmiş Deniz Seviyesi Basıncı (hPa)";
                    unit = " hPa";
                    for (MeteoStatParser.DailyWeather item : pastDailyItems) {
                        if (item == null || item.date == null || item.date.trim().isEmpty()) continue;

                        if (item.pressure == null) continue;
                        if (Double.isNaN(item.pressure) || Double.isInfinite(item.pressure)) continue;

                        String label = formatPastDateLabel(item.date);
                        double val = item.pressure;
                        points.add(new ChartDataPoint(label, (float) val, String.format(Locale.getDefault(), "%.0f hPa", val)));
                    }
                    break;
            }
        }

        if (points.isEmpty()) {
            closeChart();
            if (rootView != null && rootView.getContext() != null) {
                Toast.makeText(rootView.getContext(), "Bu parametre için o tarihlerde ölçüm verisi bulunmamaktadır.", Toast.LENGTH_SHORT).show();
            }
            return;
        }


        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        float sum = 0;
        int validPts = 0;
        for (ChartDataPoint p : points) {
            if (p == null) continue;
            if (p.value < min) min = p.value;
            if (p.value > max) max = p.value;
            sum += p.value;
            validPts++;
        }
        if (validPts == 0) {
            closeChart();
            return;
        }
        float avg = sum / validPts;

        if (tvChartTitle != null) {
            tvChartTitle.setText(title);
        }
        if (tvChartSummary != null) {
            tvChartSummary.setText(String.format(Locale.getDefault(), "En Düşük: %.1f%s  |  En Yüksek: %.1f%s  |  Ortalama: %.1f%s", min, unit, max, unit, avg, unit));
        }

        if (pointTrendChartView != null) {
            pointTrendChartView.setData(points, color);
        }

        if (cardChartContainer != null) {
            cardChartContainer.animate().cancel();
            cardChartContainer.setVisibility(View.VISIBLE);
            cardChartContainer.setAlpha(0f);
            cardChartContainer.animate().alpha(1f).setDuration(200).start();

            if (rootView != null) {
                android.widget.HorizontalScrollView scrollChart = rootView.findViewById(R.id.scrollChart);
                if (scrollChart != null) {
                    scrollChart.post(() -> scrollChart.scrollTo(0, 0));
                }
            }
        }
    }

    private void closeChart() {
        currentActiveMetric = null;
        highlightActiveCard(null);
        if (cardChartContainer != null) {
            cardChartContainer.animate().cancel();
            cardChartContainer.animate().alpha(0f).setDuration(180).withEndAction(() -> {
                cardChartContainer.setVisibility(View.GONE);
            }).start();
        }
    }

    private void highlightActiveCard(String activeMetric) {
        int defaultBg = Color.parseColor("#1E293B");
        int activeBg = Color.parseColor("#0369A1");
        int inactiveActionColor = Color.parseColor("#94A3B8");
        int activeActionColor = Color.parseColor("#FFFFFF");

        updateCardState(cardMetricTemp, tvCardTempAction, "TEMP".equals(activeMetric), defaultBg, activeBg, inactiveActionColor, activeActionColor);
        updateCardState(cardMetricRain, tvCardRainAction, "RAIN".equals(activeMetric), defaultBg, activeBg, inactiveActionColor, activeActionColor);
        updateCardState(cardMetricHumidity, tvCardHumidityAction, "HUMIDITY".equals(activeMetric), defaultBg, activeBg, inactiveActionColor, activeActionColor);
        updateCardState(cardMetricWind, tvCardWindAction, "WIND".equals(activeMetric), defaultBg, activeBg, inactiveActionColor, activeActionColor);
        updateCardState(cardMetricPressure, tvCardPressureAction, "PRESSURE".equals(activeMetric), defaultBg, activeBg, inactiveActionColor, activeActionColor);
    }

    private void updateCardState(CardView card, TextView actionText, boolean isActive, int defaultBg, int activeBg, int inactiveActionColor, int activeActionColor) {
        if (card != null) {
            card.setCardBackgroundColor(isActive ? activeBg : defaultBg);
        }
        if (actionText != null) {
            actionText.setText(isActive ? "Grafiği Gizle" : "Grafiği Göster");
            actionText.setTextColor(isActive ? activeActionColor : inactiveActionColor);
        }
    }

    private String formatItemLabel(String rawDate, boolean isHourly) {
        if (rawDate == null || rawDate.trim().isEmpty()) return "--";
        try {
            if (isHourly) {

                if (rawDate.contains("T")) {
                    return formatStandardHourlyLabel(rawDate.replace("T", " "));
                } else if (rawDate.matches(".*\\d{4}-\\d{2}-\\d{2}.*")) {
                    return formatStandardHourlyLabel(rawDate);
                } else if (rawDate.contains(".") || rawDate.contains(":")) {
                    return formatOwmHourlyLabel(rawDate);
                }
                return rawDate;
            } else {

                if (rawDate.matches(".*\\d{4}-\\d{2}-\\d{2}.*")) {
                    String cleanDate = rawDate.contains(" ") ? rawDate.split(" ")[0] : rawDate;
                    SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Date date = parser.parse(cleanDate);
                    if (date != null) {
                        SimpleDateFormat formatter = new SimpleDateFormat("dd MMM", new Locale("tr", "TR"));
                        return formatter.format(date);
                    }
                } else if (rawDate.contains(" - ")) {

                    String[] parts = rawDate.split(" ");
                    if (parts.length >= 2) {
                        return parts[0] + " " + parts[1];
                    }
                }
            }
        } catch (Exception ignored) {}
        return rawDate;
    }

    private String formatStandardHourlyLabel(String dateTimeStr) {
        try {

            String[] parts = dateTimeStr.split(" ");
            String datePart = parts[0];
            String timePart = parts.length > 1 ? parts[1] : "";
            if (timePart.length() > 5) timePart = timePart.substring(0, 5);

            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date d = parser.parse(datePart);
            if (d != null) {
                SimpleDateFormat dayFormat = new SimpleDateFormat("dd MMM", new Locale("tr", "TR"));
                String dayStr = dayFormat.format(d);
                if (!timePart.isEmpty()) {
                    return timePart + "\n" + dayStr;
                }
                return dayStr;
            }
        } catch (Exception ignored) {}
        return dateTimeStr;
    }

    private String formatOwmHourlyLabel(String rawDate) {
        try {

            String[] parts = rawDate.split(" ");
            if (parts.length >= 3) {
                String dayStr = parts[0] + " " + parts[1];
                String timeStr = parts[2].replace(".", ":");
                if (timeStr.length() > 5) timeStr = timeStr.substring(0, 5);
                return timeStr + "\n" + dayStr;
            }
        } catch (Exception ignored) {}
        return rawDate;
    }

    private String formatPastDateLabel(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) return "--";
        try {
            String cleanDate = rawDate.contains(" ") ? rawDate.split(" ")[0] : rawDate;
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = parser.parse(cleanDate);
            if (date != null) {
                SimpleDateFormat formatter = new SimpleDateFormat("dd MMM", new Locale("tr", "TR"));
                return formatter.format(date);
            }
        } catch (Exception ignored) {}
        return rawDate;
    }
}

