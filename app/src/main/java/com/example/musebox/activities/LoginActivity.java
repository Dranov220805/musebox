package com.example.musebox.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Khởi tạo SessionManager
        sessionManager = new SessionManager(this);

        // Kiểm tra nếu đã login thì chuyển thẳng sang Home
        if (sessionManager.isLoggedIn()) {
            goToHome();
            return;
        }

        setContentView(R.layout.activity_login);

        // Khởi tạo views
        emailField = findViewById(R.id.emailField);
        passwordField = findViewById(R.id.passwordField);
        loginBtn = findViewById(R.id.loginBtn);
        goToRegisterBtn = findViewById(R.id.goToRegisterBtn);

        // Khởi tạo database helper
        dbHelper = new UserDatabaseHelper(this);

        // Xử lý sự kiện nút Login
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        // Xử lý sự kiện chuyển sang Register
        if (goToRegisterBtn != null) {
            goToRegisterBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Toast để debug
                    Toast.makeText(LoginActivity.this, "Register clicked!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                    startActivity(intent);
                }
            });
        } else {
            Toast.makeText(this, "goToRegisterBtn is NULL!", Toast.LENGTH_LONG).show();
        }
    }

    private void loginUser() {
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        // Kiểm tra các trường nhập liệu
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

        // Kiểm tra đăng nhập
        boolean isValidUser = dbHelper.checkUserLogin(email, password);

        if (isValidUser) {
            // Lấy thông tin user
            User user = dbHelper.getUserByEmail(email);

            if (user != null) {
                // Lưu session
                sessionManager.createLoginSession(user.getId(), user.getUsername(), user.getEmail());

                Toast.makeText(this, "Login successful! Welcome " + user.getUsername(), Toast.LENGTH_SHORT).show();

                // Chuyển sang màn hình Home
                goToHome();
            }
        } else {
            Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show();
        }
    }

    private void goToHome() {
        // Chuyển sang HomeActivity
        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
    }
}