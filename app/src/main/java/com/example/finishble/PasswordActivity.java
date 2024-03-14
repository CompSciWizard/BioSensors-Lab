package com.example.finishble;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
public class PasswordActivity extends AppCompatActivity {

    private EditText passwordEditText;
    private TextView errorTextView;

    private final String correctPassword = "BLEUNF";

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password);

        passwordEditText = findViewById(R.id.passwordEditText);
        Button loginButton = findViewById(R.id.loginButton);
        errorTextView = findViewById(R.id.errorTextView);

        loginButton.setOnClickListener(v -> {
            String enteredPassword = passwordEditText.getText().toString();

            if (enteredPassword.equals(correctPassword)) {
                // Password is correct, navigate to the home activity
                Intent homeIntent = new Intent(PasswordActivity.this, HomeActivity.class);
                startActivity(homeIntent);

                // Start the HealthInfoActivity immediately after HomeActivity
                Intent healthInfoIntent = new Intent(PasswordActivity.this, HealthInfoActivity.class);
                startActivity(healthInfoIntent);

                finish(); // Finish PasswordActivity so that the user can't go back to it
            } else {
                // Password is incorrect, show an error message
                errorTextView.setText("Incorrect password");
            }
        });
    }
}
