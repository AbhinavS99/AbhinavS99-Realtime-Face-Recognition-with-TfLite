package com.app.facerecognition.repository;

import android.content.Context;

public class SplashRepo {
    private static SplashRepo repo;
    private static Context mContext;

    public static SplashRepo getInstance(Context context){
        mContext = context;
        if(repo == null){
            repo = new SplashRepo();
        }
        return repo;
    }
}