package com.example.musebox.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.musebox.R;
import com.example.musebox.database.UserDatabaseHelper;
import com.example.musebox.models.User;
import com.example.musebox.utils.SessionManager;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText emailField, passwordField;
    private Button loginBtn, goToRegisterBtn;
    private UserDatabaseHelper dbHelper;
    private SessionManager sessionManager;

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize SessionManager
        sessionManager = new SessionManager(this);

        // Check session
        if (sessionManager.isLoggedIn()) {
            goToHome();
            return;
        }

        setContentView(R.layout.activity_login);
        emailField = findViewById(R.id.emailField);
        passwordField = findViewById(R.id.passwordField);
        loginBtn = findViewById(R.id.loginBtn);
        goToRegisterBtn = findViewById(R.id.goToRegisterBtn);

        // Initialize database helper
        dbHelper = new UserDatabaseHelper(this);

        // Handle Login request
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        // Handle Register request
        goToRegisterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        // Request notification permission for Android 13+
        requestNotificationPermission();
    }

    private void loginUser() {
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        // Check input fields
        if (TextUtils.isEmpty(email)) {
            emailField.setError("Please enter email");
            emailField.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailField.setError("Please enter valid email");
            emailField.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordField.setError("Please enter password");
            passwordField.requestFocus();
            return;
        }

        // Authorization check
        boolean isValidUser = dbHelper.checkUserLogin(email, password);

        if (isValidUser) {
            // Take user info
            User user = dbHelper.getUserByEmail(email);

            if (user != null) {
                // Save session
                sessionManager.createLoginSession(user.getId(), user.getUsername(), user.getEmail());

                Toast.makeText(this, "Login successful! Welcome " + user.getUsername(), Toast.LENGTH_SHORT).show();

                // Move to Home Activity
                goToHome();
            } else {
                Toast.makeText(this, "Login error: User is not registered", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show();
        }
    }

    private void goToHome() {
        // Move to HomeActivity
        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    private void requestNotificationPermission() {
        // For Android 13 (API 33) and above, we need to request POST_NOTIFICATIONS
        // permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Show rationale if needed
                if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                    new AlertDialog.Builder(this)
                            .setTitle("Notification Permission")
                            .setMessage(
                                    "MuseBox needs notification permission to show playback controls and song information while you're listening to music.")
                            .setPositiveButton("OK", (dialog, which) -> {
                                requestPermissions(
                                        new String[] { Manifest.permission.POST_NOTIFICATIONS },
                                        NOTIFICATION_PERMISSION_REQUEST_CODE);
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                } else {
                    // Request permission directly
                    requestPermissions(
                            new String[] { Manifest.permission.POST_NOTIFICATIONS },
                            NOTIFICATION_PERMISSION_REQUEST_CODE);
                }
            }
        }
        // For Android 8-12 (API 26-32), notification permission is automatically
        // granted
        // The notification channel is already created in MusicService, so notifications
        // will work
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notification permission denied. You won't see playback notifications.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}