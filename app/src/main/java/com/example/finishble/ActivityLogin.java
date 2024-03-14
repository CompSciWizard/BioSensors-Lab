package com.example.finishble;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class ActivityLogin extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        TextView username = findViewById(R.id.username);
        TextView password = findViewById(R.id.password);

        MaterialButton loginbtn = findViewById(R.id.loginbtn);

        ImageView googleIcon = findViewById(R.id.google_icon);
        ImageView fbIcon = findViewById(R.id.fb_icon);
        ImageView twitterIcon = findViewById(R.id.twitter_icon);

        // Find the "Forgot password?" TextView
        TextView forgotPasswordTextView = findViewById(R.id.forgotpass);

        // Set a click listener for the "Forgot password?" TextView
        forgotPasswordTextView.setOnClickListener(view -> {
            // Show the password reset dialog
            showPasswordResetDialog();
        });

        // Set a click listener for the Login button
        loginbtn.setOnClickListener(v -> {
            if (username.getText().toString().equals("ble") && password.getText().toString().equals("ble")) {
                // Correct login, navigate to the HomeActivity
                Intent intent = new Intent(ActivityLogin.this, HealthInfoActivity.class);
                startActivity(intent);
                finish(); // Finish the login activity
            } else {
                // Incorrect login, show an error message
                Toast.makeText(ActivityLogin.this, "LOGIN FAILED !!!", Toast.LENGTH_SHORT).show();
            }
        });

        // Set click listeners for the social icons
        googleIcon.setOnClickListener(v -> {
            // Open Gmail using an Intent
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://mail.google.com"));
            startActivity(intent);
        });

        fbIcon.setOnClickListener(v -> {
            // Open Facebook using an Intent
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com"));
            startActivity(intent);
        });

        twitterIcon.setOnClickListener(v -> {
            // Open Twitter using an Intent
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.twitter.com"));
            startActivity(intent);
        });
    }

    // Method to show the password reset dialog
    private void showPasswordResetDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.password_reset);

        // Find views in the dialog layout
        EditText resetEmailInput = dialog.findViewById(R.id.reset_email_input);
        Button resetButton = dialog.findViewById(R.id.reset_button);
        Button cancelButton = dialog.findViewById(R.id.cancel_button);

        // Set click listeners for buttons
        resetButton.setOnClickListener(v -> {
            // Handle password reset logic here
            String email = resetEmailInput.getText().toString();
            // Implement the logic to send a reset link to the email address
            // You can use Firebase Authentication, your backend, or an email service for this
            // For this example, we'll just dismiss the dialog
            dialog.dismiss();
            Toast.makeText(ActivityLogin.this, "Password reset link sent to " + email, Toast.LENGTH_SHORT).show();
        });

        cancelButton.setOnClickListener(v -> {
            // Dismiss the dialog
            dialog.dismiss();
        });

        // Show the dialog
        dialog.show();
    }
}



