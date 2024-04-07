package com.example.finishble;

import static com.example.finishble.MyBleManager.ACTION_ECG_READY;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


// ENABLE NEARBY DEVICES PERMISSIONS CHECK
// CONNECTIVITY PROPERTY (look at youtube videos)

public class MainActivity extends AppCompatActivity {


    private LineChart lineChart;
    // Declare a global HashMap to store the most recent data for each sensor
    private final HashMap<String, String> sensorDataMap = new HashMap<>();
    private Map<String, LineDataSet> sensorGraphSeriesMap = new HashMap<>();

    private final HashMap<String, Integer> sensorCountMap = new HashMap<>();

    private MyBleManager mBleManager; // Reference to the MyBleManager instance

    private CountDownTimer connectionTimer;
    private TextView connectionTimerTextView;
    private long connectionStartTime;

    private final static String TAG = MainActivity.class.getSimpleName();

    private TextView mConnectionState;
    private boolean mConnected = false;

    private TextView mDataTextView;
    private Button mConnectButton;
    private Button toggleButton;

    private final Attributes attributes = new Attributes();

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            // Request location permission required for scanning on Android 6.0 and above


            mBleManager = ((MyBleManager.LocalBinder) service).getService();

            if (!mBleManager.initialize()){
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBleManager = null;

        }
    };

    private final BroadcastReceiver ecgReadyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_ECG_READY.equals(intent.getAction())) {
                String deviceAddress = intent.getStringExtra("deviceAddress");
                // Assuming you have ECGActivity correctly set up to start ECG streaming
                ECGActivity ecgActivity = new ECGActivity(getApplicationContext(), deviceAddress);
                ecgActivity.connectToDevice();
            }
        }
    };
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (MyBleManager.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                // Update UI when connected
                updateConnectionState(R.string.connection_status_connected);

            } else if (MyBleManager.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                mDataTextView.setText("\uD83D\uDE34"); // Clear the text instead of setting it to null
                // Update UI when disconnected
                updateConnectionState(R.string.connection_status_disconnected);

            } /*else if(MyBleManager.ACTION_GATT_SERVICES_DISCOVERED.equals(action)){
                mConnected = true;
                //displayGattServices Method
            }*/ else if (MyBleManager.ACTION_DATA_AVAILABLE.equals(action)) {
                // Update UI with received data
                String data = intent.getStringExtra(MyBleManager.EXTRA_DATA);
                String sensorKey = intent.getStringExtra("SENSOR_KEY");
                if (!sensorGraphSeriesMap.containsKey(sensorKey)) {
                    // This is either the first data point for this sensor, or the graph hasn't been initialized for it yet.
                    runOnUiThread(() -> {
                        if (lineChart.getData() == null) {
                            lineChart.setData(new LineData());
                        }
                        GraphsUtil.createGraphSeries(lineChart, sensorKey, Color.RED);
                        sensorGraphSeriesMap.put(sensorKey, null); // Just to mark that this sensor has been initialized
                    });
                }

                displayReceivedData(data, sensorKey);
                updateGraphWithData(sensorKey, data);
            }
        }
    };


    private void updateConnectionState(int resourceId) {

        runOnUiThread(
                () -> mConnectionState.setText(resourceId));
    }
    private void updateGraphWithData(String sensorKey, String dataString) {
        // Assuming the numeric extraction and parsing is correct...
        runOnUiThread(() -> {
            try {
                String numericPart = dataString.replaceAll("[^\\d.]+", "");
                float value = Float.parseFloat(numericPart);
                GraphsUtil.addDataPoint(sensorKey, value);
                GraphsUtil.plotDataPoints(lineChart, sensorKey);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Failed to parse float from received data", e);
            }
        });
    }

    private void displayReceivedData(String data, String sensorKey) {
        if (data != null && sensorKey != null) {
            if (!sensorDataMap.containsKey(sensorKey)) {
                // New sensor found, initialize the sensor count and add it to the map
                int nextCount = getNextSensorCount();
                //sensorDataMap.put(sensorKey, "Sensor " + nextCount + ": " + data);
                sensorDataMap.put(sensorKey, "❤️" + data);
                sensorCountMap.put(sensorKey, nextCount);
            } else {
                // Update the most recent data for the corresponding sensor
                int currentCount = sensorCountMap.get(sensorKey);
                //sensorDataMap.put(sensorKey, "Sensor " + currentCount + ": " + data);
                sensorDataMap.put(sensorKey, "❤️" + data);
            }

            // Generate a single string with data for all sensors
            StringBuilder displayDataBuilder = new StringBuilder();
            for (String key : sensorDataMap.keySet()) {
                displayDataBuilder.append(sensorDataMap.get(key)).append("\n");
            }

            // Update the UI TextView with the combined data for all sensors
            if (mDataTextView != null) {
                mDataTextView.setText(displayDataBuilder.toString());
            }

            } else {
                // Handle the case when the MyBleManager service is not available
                Toast.makeText(MainActivity.this, "Bluetooth service not available", Toast.LENGTH_SHORT).show();
            }
        }


    // Helper method to get the next sensor count
    private int getNextSensorCount() {
        int currentCount = sensorCountMap.size();
        return currentCount + 1;
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        setContentView(R.layout.activity_main);

        final Intent intent = getIntent();

        String sensorKey = intent.getStringExtra("SENSOR_KEY");
        lineChart = findViewById(R.id.line_chart);
        // Initialize with empty data to prepare the chart
        lineChart.setData(new LineData());
        // Example setup call; adapt as needed
        GraphsUtil.createGraphSeries(lineChart, sensorKey, Color.BLUE);
        connectionTimerTextView = findViewById(R.id.connection_timer);
        mDataTextView = findViewById(R.id.received_data_text_view);
        mConnectButton = findViewById(R.id.send_button);
        mConnectionState = findViewById(R.id.connection_status_text_view);
        // GraphView mGraphView = findViewById(R.id.graph_view);
        //  GraphView mGraphView2 = findViewById(R.id.graph_view2);

        // ... Add more GraphView instances

        //  graphViews.add(mGraphView);
        //  graphViews.add(mGraphView2);
        // ... Add more GraphView instances to the list

        // Add the "Show Graphs" button
        //  Button showGraphsButton = findViewById(R.id.show_graphs_button);
        // showGraphsButton.setOnClickListener(new View.OnClickListener() {
        // @Override
        // public void onClick(View v) {

        // if (mBleManager != null) {
        // Get the sensor characteristics from the MyBleManager service
        //  HashMap<String, List<BluetoothGattCharacteristic>> sensorCharacteristics = mBleManager.getSensorCharacteristics();

        // Collect the sensor keys
        //  List<String> sensorKeys = new ArrayList<>(sensorCharacteristics.keySet());

        // Start the GraphActivity with the sensor keys as intent extra
        //   Intent graphIntent = new Intent(MainActivity.this, GraphActivity.class);
        //  graphIntent.putStringArrayListExtra("SENSOR_KEYS", (ArrayList<String>) sensorKeys);
        //  startActivity(graphIntent);
        //  }// else {
        // Handle the case when the MyBleManager service is not available
        //  Toast.makeText(MainActivity.this, "Bluetooth service not available", Toast.LENGTH_SHORT).show();
        //  //   }
//        });


        // Add a single button for start and stop
        toggleButton = findViewById(R.id.toggle_button);
        toggleButton.setText("Start CSV");

        Intent gattServiceIntent = new Intent(this, MyBleManager.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        String deviceAddress = intent.getStringExtra("deviceAddress");

        mConnectButton.setOnClickListener(v -> {
            if (mConnected) {
                // Already connected, so disconnect first
                mBleManager.disconnect();


                if (connectionTimer != null) {
                    connectionTimer.cancel();
                    connectionTimer = null;
                }

                Intent Return = new Intent(MainActivity.this, HomeActivity.class);
                startActivity(Return);

            } else {
                if (deviceAddress != null) {

                    attributes.clearDesiredCharacteristics();

                    mBleManager.connect(deviceAddress);

                    ECGActivity ecgActivity = new ECGActivity(getApplicationContext(), deviceAddress);
                    ecgActivity.connectToDevice();


                    connectionStartTime = System.currentTimeMillis();
                    startConnectionTimer();

                    mConnectButton.setText("REFRESH");

                } else {
                    Toast.makeText(MainActivity.this, "No device selected", Toast.LENGTH_SHORT).show();
                }
            }
        });


        // Add a Button
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) Button statsButton = findViewById(R.id.stats_button);

        // Set a click listener for the button
// Set a click listener for the button
        statsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Launch the StatsActivity when the button is clicked
                Intent intent = new Intent(MainActivity.this, MusicActivity.class);
                startActivity(intent);
            }
        });


        // Set up the initial visibility of the graphs
        //  for (int i = 0; i < graphViews.size(); i++) {
        //  if (i == currentGraphIndex) {
        //      graphViews.get(i).setVisibility(View.VISIBLE);
        //    } else {
        //    graphViews.get(i).setVisibility(View.GONE);
        //   }
        //   }

        // Button toggleGraphButton = findViewById(R.id.toggleGraphButton);
        //toggleGraphButton.setOnClickListener(v -> toggleGraph());

        toggleButton.setOnClickListener(v -> {
            if (toggleButton.getText().equals("Start CSV")) {
                DataCollectorCSV.startDataCollection();
                toggleButton.setText("Save CSV");
                Toast.makeText(MainActivity.this, "Data collection started", Toast.LENGTH_SHORT).show();
            } else {

                List<String> sensorKeys = new ArrayList<>(mBleManager.getSensorCharacteristics().keySet());
                DataCollectorCSV.stopDataCollection(getContentResolver(), sensorKeys);

                toggleButton.setText("Start CSV");
                Toast.makeText(MainActivity.this, "Data collection stopped and saved to CSV", Toast.LENGTH_SHORT).show();
            }
        });

    }



    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onResume() {
        super.onResume();
        DataCollectorCSV collector = DataCollectorCSV.getInstance();
        if (collector.isCollectingData()) {
            toggleButton.setText("Save CSV");
        } else {
            toggleButton.setText("Start CSV");
        }
        // Register the ECG ready receiver
        IntentFilter ecgReadyFilter = new IntentFilter(ACTION_ECG_READY);
        registerReceiver(ecgReadyReceiver, ecgReadyFilter);

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

    }
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBleManager.close();
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MyBleManager.ACTION_GATT_CONNECTED);
        intentFilter.addAction(MyBleManager.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(MyBleManager.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }


    private void startConnectionTimer() {
        connectionTimer = new CountDownTimer(Long.MAX_VALUE, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long elapsedTime = System.currentTimeMillis() - connectionStartTime;
                updateConnectionTimerText(elapsedTime);
            }

            @Override
            public void onFinish() {
                // Timer finished
            }
        };
        connectionTimer.start();
    }

    private void updateConnectionTimerText(long elapsedTime) {
        long seconds = elapsedTime / 1000;
        long minutes = seconds / 60;
        seconds %= 60;

        String timerText = String.format(Locale.US, "%02d:%02d", minutes, seconds);

        connectionTimerTextView.setText(timerText);
    }
}
