package com.example.musebox.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import androidx.appcompat.app.AppCompatActivity;

import com.example.musebox.R;
import com.example.musebox.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Khởi tạo SessionManager
        sessionManager = new SessionManager(this);

        // Hide system bars (status + navigation)
        // hideSystemBars();

        // Delay 2 giây rồi kiểm tra session
        new Handler().postDelayed(() -> {
            checkLoginStatus();
        }, 2000);
    }

    private void checkLoginStatus() {
        Intent intent;

        // Kiểm tra xem user đã login chưa
        if (sessionManager.isLoggedIn()) {
            // Đã login → Chuyển sang HomeActivity
            intent = new Intent(SplashActivity.this, HomeActivity.class);
        } else {
            // Chưa login → Chuyển sang MainActivity (Login)
            intent = new Intent(SplashActivity.this, LoginActivity.class);
        }

        startActivity(intent);
        finish();
    }

//    private void hideSystemBars() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            // Android 11+
//            final WindowInsetsController controller = getWindow().getInsetsController();
//            if (controller != null) {
//                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
//                controller.setSystemBarsBehavior(
//                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
//                );
//            }
//        } else {
//            // Legacy support
//            View decorView = getWindow().getDecorView();
//            int flags = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                    | View.SYSTEM_UI_FLAG_FULLSCREEN;
//            decorView.setSystemUiVisibility(flags);
//        }
//    }
}