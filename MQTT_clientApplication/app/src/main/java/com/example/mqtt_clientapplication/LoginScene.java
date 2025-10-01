package com.example.mqtt_clientapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class LoginScene extends AppCompatActivity {
    final String correct_username = "AndroidApp";
    final String correct_password = "Haitc007";
    public Button loginButton;
    public EditText username, password;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_scene);
        username = findViewById(R.id.edUsername);
        password = findViewById(R.id.edPassword);
        loginButton = findViewById(R.id.buttonLogin);

        //cheat for testing
        username.setText(correct_username);
        password.setText(correct_password);

        loginButton.setOnClickListener(v -> {
            if (!username.getText().toString().equals(correct_username) || !password.getText().toString().equals(correct_password)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(LoginScene.this);
                builder.setTitle("Login Failed");
                builder.setMessage("Username or password is incorrect");
                builder.setPositiveButton("OK", null);
                builder.show();
            }
            else {
                Intent intent = new Intent(LoginScene.this, SplashScene.class);
                intent.putExtra("username", username.getText().toString());
                intent.putExtra("password", password.getText().toString());
                startActivity(intent);
            }
        });
    }
}
