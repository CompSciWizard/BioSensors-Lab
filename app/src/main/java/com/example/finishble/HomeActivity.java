package com.example.finishble;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

// check the functionality features, UUID implementations and how they interfere with connection
//
public class HomeActivity extends AppCompatActivity {

    private static final long SCAN_PERIOD = 5000;

    private BluetoothAdapter bluetoothAdapter;
    private boolean scanning;
    private Handler handler;
    private LeDeviceListAdapter deviceListAdapter = new LeDeviceListAdapter();

    private Button scanButton;

    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final int REQUEST_BLUETOOTH_PERMISSION = 2;

    private String selectedDeviceAddress; // Store the address of the selected device

    @SuppressLint("SetTextI18n")
    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        setContentView(R.layout.activity_home);
        scanButton = findViewById(R.id.scan_button);
        ListView deviceListView = findViewById(R.id.device_list);

        handler = new Handler();

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {

                requestEnableBluetooth();
            } else {
                checkAndRequestLocationPermission();
            }
        }

        deviceListAdapter = new LeDeviceListAdapter();
        deviceListView.setAdapter(deviceListAdapter);

        deviceListView.setOnItemClickListener((parent, view, position, id) -> {

            BluetoothDevice device = (BluetoothDevice) deviceListAdapter.getItem(position);
            selectedDeviceAddress = device.getAddress();

            // Create an intent to start the MainActivity
            Intent intent = new Intent(HomeActivity.this, MainActivity.class);
            intent.putExtra("deviceAddress", selectedDeviceAddress); // Pass the selected device address as an extra

            startActivity(intent);

        });

        scanButton.setOnClickListener(v -> {
            if (!scanning) {
                startScan();
                scanButton.setText("Stop Scan");
            } else {
                stopScan();
                scanButton.setText("Start Scan");
            }
        });

    }

    @SuppressLint({"MissingPermission", "SetTextI18n"})
    private void startScan() {

        if (deviceListAdapter != null) {
            deviceListAdapter.clear();
        }
        assert deviceListAdapter != null;
        deviceListAdapter.notifyDataSetChanged();

        if (bluetoothAdapter == null) {
            return;
        }

        scanning = true;
        scanButton.setText("Stop Scan");

        handler.postDelayed(this::stopScan, SCAN_PERIOD);

        bluetoothAdapter.getBluetoothLeScanner().startScan(scanCallback);
    }

    @SuppressLint({"MissingPermission", "SetTextI18n"})
    private void stopScan() {
        scanning = false;
        scanButton.setText("Start Scan");

        if (bluetoothAdapter == null) {
            return;
        }

        bluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
    }


    private final android.bluetooth.le.ScanCallback scanCallback = new android.bluetooth.le.ScanCallback() {
        @Override
        public void onScanResult(int callbackType, android.bluetooth.le.ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            deviceListAdapter.addDevice(device);
            deviceListAdapter.notifyDataSetChanged();
        }

    };

    @Override
    protected void onResume() {
        super.onResume();

        if (scanning) {
            startScan();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopScan();
        deviceListAdapter.clear();
        deviceListAdapter.notifyDataSetChanged();
    }

    private class LeDeviceListAdapter extends BaseAdapter {

        private final List<BluetoothDevice> deviceList;

        public LeDeviceListAdapter() {
            deviceList = new ArrayList<>();
        }

        @SuppressLint("MissingPermission")
        public void addDevice(BluetoothDevice device) {
            if (device.getName() != null && !deviceList.contains(device)) {
                deviceList.add(device);
            }
        }

        public void clear() {
            deviceList.clear();
        }

        @Override
        public int getCount() {
            return deviceList.size();
        }

        @Override
        public Object getItem(int position) {
            return deviceList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @SuppressLint("MissingPermission")
        @RequiresApi(api = Build.VERSION_CODES.S)
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView textView;
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, parent, false);
                textView = convertView.findViewById(android.R.id.text1);
                convertView.setTag(textView);
            } else {
                textView = (TextView) convertView.getTag();
            }

            BluetoothDevice device = deviceList.get(position);

            textView.setText(device.getName() != null ? device.getName() : "Unknown Device");


            return convertView;
        }
    }
    private void requestEnableBluetooth() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        ActivityResultLauncher<Intent> enableBluetoothLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // Bluetooth was enabled, proceed with other operations
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            checkAndRequestLocationPermission();
                        }
                    } else {
                        Toast.makeText(this, "Bluetooth is required to continue.", Toast.LENGTH_SHORT).show();
                    }
                });

        enableBluetoothLauncher.launch(enableBtIntent);
    }


    @RequiresApi(api = Build.VERSION_CODES.S)
    private void checkAndRequestLocationPermission() {
        List<String> permissionsToRequest = new ArrayList<>();

        // Check and add location permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        // Check and add Bluetooth scanning permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN);
        }

        // Check and add Bluetooth connecting permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT);
        }

        // Check and add nearby devices permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), REQUEST_LOCATION_PERMISSION);
        }
    }




    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

               // requestEnableBluetooth();

            } else {
                Toast.makeText(this, "Location permission is required to scan for devices.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestEnableBluetooth();
            } else {
                Toast.makeText(this, "Bluetooth permission is required to enable Bluetooth.", Toast.LENGTH_SHORT).show();
            }
        }
    }


}