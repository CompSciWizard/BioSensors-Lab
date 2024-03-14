package com.example.finishble;

import android.bluetooth.BluetoothGattCharacteristic;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Attributes {

    private final Set<UUID> desiredCharacteristics;

    public final static UUID UUID_HEART_RATE_SERVICE = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
    public final static UUID UUID_HEART_RATE_MEASUREMENT = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");

    public static String HEART_RATE_MEASUREMENT = "00002A37-0000-1000-8000-00805F9B34FB";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    public Attributes() {

        desiredCharacteristics = new HashSet<>();

      //  desiredCharacteristics.add(UUID.fromString("74cb87b3-798b-463c-a59c-c9e3127f90bb")); // custom for esp32unf dual
       // desiredCharacteristics.add(UUID.fromString("6ff0d9fd-2172-4c19-87ad-0ea25f2a5a12")); //#2

       desiredCharacteristics.add(UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")); // custom for esp32unf yel
        desiredCharacteristics.add(UUID.fromString("0c323606-6f62-4f25-bbd9-c41960a460c6")); // custom for esp32unf RED
        desiredCharacteristics.add(UUID.fromString(HEART_RATE_MEASUREMENT)); // Heart Rate Measurement - Used for heart rate monitoring
       // desiredCharacteristics.add(UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb")); // Manufacturer Name String - Provides the name of the device manufacturer
      //  desiredCharacteristics.add(UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb")); // Model Number String - Provides the model number of the device
       // desiredCharacteristics.add(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")); // Current Time - Indicates the current time
       // desiredCharacteristics.add(UUID.fromString("00002a35-0000-1000-8000-00805f9b34fb")); // Blood Pressure Measurement - Used for blood pressure monitoring
      //  desiredCharacteristics.add(UUID.fromString("00002a53-0000-1000-8000-00805f9b34fb")); // RSC Measurement - Used for running speed and cadence monitoring
      //  desiredCharacteristics.add(UUID.fromString("00002a5b-0000-1000-8000-00805f9b34fb")); // CSC Measurement - Used for cycling speed and cadence monitoring
        //desiredCharacteristics.add(UUID.fromString("00002a66-0000-1000-8000-00805f9b34fb")); // CGM Measurement - Used for continuous glucose monitoring
        //desiredCharacteristics.add(UUID.fromString("00002a68-0000-1000-8000-00805f9b34fb")); // CGM Session Run Time - Provides the run time of a continuous glucose monitoring session
        //desiredCharacteristics.add(UUID.fromString("00002a6b-0000-1000-8000-00805f9b34fb")); // Indoor Bike Data - Used for indoor cycling data monitoring
        //desiredCharacteristics.add(UUID.fromString("00002a6e-0000-1000-8000-00805f9b34fb")); // Supported Heart Rate Range - Indicates the supported heart rate range
        //desiredCharacteristics.add(UUID.fromString("00002a98-0000-1000-8000-00805f9b34fb")); // FTP Server - Provides information about an FTP server

        // Add more UUIDs as needed
    }

    public boolean isDesiredCharacteristic(BluetoothGattCharacteristic characteristic) {
        UUID characteristicUuid = characteristic.getUuid();
        return desiredCharacteristics.contains(characteristicUuid);
    }
    public void clearDesiredCharacteristics() {
        desiredCharacteristics.clear();
    }
    // Other methods related to managing desired characteristics
}
