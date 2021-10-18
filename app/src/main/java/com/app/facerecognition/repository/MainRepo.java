package com.app.facerecognition.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.app.facerecognition.classifier.SimilarityClassifier;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.content.Context.MODE_PRIVATE;

public class MainRepo {
    private static MainRepo repo;
    private static Context mContext;

    public static MainRepo getInstance(Context context){
        mContext = context;
        if(repo == null){
            repo = new MainRepo();
        }
        return repo;
    }

    public HashMap<String, SimilarityClassifier.Recognition> readFromSP(){
        int OUTPUT_SIZE = 192;
        SharedPreferences sharedPreferences = mContext.getSharedPreferences("HashMap", MODE_PRIVATE);
        String defValue = new Gson().toJson(new HashMap<String, SimilarityClassifier.Recognition>());
        String json=sharedPreferences.getString("map",defValue);

        TypeToken<HashMap<String,SimilarityClassifier.Recognition>> token = new TypeToken<HashMap<String,SimilarityClassifier.Recognition>>() {};
        HashMap<String,SimilarityClassifier.Recognition> retrievedMap=new Gson().fromJson(json,token.getType());

        for (Map.Entry<String, SimilarityClassifier.Recognition> entry : retrievedMap.entrySet()) {
            float[][] output=new float[1][OUTPUT_SIZE];
            ArrayList arrayList= (ArrayList) entry.getValue().getExtra();
            arrayList = (ArrayList) arrayList.get(0);
            for (int counter = 0; counter < arrayList.size(); counter++) {
                output[0][counter]= ((Double) arrayList.get(counter)).floatValue();
            }
            entry.getValue().setExtra(output);
        }
        Toast.makeText(mContext, "Recognitions Loaded", Toast.LENGTH_SHORT).show();
        return retrievedMap;
    }

    public void insertToSP(HashMap<String, SimilarityClassifier.Recognition> jsonMap,boolean clear, HashMap<String, SimilarityClassifier.Recognition> registered) {
        if(clear)
            jsonMap.clear();
        else
            jsonMap.putAll(registered);
        String jsonString = new Gson().toJson(jsonMap);
        SharedPreferences sharedPreferences = mContext.getSharedPreferences("HashMap", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("map", jsonString);
        editor.apply();
        Toast.makeText(mContext, "Recognitions Saved", Toast.LENGTH_SHORT).show();
    }

}
