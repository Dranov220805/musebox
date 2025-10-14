package com.example.musebox.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.musebox.R;
import com.example.musebox.database.UserDatabaseHelper;
import com.example.musebox.models.User;
import com.google.android.material.textfield.TextInputEditText;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText usernameField, emailField, passwordField, confirmPasswordField;
    private Button registerBtn;
    private TextView goToLoginText;
    private UserDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        usernameField = findViewById(R.id.usernameField);
        emailField = findViewById(R.id.emailField);
        passwordField = findViewById(R.id.passwordField);
        confirmPasswordField = findViewById(R.id.confirmPasswordField);
        registerBtn = findViewById(R.id.registerBtn);
        goToLoginText = findViewById(R.id.goToLoginText);

        // Initialize database helper
        dbHelper = new UserDatabaseHelper(this);

        // Handle register request
        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        // Handle login redirect
        goToLoginText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void registerUser() {
        String username = usernameField.getText().toString().trim();
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();
        String confirmPassword = confirmPasswordField.getText().toString().trim();

        // Check input fields
        if (TextUtils.isEmpty(username)) {
            usernameField.setError("Please enter username");
            usernameField.requestFocus();
            return;
        }

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

        if (password.length() < 6) {
            passwordField.setError("Password must be at least 6 characters");
            passwordField.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordField.setError("Please confirm password");
            confirmPasswordField.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordField.setError("Passwords do not match");
            confirmPasswordField.requestFocus();
            return;
        }

        // Check duplicate email or not
        if (dbHelper.checkEmailExists(email)) {
            emailField.setError("Email already exists");
            emailField.requestFocus();
            return;
        }

        // Check duplicate username or not
        if (dbHelper.checkUsernameExists(username)) {
            usernameField.setError("Username already exists");
            usernameField.requestFocus();
            return;
        }

        // Create new user and save to database
        User newUser = new User(username, email, password);
        boolean isRegistered = dbHelper.addUser(newUser);

        if (isRegistered) {
            Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();

            // Redirect to login activity
            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Registration failed. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }
}