package com.example.mqtt_clientapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class LoadAppScene extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash_scene);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(LoadAppScene.this, LoginScene.class);
            startActivity(intent);
            finish();
        }, 2000);
    }
}