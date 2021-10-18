package com.app.facerecognition.viewModel;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.view.View;

import androidx.databinding.ObservableField;
import androidx.lifecycle.ViewModel;

import com.app.facerecognition.classifier.SimilarityClassifier;
import com.app.facerecognition.repository.MainRepo;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;

public class MainViewModel extends ViewModel {
    private MainRepo repo;
    private ObservableField<Integer> addFaceVisibility = new ObservableField<>();
    private ObservableField<Integer> facePreviewVisibility = new ObservableField<>();
    private ObservableField<Integer> recoNameVisibility = new ObservableField<>();


    private ObservableField<String> previewInfoText = new ObservableField<>();
    private ObservableField<String> recognizeText = new ObservableField<>();
    private ObservableField<String> recoNameText = new ObservableField<>();


    public void init(Context context){
        repo = MainRepo.getInstance(context);
        addFaceVisibility.set(View.INVISIBLE);
        facePreviewVisibility.set(View.INVISIBLE);
        previewInfoText.set("\n    Recognized Face:");
        recognizeText.set("Add Face");
    }

    public HashMap<String, SimilarityClassifier.Recognition> readFromSP(){
        return repo.readFromSP();
    }

    public void insertToSP(HashMap<String, SimilarityClassifier.Recognition> jsonMap,boolean clear, HashMap<String, SimilarityClassifier.Recognition> registered){
        repo.insertToSP(jsonMap, clear, registered);
    }

    public Interpreter getModel(Activity activity) throws IOException{
        String modelFile="mobile_face_net.tflite";
        return new Interpreter(loadModelFile(activity, modelFile));

    }

    public FaceDetector getDetector(){
        FaceDetectorOptions highAccuracyOpts = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .build();

        return FaceDetection.getClient(highAccuracyOpts);
    }


    public MappedByteBuffer loadModelFile(Activity activity, String MODEL_FILE) throws IOException{
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(MODEL_FILE);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public ObservableField<Integer> getAddFaceVisibility() {
        return addFaceVisibility;
    }

    public ObservableField<Integer> getFacePreviewVisibility() {
        return facePreviewVisibility;
    }

    public ObservableField<Integer> getRecoNameVisibility() {
        return recoNameVisibility;
    }

    public ObservableField<String> getPreviewInfoText() {
        return previewInfoText;
    }

    public ObservableField<String> getRecognizeText() {
        return recognizeText;
    }

    public ObservableField<String> getRecoNameText() {
        return recoNameText;
    }

    public void view_call(int flag) {
        if(flag == 1){
            recognizeText.set("Add Face");
            addFaceVisibility.set(View.INVISIBLE);
            recoNameVisibility.set(View.VISIBLE);
            facePreviewVisibility.set(View.INVISIBLE);
            previewInfoText.set("\n    Recognized Face:");
        }else if(flag == 2){
            recognizeText.set("Recognize");
            addFaceVisibility.set(View.VISIBLE);
            recoNameVisibility.set(View.INVISIBLE);
            facePreviewVisibility.set(View.VISIBLE);
            previewInfoText.set("1.Bring Face in view of Camera.\n\n2.Your Face preview will appear here.\n\n3.Click Add button to save face.");
        }
    }
}
