package com.app.facerecognition;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.app.facerecognition.viewModel.SplashViewModel;

import java.util.Objects;

public class SplashScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        Objects.requireNonNull(getSupportActionBar()).hide();

        SplashViewModel splashViewModel = new ViewModelProvider(this).get(SplashViewModel.class);
        splashViewModel.init(this);

        int SPLASH_TIME_OUT = 1700;
        new Handler().postDelayed(() ->{
            Intent intent = new Intent(SplashScreenActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }, SPLASH_TIME_OUT);
    }
}