package com.example.finishble;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyBleManager extends Service {

    //  private PolarBleApi polarApi;
// In MyBleManager.java or a similar place
    public static final String ACTION_ECG_READY = "com.example.finishble.ACTION_ECG_READY";
    private static final int SENSOR_PROCESSING_DELAY_MS =  1;
    // Define a HashMap to store the thread pool executors for each characteristic
    private final HashMap<String, ExecutorService> characteristicExecutors = new HashMap<>();

    private ExecutorService notificationExecutor = Executors.newFixedThreadPool(3); // Adjust pool size as needed

    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private final IBinder mBinder = new LocalBinder();
    private final static String TAG = MyBleManager.class.getSimpleName();
    private static BluetoothManager mBleManager;
    private static BluetoothAdapter mBleAdapter;


    // Define a data structure to store the characteristics for each sensor
    private final HashMap<String, List<BluetoothGattCharacteristic>> sensorCharacteristics = new HashMap<>();

    public final static UUID UUID_HEART_RATE_MEASUREMENT =
            UUID.fromString(Attributes.HEART_RATE_MEASUREMENT);


    // Replace with your ESP32 characteristic UUID for data
    private String mBleDeviceAddress;
    private static BluetoothGatt mBleGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA";

    private final Attributes attributes = new Attributes();  // collect the uuid characteristic

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            String intentAction;

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                if (ActivityCompat.checkSelfPermission(MyBleManager.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // Handle the case where the permission is not granted
                    // (e.g., show an error message or disable functionality)
                    return;
                }
                Log.i(TAG, "Attempting to start service discovery: " + mBleGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
                close();

            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            // Clear desired characteristics before each connection attempt
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);

                List<BluetoothGattService> services = gatt.getServices();

                // Iterate through the discovered services
                for (BluetoothGattService service : services) {
                    UUID serviceUUID = service.getUuid();
                    Log.d(TAG, "Service UUID: " + serviceUUID.toString());

                    List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();

                    // Iterate through the characteristics of the service
                    for (BluetoothGattCharacteristic characteristic : characteristics) {
                        UUID charUUID = characteristic.getUuid();
                        Log.d(TAG, "Characteristic UUID: " + charUUID.toString());

                        // Match the desired characteristics based on their properties, permissions, or other criteria
                        if (attributes.isDesiredCharacteristic(characteristic)) {
                            // Get the sensor key based on the UUID of the characteristic
                            String sensorKey = characteristic.getUuid().toString();

                            // Add the characteristic to the sensorCharacteristics HashMap using the sensor key
                            if (!sensorCharacteristics.containsKey(sensorKey)) {
                                sensorCharacteristics.put(sensorKey, new ArrayList<>());
                            }
                            Objects.requireNonNull(sensorCharacteristics.get(sensorKey)).add(characteristic);
                            // Add a log statement to confirm the UUIDs added to the HashMap
                            Log.d(TAG, "Added characteristic with UUID " + charUUID + " to sensor key: " + sensorKey);
                        }
                    }
                }


                // After processing all characteristics, enable notifications and write descriptors
                for (String sensorKey : sensorCharacteristics.keySet()) {
                    List<BluetoothGattCharacteristic> sensorCharacteristicsList = sensorCharacteristics.get(sensorKey);

                    Log.d(TAG, "Sensor Key: " + sensorKey);
                    assert sensorCharacteristicsList != null;
                    for (BluetoothGattCharacteristic characteristic : sensorCharacteristicsList) {
                        // Enable notifications for each characteristic in a separate thread
                        enableNotificationsAsync(gatt, characteristic);
                    }
                }
                // After confirming ECG service is available
                Intent intent = new Intent(ACTION_ECG_READY);
                intent.putExtra("deviceAddress", gatt.getDevice().getAddress());
                sendBroadcast(intent);
            }
        }

        // Method to enable notifications for each characteristic in a separate thread
        private void enableNotificationsAsync(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            Runnable enableNotificationsTask = () -> enableNotifications(gatt, characteristic);

            // Use the same executor for all characteristics since notifications can be processed concurrently
            ExecutorService executor = characteristicExecutors.get("notifications");
            if (executor == null) {
                // If the executor does not exist for notifications, create a new cached thread pool
                executor = Executors.newCachedThreadPool();
                characteristicExecutors.put("notifications", executor);
            }

            executor.execute(enableNotificationsTask);
        }

        // Method to enable notifications for a characteristic
        @SuppressLint("MissingPermission")
        private void enableNotifications(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (gatt == null || characteristic == null) {
                Log.w(TAG, "BluetoothGatt or Characteristic is null");
                return;
            }

            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
            if (descriptor == null) {
                Log.w(TAG, "Descriptor not found for characteristic: " + characteristic.getUuid().toString());
                return;
            }

            gatt.setCharacteristicNotification(characteristic, true);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

            boolean status = gatt.writeDescriptor(descriptor);
            if (!status) {
                Log.w(TAG, "Failed to write descriptor");
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            // Log when the method is triggered
            Log.d(TAG, "Characteristic changed: " + characteristic.getUuid().toString());

            broadcastUpdate(characteristic);
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    // The modified broadcastUpdate method
    private void broadcastUpdate(final BluetoothGattCharacteristic characteristic) {
        for (String sensorKey : sensorCharacteristics.keySet()) {
            List<BluetoothGattCharacteristic> sensorCharacteristicsList = sensorCharacteristics.get(sensorKey);
            if (sensorCharacteristicsList != null && sensorCharacteristicsList.contains(characteristic)) {
                // If the characteristic is present in the sensorCharacteristicsList,
                // process it in a separate thread
                processDataForCharacteristicAsync(characteristic, sensorKey);

                try {
                    // Add a delay to allow switching to the next sensor quickly
                    Thread.sleep(SENSOR_PROCESSING_DELAY_MS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }





    private void processDataForCharacteristicAsync(final BluetoothGattCharacteristic characteristic, final String sensorKey) {
        Runnable processCharacteristicTask = () -> processDataForCharacteristic(characteristic, sensorKey);

        // Get or create the appropriate thread pool executor for the characteristic's UUID
        ExecutorService executor = characteristicExecutors.get(sensorKey);
        if (executor == null) {
            // If the executor does not exist for this characteristic, create a new cached thread pool
            executor = Executors.newCachedThreadPool();
            characteristicExecutors.put(sensorKey, executor);
        }

        executor.execute(processCharacteristicTask);
    }



    // The modified processDataForCharacteristic method
// Process the data for all relevant characteristics of a sensor
    private void processDataForCharacteristic(final BluetoothGattCharacteristic characteristic, String sensorKey) {
        final Intent intent = new Intent(MyBleManager.ACTION_DATA_AVAILABLE);
        final byte[] data = characteristic.getValue();

        Log.d(TAG, "Processing characteristic with UUID: " +
                characteristic.getUuid().toString() + " for sensor key: " + sensorKey);

        List<BluetoothGattCharacteristic> sensorCharacteristicsList = sensorCharacteristics.get(sensorKey);

        // Check if the list is not null and not empty before processing
        float floatValue = 0;
        if (sensorCharacteristicsList != null && !sensorCharacteristicsList.isEmpty()) {
            for (BluetoothGattCharacteristic sensorCharacteristic : sensorCharacteristicsList) {
                // Process each characteristic in the list separately
                if (UUID_HEART_RATE_MEASUREMENT.equals(sensorCharacteristic.getUuid())) {
                    // Process heart rate data
                    int flag = characteristic.getProperties();
                    int format;

                    if ((flag & 0x01) != 0) {
                        format = BluetoothGattCharacteristic.FORMAT_UINT16;
                        Log.d(TAG, "Heart rate format UINT16.");
                    } else {
                        format = BluetoothGattCharacteristic.FORMAT_UINT8;
                        Log.d(TAG, "Heart rate format UINT8.");
                    }
                    final int heartRate = characteristic.getIntValue(format, 1);
                    Log.d(TAG, String.format("Received heart rate: %d", heartRate));

                    GraphsUtil.addDataPoint(sensorKey, heartRate);
                    DataCollectorCSV.addDataPoint(sensorKey, heartRate);

                    intent.putExtra(EXTRA_DATA, heartRate + " bpm");
                } else {
                    // Process other characteristics as floating-point data
                    if (data != null && data.length > 0) {
                        final StringBuilder stringBuilder = new StringBuilder(data.length);
                        for (int i = 0; i < data.length; i += 4) {
                            // Extract the bytes representing the floating-point value
                            byte[] bytes = new byte[4];
                            System.arraycopy(data, i, bytes, 0, 4);

                            // Convert the bytes to a float value
                            floatValue = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getFloat();

                            GraphsUtil.addDataPoint(sensorKey, floatValue);
                            DataCollectorCSV.addDataPoint(sensorKey, floatValue);

                            DecimalFormat decimalFormat = new DecimalFormat("0.00");
                            String formattedValue = decimalFormat.format(floatValue);

                            stringBuilder.append(formattedValue).append(" V");
                        }

                        String formattedData = stringBuilder.toString().trim();
                        intent.putExtra(EXTRA_DATA, formattedData);
                        Log.d(TAG, "Processed floating-point data: " + formattedData);
                    }
                }
            }
        }
        // Send the broadcast with the sensor key as an extra to identify the sensor
        intent.putExtra("SENSOR_KEY", sensorKey);
        sendBroadcast(intent);

        // Send the sensor data to StatsActivity
        Intent sensorDataIntent = new Intent("SENSOR_DATA_RECEIVED");
        sensorDataIntent.putExtra("SENSOR_DATA", floatValue); // Replace with your sensor data
        sendBroadcast(sensorDataIntent);

        // Send the sensor data to GraphActivity
        Intent GraphDataIntent = new Intent("SENSOR_DATA_RECEIVED");
        GraphDataIntent.putExtra("SENSOR_DATA", floatValue); // Replace with your sensor data
        sendBroadcast(GraphDataIntent);

    }

    public HashMap<String, List<BluetoothGattCharacteristic>> getSensorCharacteristics() {
        return sensorCharacteristics;
    }


    public class LocalBinder extends Binder {
        MyBleManager getService() {
            return MyBleManager.this;
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        if (mHandlerThread == null) {
            mHandlerThread = new HandlerThread("BleThread");
            mHandlerThread.start();
            mHandler = new Handler(mHandlerThread.getLooper());
        }
        return mBinder;
    }

    public boolean onUnbind(Intent intent) {
        close();

        return super.onUnbind(intent);
    }

    public boolean initialize() {

        HandlerThread mHandlerThread = new HandlerThread("BleThread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        if (mBleManager == null) {
            mBleManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

            if (mBleManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager");
                return false;
            }
        }
        mBleAdapter = mBleManager.getAdapter();
        if (mBleAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter");
            return false;
        }
        return true;
    }

    @SuppressLint("MissingPermission")
    public void connect(final String address) {
        mHandler.post(() -> {
            if (mBleAdapter == null || address == null) {
                Log.w(TAG, "Bluetooth not initialized or unspecified address.");
                return;
            }

            if (address.equals(mBleDeviceAddress) && mBleGatt != null) {
                Log.d(TAG, "Trying to use an existing mBleGatt for connection.");

                // Check the current connection state
                if (mConnectionState == BluetoothGatt.STATE_CONNECTED) {
                    Log.d(TAG, "Already connected to the device.");
                    return;
                } else if (mConnectionState == BluetoothGatt.STATE_DISCONNECTED) {
                    // Connect to the device

                        mBleGatt.connect();
                        mConnectionState = STATE_CONNECTING;
                    return;
                } else {
                    // Other connection states, handle accordingly
                    return;
                }
            }

            final BluetoothDevice device = mBleAdapter.getRemoteDevice(address);
            if (device == null) {
                Log.d(TAG, "Device not found. Unable to connect.");
                return;
            }

            mBleGatt = device.connectGatt(MyBleManager.this, false, mGattCallback);
            Log.d(TAG, "Trying to create new connection");
            mBleDeviceAddress = address;
            mConnectionState = STATE_CONNECTING;
        });
    }

    @SuppressLint("MissingPermission")
    public void disconnect() {
        if (mBleAdapter == null || mBleGatt == null) {
            Log.w(TAG, "Bluetooth not initialized");
            return;
        }

        mBleGatt.disconnect();
        mConnectionState = STATE_DISCONNECTED;
    }

    @SuppressLint("MissingPermission")
    public void close() {
        if (mBleGatt == null) {
            return;
        }
        mBleGatt.close();
        mBleGatt = null;
    }


    //To modify UUID specifics

   // public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
       // if (mBleAdapter == null || mBleGatt == null) {
           // Log.w(TAG, "BluetoothAdapter not initialized");
         //   return;
     //   }
      //  if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
      //      return;
      //  }
      //  mBleGatt.readCharacteristic(characteristic);
  //  }


    //public static void getSupportedGattServices() {
        //if (mBleGatt == null) {
       //     return;
      //  }
      //  mBleGatt.getServices();
  //  }

}