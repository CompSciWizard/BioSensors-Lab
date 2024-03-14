package com.example.finishble;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GraphsUtil {

    private static final long startTime = System.currentTimeMillis() / 1000;
    private static final int MAX_BUFFER_SIZE = 1000;
    private static final long PLOT_INTERVAL = 250;
    private static final Map<String, LineDataSet> sensorDataSetMap = new HashMap<>();
    private static final Map<String, List<Entry>> sensorDataBufferMap = new HashMap<>();

    public static void createGraphSeries(LineChart chart, String sensorKey, int lineColor) {
        LineDataSet lineDataSet = new LineDataSet(new ArrayList<>(), "Polar H10 Device Heart Rate");

        lineDataSet.setColor(lineColor);
        lineDataSet.setCircleColor(lineColor);
        lineDataSet.setValueTextColor(lineColor); // if you want to hide values
        lineDataSet.setLineWidth(1.5f);
        lineDataSet.setDrawValues(false);
        lineDataSet.setDrawCircles(false);

        if (!chart.getData().getDataSets().contains(lineDataSet)) {
            chart.getData().addDataSet(lineDataSet);
        }

        chart.getDescription().setEnabled(false);
        chart.getAxisLeft().setDrawGridLines(false);
        chart.getXAxis().setDrawGridLines(false);
        chart.setTouchEnabled(false);
        chart.setPinchZoom(false);
        // Set X-axis properties
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.TOP); // Position labels at the top
        //xAxis.setTextColor(Color.WHITE); // Set label colors to white
        // Disable right Y-axis
        chart.getAxisRight().setEnabled(false); // Only show Y-axis values on the left

        xAxis.setDrawGridLines(false); // Optionally, remove grid lines for a cleaner look
        xAxis.setValueFormatter(new TimeValueFormatter());
        sensorDataSetMap.put(sensorKey, lineDataSet);
        sensorDataBufferMap.put(sensorKey, new ArrayList<>());

        startPlotTimer(chart, sensorKey);
    }

    // Define a custom ValueFormatter to format the X-axis values as time
    private static class TimeValueFormatter extends ValueFormatter {
        @Override
        public String getFormattedValue(float value) {
            if (value < 60) {
                // Less than a minute, display in seconds
                return String.format(Locale.getDefault(), "%ds", (int) value);
            } else if (value < 3600) {
                // Less than an hour, display in minutes and seconds
                int minutes = (int) (value / 60);
                int seconds = (int) (value % 60);
                return String.format(Locale.getDefault(), "%dm:%ds", minutes, seconds);
            } else {
                // Hours or more, display in hours and minutes
                int hours = (int) (value / 3600);
                int minutes = (int) ((value % 3600) / 60);
                return String.format(Locale.getDefault(), "%dhr:%dm", hours, minutes);
            }
        }
    }

    public static void addDataPoint(String sensorKey, float value) {
        double currentTime = System.currentTimeMillis() / 1000.0 - startTime;
        Entry newEntry = new Entry((float) currentTime, value);

        // Retrieve the dataset for the sensorKey. Assume it's been previously created.
        LineDataSet dataSet = sensorDataSetMap.get(sensorKey);
        if (dataSet != null) {
            // Add the entry to the dataset
            dataSet.addEntry(newEntry);

            // Sort the entries. It's important for MPAndroidChart to have entries sorted by their x-value.
            dataSet.getValues().sort((entry1, entry2) -> Float.compare(entry1.getX(), entry2.getX()));
        }
    }

    private static void discardOldestDataPoints(String sensorKey) {
        List<Entry> dataBuffer = sensorDataBufferMap.get(sensorKey);
        assert dataBuffer != null;
        while (dataBuffer.size() > MAX_BUFFER_SIZE) {
            dataBuffer.remove(0);
        }
    }

    private static void startPlotTimer(final LineChart chart, String sensorKey) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> plotDataPoints(chart, sensorKey), 0, PLOT_INTERVAL, TimeUnit.MILLISECONDS);
    }

    public static void plotDataPoints(LineChart chart, String sensorKey) {
        LineDataSet dataSet = sensorDataSetMap.get(sensorKey);
        if (dataSet != null && !dataSet.getValues().isEmpty()) {
            chart.getData().notifyDataChanged();
            chart.notifyDataSetChanged(); // Let the chart know it's data changed
            chart.invalidate(); // Refresh the chart
        }
    }


    private static Handler getMainHandler() {
        return new Handler(Looper.getMainLooper());
    }
}
