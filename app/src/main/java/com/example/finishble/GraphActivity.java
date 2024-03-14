package com.example.finishble;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class GraphActivity extends AppCompatActivity {

    // Define a map to store sensor values by sensor key
    private Map<String, String> sensorDataMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Retrieve data from the intent extras
        Intent intent = getIntent();
        ArrayList<String> sensorUUIDs = intent.getStringArrayListExtra("SENSOR_UUIDS");

        // Create and populate graphs based on the sensorUUIDs
        assert sensorUUIDs != null;
        createAndPopulateGraphs(sensorUUIDs);
    }

    private void createAndPopulateGraphs(ArrayList<String> sensorUUIDs) {
        // Assuming your layout file has been updated to use a LineChart with the ID R.id.line_chart
        LineChart lineChart = findViewById(R.id.line_chart); // Update this ID based on your layout
        lineChart.setData(new LineData()); // Initialize the chart with empty LineData


        // Create and populate graphs based on sensorUUIDs
        for (String uuid : sensorUUIDs) {
            int randomColor = generateRandomColor();
            GraphsUtil.createGraphSeries(lineChart, uuid, randomColor);

            // Create a legend item
          //  createLegendItem(legendLayout, "Sensor " + (sensorUUIDs.indexOf(uuid) + 1), randomColor, uuid);
        }
    }

    private void createLegendItem(LinearLayout legendLayout, String sensorName, int color, String sensorKey) {
        // Layout creation and setup are the same, no changes required here
        LinearLayout legendItemLayout = new LinearLayout(this);
        legendItemLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        legendItemLayout.setOrientation(LinearLayout.HORIZONTAL);
        legendItemLayout.setPadding(8, 8, 8, 8);

        // Colored box
        View colorBox = new View(this);
        colorBox.setLayoutParams(new LinearLayout.LayoutParams(20, 20));
        colorBox.setBackgroundColor(color);

        // Sensor name
        TextView sensorNameTextView = new TextView(this);
        sensorNameTextView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        sensorNameTextView.setText(sensorName);

        // Add the colored box and sensor name to the legend item layout
        legendItemLayout.addView(colorBox);
        legendItemLayout.addView(sensorNameTextView);

        // Add the legend item to the legend layout
        legendLayout.addView(legendItemLayout);

        // Store the sensor data in the map
        sensorDataMap.put(sensorKey, "");
    }

    // Modify this method to update the stored sensor data and display it
    private void displayReceivedData(String data, String sensorKey) {
        // Update the stored sensor data
        sensorDataMap.put(sensorKey, data);

    }

    private final BroadcastReceiver dataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (MyBleManager.ACTION_DATA_AVAILABLE.equals(action)) {
                String data = intent.getStringExtra(MyBleManager.EXTRA_DATA);
                String sensorKey = intent.getStringExtra("SENSOR_KEY");
                displayReceivedData(data, sensorKey);
            }
        }
    };
    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(MyBleManager.ACTION_DATA_AVAILABLE);
        registerReceiver(dataReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(dataReceiver);
    }

    private int generateRandomColor() {

        return Color.rgb(255, 140, 0);
    }
}