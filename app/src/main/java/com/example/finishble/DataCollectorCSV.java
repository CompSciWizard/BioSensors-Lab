package com.example.finishble;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import androidx.annotation.RequiresApi;

import com.polar.sdk.api.model.PolarEcgData;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class DataCollectorCSV {

    // Single instance
    private static DataCollectorCSV instance;
    private static boolean isCollectingData;
    private static long startTime;

    private static String currentMusicGenre = "None"; // Default to "None"

    private static Map<String, List<DataPoint>> dataPointsMap;


    // Private constructor to prevent instantiation
    private DataCollectorCSV() {
        dataPointsMap = new HashMap<>();
        isCollectingData = false;
        currentMusicGenre = "None"; // Default genre
    }

    // Public method to get the instance
    public static synchronized DataCollectorCSV getInstance() {
        if (instance == null) {
            instance = new DataCollectorCSV();
        }
        return instance;
    }

    public static void setCurrentMusicGenre(String musicGenre) {
        currentMusicGenre = musicGenre;
    }

    public static void startDataCollection() {
        dataPointsMap = new HashMap<>();
        isCollectingData = true;
        startTime = System.currentTimeMillis() / 1000;
    }

    public static void stopDataCollection(ContentResolver contentResolver, List<String> sensorKeys) {
        isCollectingData = false;
        long stopTime = System.currentTimeMillis() / 1000;
        long elapsedTime = stopTime - startTime;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveDataToCSV(contentResolver, elapsedTime, sensorKeys);
            instance.saveEcgDataToCSV(contentResolver, elapsedTime); // Save ECG data separately

        }
    }

    // In DataCollectorCSV, the addDataPoint method for non-ECG sensors
    // For adding general (non-ECG) sensor data
    public static void addDataPoint(String sensorKey, float value) {
        if (isCollectingData) {
            dataPointsMap.computeIfAbsent(sensorKey, k -> new ArrayList<>())
                    .add(new DataPoint(value, currentMusicGenre));
        }
    }

    // Specifically for adding ECG data
// Method to add multiple ECG data points at once
    public void addEcgDataPoints(List<PolarEcgData.PolarEcgDataSample> samples) {
        if (!isCollectingData) return;

        String ecgKey = "ECG";
        List<DataPoint> ecgDataList = dataPointsMap.computeIfAbsent(ecgKey, k -> new ArrayList<>());
        for (PolarEcgData.PolarEcgDataSample sample : samples) {
            ecgDataList.add(new DataPoint(sample.getVoltage(), currentMusicGenre, true, sample.getTimeStamp()));
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void saveEcgDataToCSV(ContentResolver contentResolver, long elapsedTime) {
        List<DataPoint> ecgDataPoints = dataPointsMap.getOrDefault("ECG", new ArrayList<>());
        if (ecgDataPoints.isEmpty()) {
            System.out.println("No ECG data points to save.");
            return;
        }
        String fileName = HealthInfoActivity.getFileName();
        String ecgFileName = "ECG_Data_From_" + fileName + generateFileName() + ".csv";

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, ecgFileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
        Uri externalUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL);
        Uri itemUri = contentResolver.insert(externalUri, contentValues);

        try (OutputStream outputStream = contentResolver.openOutputStream(Objects.requireNonNull(itemUri))) {
            StringBuilder csvData = new StringBuilder("TimeStamp,ECG Voltage (μV),Music Genre\n");

            for (DataPoint dp : ecgDataPoints) {
                String timestamp = String.valueOf(dp.getTimeStamp());
                csvData.append(timestamp).append(",").append(dp.getEcgValue()).append(",").append(dp.getMusicGenre()).append("\n");
            }
            outputStream.write(csvData.toString().getBytes());
            System.out.println("ECG Data saved to CSV file: " + ecgFileName);
        } catch (IOException e) {
            System.err.println("Error writing ECG data to CSV file: " + e.getMessage());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static void saveDataToCSV(ContentResolver contentResolver, long elapsedTime, List<String> sensorKeys) {
        if (dataPointsMap.isEmpty()) {
            System.out.println("No data points to save.");
            return;
        }

        String fileName = HealthInfoActivity.getFileName();

        String displayName = "Data_From_" + fileName + ".csv";

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");

        Uri externalUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL);
        Uri itemUri = contentResolver.insert(externalUri, contentValues);

        try {
            assert itemUri != null;
            try (OutputStream outputStream = contentResolver.openOutputStream(itemUri)) {
                if (outputStream != null) {
                    StringBuilder csvData = new StringBuilder();
                    csvData.append("Time (s),Data,Music Genre\n");


                    DecimalFormat decimalFormat = new DecimalFormat("#.00");

                    float timeIncrement = elapsedTime / (float) Objects.requireNonNull(dataPointsMap.get(sensorKeys.get(0))).size();

                    float currentTime = 0.0f;
                    int dataSize = Objects.requireNonNull(dataPointsMap.get(sensorKeys.get(0))).size(); // Use the size of any sensor's data
                    // Inside saveDataToCSV, when iterating over dataPoints to write to CSV
                    for (int i = 0; i < dataSize; i++) {
                        csvData.append(decimalFormat.format(currentTime)).append("s,");
                        for (String sensorKey : sensorKeys) {
                            DataPoint dataPoint = dataPointsMap.get(sensorKey).get(i);
                            // Decide whether to use value or ecgValue based on the context
                            float sensorValue = sensorKey.equals("ECG") ? dataPoint.getEcgValue() : dataPoint.getValue();
                            String genre = dataPoint.getMusicGenre();
                            csvData.append(decimalFormat.format(sensorValue)).append(sensorKey.equals("ECG") ? "μV," : "V,");
                            csvData.append(genre).append(",");
                        }
                        csvData.append("\n");
                        currentTime += timeIncrement;
                    }

                    outputStream.write(csvData.toString().getBytes());
                    outputStream.flush();
                    System.out.println("Data saved to CSV file: " + displayName);
                }
            }
        } catch (IOException e) {
            System.err.println("Error writing data to CSV file: " + e.getMessage());
        }
    }

    private static String generateFileName() {
        SimpleDateFormat dateFormat = (SimpleDateFormat) SimpleDateFormat.getInstance();
        String timestamp = dateFormat.format(new Date());
        return "data_" + timestamp;
    }

    public static class DataPoint {
        private Float value; // Use Float for optional non-ECG data
        private Float ecgValue; // Use Float for ECG data
        private String musicGenre;
        private long timeStamp; // New field to store timestamp

        // Constructor for non-ECG data points
        public DataPoint(Float value, String musicGenre) {
            this.value = value;
            this.ecgValue = null; // Indicates this is not ECG data
            this.musicGenre = musicGenre;
            this.timeStamp = System.currentTimeMillis(); // Set timestamp
        }


        // New constructor for ECG data points
        public DataPoint(float ecgValue, String musicGenre, boolean isEcgData, long timeStamp) {
            // Assuming isEcgData is always true when this constructor is used, we might not need it as a parameter.
            this.ecgValue = ecgValue;
            this.musicGenre = musicGenre;
            this.timeStamp = timeStamp;
            // Since this constructor is specifically for ECG data, we can ignore the 'value' field or set it to null/0
        }

        // Getters (and potentially setters if needed)
        public Float getValue() { return value; }
        public Float getEcgValue() { return ecgValue; }
        public String getMusicGenre() { return musicGenre; }
        public long getTimeStamp() { return timeStamp; }
    }

    public static String getCurrentMusicGenre() {
        return currentMusicGenre;
    }


    public boolean isCollectingData() {
        return isCollectingData;
    }

}

