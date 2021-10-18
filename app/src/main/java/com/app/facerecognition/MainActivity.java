package com.app.facerecognition;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.InputType;
import android.util.Pair;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import com.app.facerecognition.callBack.MainCallBack;
import com.app.facerecognition.classifier.SimilarityClassifier;
import com.app.facerecognition.databinding.ActivityMainBinding;
import com.app.facerecognition.utils.BitmapUtils;
import com.app.facerecognition.utils.ImageRecognitionUtils;
import com.app.facerecognition.viewModel.MainViewModel;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetector;

import org.tensorflow.lite.Interpreter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity implements MainCallBack {
    private ActivityMainBinding binding;
    private MainViewModel viewModel;
    private Interpreter tfLite;
    private FaceDetector detector;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ProcessCameraProvider cameraProvider;
    private int cam_face = CameraSelector.LENS_FACING_BACK;
    private static final int MY_CAMERA_REQUEST_CODE = 100;

    private HashMap<String, SimilarityClassifier.Recognition> registered = new HashMap<>();
    private boolean flipX = false;
    private boolean start = true;
    
    private float[][] embeddings;
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        Objects.requireNonNull(getSupportActionBar()).setTitle(Html.fromHtml("<font color='#FB4E00'>Face Recognition App</font>"));
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        viewModel.init(this);
        binding.setContract(this);
        binding.setViewModel(viewModel);

        checkCameraPermission();
        registered = viewModel.readFromSP();
        try {
            tfLite = viewModel.getModel(MainActivity.this);
        }catch (IOException e){
            e.printStackTrace();
        }

        detector = viewModel.getDetector();
        cameraBind();
    }

    @Override
    public void onActionClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Select Action:");
        String[] names= {"View Recognition List","Save Recognitions","Load Recognitions","Clear All Recognitions"};

        builder.setItems(names, (dialog, which) -> {
            switch (which) {
                case 0:
                    displayNameListView();
                    break;
                case 1:
                    viewModel.insertToSP(registered,false, viewModel.readFromSP());
                    break;
                case 2:
                    registered.putAll(viewModel.readFromSP());
                    break;
                case 3:
                    clearNameList();
                    break;
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onCameraSwitchClick() {
        if(cam_face == CameraSelector.LENS_FACING_BACK){
            cam_face = CameraSelector.LENS_FACING_FRONT;
            flipX = true;
        }else{
            cam_face = CameraSelector.LENS_FACING_BACK;
            flipX = false;
        }
        cameraProvider.unbindAll();
        cameraBind();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onRecognizeClick() {
        if(binding.recognize.getText().toString().equals("Recognize")){
            start = true;
            viewModel.view_call(1);
        }else{
            viewModel.view_call(2);
        }
    }

    @Override
    public void onAddFaceClick() {
        addFace();
    }

    private void cameraBind(){
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException ignored) {

            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider){
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(cam_face)
                .build();

        ImageAnalysis imageAnalysis = ImageRecognitionUtils.getImageAnalysis(preview, binding);
        Executor executor = Executors.newSingleThreadExecutor();

        imageAnalysis.setAnalyzer(executor, imageProxy -> {
            InputImage image = null;
            @SuppressLint("UnsafeExperimentalUsageError")
            Image mediaImage = imageProxy.getImage();

            if(mediaImage != null){
                image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
            }

            assert image != null;
            @SuppressLint("SetTextI18n") Task<List<Face>> result =
                    detector.process(image).addOnSuccessListener(faces -> {
                        if(faces.size() != 0){
                            Face face = faces.get(0);
                            Bitmap frame_bmp = BitmapUtils.toBitmap(mediaImage);
                            int rot = imageProxy.getImageInfo().getRotationDegrees();
                            Bitmap frame_bmp1 = BitmapUtils.rotateBitmap(frame_bmp, rot, false, false);

                            RectF boundingBox = new RectF(face.getBoundingBox());
                            Bitmap cropped_face = BitmapUtils.getCropBitmapByCPU(frame_bmp1, boundingBox);

                            if(flipX){
                                cropped_face = BitmapUtils.rotateBitmap(cropped_face, 0, true, false);
                            }
                            Bitmap scaled = BitmapUtils.getResizedBitmap(cropped_face, 112, 112);
                            if(start){
                                recognizeImage(scaled, binding, tfLite, registered);
                            }
                            try{
                                Thread.sleep(10);
                            }catch (InterruptedException e){
                                e.printStackTrace();
                            }
                        }else{
                            if(registered.isEmpty()){
                                viewModel.getRecoNameText().set("Add Face");
                            }else{
                                viewModel.getRecoNameText().set("No Face Detected");
                            }
                        }
                    })
                    .addOnCompleteListener(task -> {
                        imageProxy.close();
                    });
        });
        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageAnalysis, preview);
    }

    public void recognizeImage(final Bitmap bitmap, ActivityMainBinding binding, Interpreter tfLite, HashMap<String, SimilarityClassifier.Recognition> registered){
        int inputSize = 112;
        int OUTPUT_SIZE = 192;
        float IMAGE_MEAN = 128.0f;
        float IMAGE_STD = 128.0f;

        binding.facePreview.setImageBitmap(bitmap);
        ByteBuffer imgData = ImageRecognitionUtils.getImgData(inputSize, bitmap, IMAGE_MEAN, IMAGE_STD);
        Object[] inputArray = {imgData};
        Map<Integer, Object> outputMap = new HashMap<>();
        embeddings = new float[1][OUTPUT_SIZE];

        outputMap.put(0, embeddings);
        tfLite.runForMultipleInputsOutputs(inputArray, outputMap);

        float distance;

        if(registered.size() > 0){
            final Pair<String, Float> nearest = ImageRecognitionUtils.findNearest(embeddings[0], registered);
            if(nearest != null){
                final String name = nearest.first;
                distance = nearest.second;
                if(distance < 1.000f){
                    viewModel.getRecoNameText().set(name);
                }else{
                    viewModel.getRecoNameText().set("Unknown");
                }
            }
        }
    }

    private void addFace(){
        start=false;
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Enter Name");
        final EditText input = new EditText(MainActivity.this);
        input.setInputType(InputType.TYPE_CLASS_TEXT );
        builder.setView(input);

        builder.setPositiveButton("ADD", (dialog, which) -> {
            SimilarityClassifier.Recognition result = new SimilarityClassifier.Recognition(
                    "0", "", -1f);
            result.setExtra(embeddings);
            registered.put( input.getText().toString(),result);
            start=true;

        });
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            start=true;
            dialog.cancel();
        });
        builder.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkCameraPermission() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_REQUEST_CODE);
        }
    }

    private  void clearNameList() {
        AlertDialog.Builder builder =new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Do you want to delete all Recognitions?");
        builder.setPositiveButton("Delete All", (dialog, which) -> {
            registered.clear();
            Toast.makeText(MainActivity.this, "Recognitions Cleared", Toast.LENGTH_SHORT).show();
        });
        viewModel.insertToSP(registered,true, viewModel.readFromSP());
        builder.setNegativeButton("Cancel",null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void displayNameListView() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        if(registered.isEmpty())
            builder.setTitle("No Faces Added!!");
        else
            builder.setTitle("Recognitions:");
        String[] names= new String[registered.size()];
        boolean[] checkedItems = new boolean[registered.size()];
        int i=0;
        for (Map.Entry<String, SimilarityClassifier.Recognition> entry : registered.entrySet()) {
            names[i]=entry.getKey();
            checkedItems[i]=false;
            i=i+1;
        }
        builder.setItems(names,null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission granted", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

}