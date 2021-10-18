package com.app.facerecognition.viewModel;

import android.content.Context;

import androidx.lifecycle.ViewModel;

import com.app.facerecognition.repository.SplashRepo;

public class SplashViewModel extends ViewModel {
    private Context context;
    private SplashRepo repo;

    public void init(Context context){
        this.context = context;
    }
}