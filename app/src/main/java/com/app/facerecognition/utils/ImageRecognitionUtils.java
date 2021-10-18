package com.app.facerecognition.utils;

import android.graphics.Bitmap;
import android.util.Pair;
import android.util.Size;

import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;

import com.app.facerecognition.classifier.SimilarityClassifier;
import com.app.facerecognition.databinding.ActivityMainBinding;

import org.tensorflow.lite.Interpreter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

public class ImageRecognitionUtils {

    public static Pair<String, Float> findNearest(float[] emb, HashMap<String, SimilarityClassifier.Recognition> registered){
        Pair<String, Float> ret = null;
        for(Map.Entry<String, SimilarityClassifier.Recognition> entry: registered.entrySet()){
            final String name = entry.getKey();
            final float[] knownEmb = ((float[][]) entry.getValue().getExtra())[0];
            float distance = 0;

            for(int i=0; i<emb.length; i++){
                float diff = emb[i] - knownEmb[i];
                distance += diff*diff;
            }
            distance = (float) Math.sqrt(distance);
            if (ret == null || distance < ret.second) {
                ret = new Pair<>(name, distance);
            }
        }
        return ret;
    }

    public static ImageAnalysis getImageAnalysis(Preview preview, ActivityMainBinding binding){
        preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());
        return new ImageAnalysis.Builder()
                .setTargetResolution(new Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
    }

    public static ByteBuffer getImgData(int inputSize, Bitmap bitmap, float IMAGE_MEAN, float IMAGE_STD){
        ByteBuffer imgData = ByteBuffer.allocateDirect(inputSize * inputSize * 3 * 4);

        imgData.order(ByteOrder.nativeOrder());
        int [] intValues = new int[inputSize*inputSize];

        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        imgData.rewind();

        for(int i=0; i<inputSize; ++i){
            for(int j=0; j<inputSize; ++j){
                int pixelValue =  intValues[i*inputSize + j];
                imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
            }
        }
        return  imgData;
    }
}
