package com.example.finishble;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class HealthInfoActivity extends AppCompatActivity {


    private EditText fullNameInput;
    private EditText musicGenreInput;
    private EditText ageInput;

    static String fileName;
    private RadioGroup genderRadioGroup;
    private EditText conditionsInput;
    private EditText historyInput;
    private EditText perscriptionInput;
    private EditText allergiesInput;
    private EditText drugInput;
    private EditText armyInput;
    private Button submitButton;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_health_info);

        // Initialize UI elements
        ageInput = findViewById(R.id.age_input);
        genderRadioGroup = findViewById(R.id.gender_radio_group);
        conditionsInput = findViewById(R.id.conditions_input);
        historyInput = findViewById(R.id.history_input);
        perscriptionInput = findViewById(R.id.prescription_input);
        allergiesInput = findViewById(R.id.allergies_input);
        drugInput = findViewById(R.id.drug_input);
        armyInput = findViewById(R.id.army_input);
        submitButton = findViewById(R.id.submit_button);
        fullNameInput = findViewById(R.id.full_name_input); // replace with actual ID from your layout
        musicGenreInput = findViewById(R.id.music_genre_input); // replace with actual ID from your layout

        // Set a click listener for the submit button
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Retrieve user inputs
                String age = ageInput.getText().toString();
                int selectedGenderId = genderRadioGroup.getCheckedRadioButtonId();
                RadioButton selectedGender = findViewById(selectedGenderId);
                String gender = selectedGender.getText().toString().trim();
                String conditions = conditionsInput.getText().toString().trim();
                String history = historyInput.getText().toString().trim();
                String prescriptions = perscriptionInput.getText().toString().trim();
                String allergies = allergiesInput.getText().toString().trim();
                String drug_status = drugInput.getText().toString().trim();
                String army_services = armyInput.getText().toString().trim();
                String full_name = fullNameInput.getText().toString().trim();
                fileName = fullNameInput.getText().toString().trim();
                String music_genre = musicGenreInput.getText().toString().trim();


                // Insert data in background thread
               // insertDataIntoDatabase(age, gender, conditions, history, prescriptions, allergies, drug_status, army_services, full_name, music_genre);
                // Perform validation and processing here

                // For now, display the collected data
                displayData(age, gender, conditions, history, prescriptions, allergies, drug_status, army_services, full_name, music_genre);


                // Call the method to write to CSV
                onSubmitButtonClick(getContentResolver(), age, gender, conditions, history, prescriptions, allergies, drug_status, army_services, full_name, music_genre);

                // Navigate to the home activity

                // After processing, navigate to the home activity
                Intent intent = new Intent(HealthInfoActivity.this, HomeActivity.class);
                startActivity(intent);
                finish(); // Finish the HealthInfoActivity to prevent going back to it with the back button
            }
            // Function to handle the button click and write to CSV

        });

    }

    public static String getFileName() {

        String displayName = fileName;

        return displayName;
    }

    public void onSubmitButtonClick(ContentResolver contentResolver, String age, String gender, String conditions, String history, String prescriptions, String allergies, String drug_status, String army_services, String full_name, String music_genre) {
        // Format as CSV
        String csvLine = age + "," + gender + "," + conditions + "," + history + "," + prescriptions + "," +
                allergies + "," + drug_status + "," + army_services + "," + full_name + "," + music_genre + "\n";

        // Write to CSV file
        String displayName = "Screening_From_" + full_name + ".csv"; // Ensure fileName is initialized properly
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");

        Uri externalUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL);
        Uri itemUri = contentResolver.insert(externalUri, contentValues);

        try {
            assert itemUri != null;
            try (OutputStream outputStream = contentResolver.openOutputStream(itemUri, "wa")) { // "wa" for append mode
                if (outputStream != null) {
                    outputStream.write(csvLine.getBytes());
                    Toast.makeText(this, "Data saved to CSV", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving data", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayData(String age, String gender, String conditions, String history, String perscriptions, String allergies, String drugStatus, String armyService, String fullName, String musicGenre) {
        // Display collected data (you can replace this with your logic)
        String message = "Age: " + age + "\nGender: " + gender + "\nConditions: " + conditions + "\nHistory: " + history
                + "\nPerscriptions: " + perscriptions + "\nAllergies: " + allergies + "\nDrug Status: " + drugStatus
                + "\nArmy Service: " + armyService;
        // Add the new data to the message
        message += "\nFull Name: " + fullName + "\nMusic Genre: " + musicGenre;

        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
/*
    //  root@localhost:3306
    // jdbc:mysql://localhost:3306/?user=root
    private void insertDataIntoDatabase(final String age, final String gender, final String conditions, final String history, final String prescriptions, final String allergies, final String drug_status, final String army_services, final String full_name, final String music_genre) {
        // URL of your PHP script on the server
        String url = "http://192.168.1.77/conn.php";

        // Create a new request queue
        RequestQueue queue = Volley.newRequestQueue(this);

        // Create a StringRequest
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            // Convert the response into a JSONObject for parsing
                            JSONObject jsonResponse = new JSONObject(response);
                            boolean error = jsonResponse.getBoolean("error");
                            String message = jsonResponse.getString("message");
                            if (!error) {
                                // Data inserted successfully
                                Toast.makeText(HealthInfoActivity.this, message, Toast.LENGTH_SHORT).show();
                            } else {
                                // Server returned failure
                                Toast.makeText(HealthInfoActivity.this, message, Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(HealthInfoActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Handle error
                        Toast.makeText(HealthInfoActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("age", age);
                params.put("gender", gender);
                params.put("conditions", conditions);
                params.put("history", history);
                params.put("prescriptions", prescriptions);
                params.put("allergies", allergies);
                params.put("drug_status", drug_status); // Changed to match the case in the PHP script
                params.put("army_services", army_services); // Changed to match the case in the PHP script
                params.put("full_name", full_name); // Make sure the key matches the one expected by your PHP script
                params.put("music_genre", music_genre); // Make sure the key matches the one expected by your PHP script
                return params;
            }


        };

        // Add the request to the request queue
        queue.add(stringRequest);
    }
}

*/
/*
    private void insertDataIntoDatabase(final String age, final String gender, final String conditions, final String history, final String prescriptions, final String allergies, final String drug_status, final String army_service, final String full_name, final String music_genre) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                Connection conn = null;
                PreparedStatement preparedStatement = null;
                try {
                    // Step 1: Load the JDBC driver
                    Class.forName("com.mysql.jdbc.Driver");

                    // Step 2: Define connection URL
                    String url = "jdbc:mysql://localhost:3306/healthinfo"; // replace with your database URL
                    String user = "root"; // replace with your database username
                    String password = "admin"; // replace with your database password

                    // Step 3: Establish the connection
                    conn = DriverManager.getConnection(url, user, password);

                    // Step 4: Create SQL statement
                    String sql = "INSERT INTO healthscreeninginfo (age, gender, conditions, history, prescriptions, allergies, drug_status, army_service, full_name, music_genre) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"; // replace with your table name and columns

                    // Step 5: Prepare the statement
                    preparedStatement = conn.prepareStatement(sql);
                    preparedStatement.setString(1, age);
                    preparedStatement.setString(2, gender);
                    preparedStatement.setString(3, conditions);
                    preparedStatement.setString(4, history);
                    preparedStatement.setString(5, prescriptions);
                    preparedStatement.setString(6, allergies);
                    preparedStatement.setString(7, drug_status);
                    preparedStatement.setString(8, army_service);
                    preparedStatement.setString(9, full_name);
                    preparedStatement.setString(10, music_genre);

                    // Step 6: Execute the statement
                    int affectedRows = preparedStatement.executeUpdate();
                    if (affectedRows > 0) {
                        // Data inserted successfully
                        // Use runOnUiThread to perform UI operations
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(HealthInfoActivity.this, "Data inserted successfully", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(HealthInfoActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } finally {
                    // Step 7: Close resources
                    try {
                        if (preparedStatement != null) preparedStatement.close();
                        if (conn != null) conn.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

}
*/