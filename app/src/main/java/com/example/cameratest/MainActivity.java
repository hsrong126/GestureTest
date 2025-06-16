package com.example.cameratest;

import android.Manifest;                     // ← 加這行

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// --- CameraX 與註解 ---
import androidx.camera.core.ImageProxy;
import androidx.camera.core.ImageCaptureException;
import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;      // ★ 新增
import androidx.camera.core.ImageProxy;


// --- 影像格式轉換 ---
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.widget.Toast;

import java.nio.ByteBuffer;   // imageProxyToBitmap 會用到


import com.chaquo.python.Python;
import com.chaquo.python.PyObject;
import com.chaquo.python.android.AndroidPlatform;
import com.google.common.util.concurrent.ListenableFuture;

public class MainActivity extends AppCompatActivity {
    private PreviewView previewView;
    private ImageView  resultView;
    private ImageCapture imageCapture;
    private ImageAnalysis imageAnalysis;
    private ExecutorService cameraExecutor;

    private Python py;
    private PyObject module;

    private final ActivityResultLauncher<String> cameraPermLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    startCamera();     // ← 僅在拿到權限後才綁定 CameraX
                } else {
                    Toast.makeText(this, "需要攝影機權限才能運作", Toast.LENGTH_LONG).show();
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 啟動 Chaquopy Python
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        py = Python.getInstance();
        module = py.getModule("Hello");

        // 複製模型檔案並獲取路徑
        String[] modelFiles = {
                "opencv_face_detector.pbtxt", "opencv_face_detector_uint8.pb",
                "age_deploy.prototxt", "age_net.caffemodel",
                "gender_deploy.prototxt", "gender_net.caffemodel"
        };
        String[] modelPaths = copyModelFiles(modelFiles);

        // 載入模型
        if (!loadModel(modelPaths)) {
            Toast.makeText(this, "Failed to load models", Toast.LENGTH_LONG).show();
            return;
        }

        setContentView(R.layout.activity_main);
        previewView = findViewById(R.id.previewView);
        resultView  = findViewById(R.id.resultView);
        Button btn = findViewById(R.id.processBtn);
        btn.setOnClickListener(v -> captureAndProcess());

        cameraExecutor = Executors.newSingleThreadExecutor();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            cameraPermLauncher.launch(Manifest.permission.CAMERA);
        }

    }

    /** 初始化 CameraX：Preview + ImageCapture */
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);

        future.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = future.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // ★ 新的即時分析 Use-case
                imageAnalysis = new ImageAnalysis.Builder()
                    .setTargetResolution(new Size(640, 480))              // 解析度決定速度
                    .setBackpressureStrategy(
                            ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)      // 只保留最新影格
                    .setOutputImageFormat(
                            ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build();

                // ★ 指定 Analyzer
                imageAnalysis.setAnalyzer(cameraExecutor, new EdgeAnalyzer());

                // ★ 指定 自拍 (DEFAULT_FRONT_CAMERA) 或 後相機 (DEFAULT_BACK_CAMERA)
                CameraSelector selector = CameraSelector.DEFAULT_FRONT_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, selector, preview, imageAnalysis);

            } catch (Exception e) {
                Log.e("CameraX", "Failed to bind camera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    /** 按下按鈕擷取一幀 → Python 處理 → 回傳顯示 */
    private void captureAndProcess() {
        imageCapture.takePicture(cameraExecutor,
            new ImageCapture.OnImageCapturedCallback() {

                @Override
                public void onCaptureSuccess(@NonNull ImageProxy image) {
                    //Bitmap bmp = imageProxyToBitmap(image);
                    byte[] nv21 = imageProxyToNv21(image);
                    int width  = image.getWidth();
                    int height = image.getHeight();
                    image.close();

                    // Python 處理 (背景執行)
                    cameraExecutor.execute(() -> {
                        try {

                            PyObject result = module.callAttr("process_nv21", nv21, width, height);
                            byte[] outPng = result.toJava(byte[].class);

                            runOnUiThread(() -> {
                                Bitmap outBmp = BitmapFactory.decodeByteArray(outPng, 0, outPng.length);
                                resultView.setImageBitmap(outBmp);
                                resultView.setVisibility(View.VISIBLE);
                                resultView.bringToFront();   // ← 關鍵，把它蓋到最上層
                                resultView.setRotation(90f);
                            });

                        } catch (Exception e) {
                            Log.e("Python", "Error in Python call", e);
                        }
                    });
                }

                @Override
                public void onError(@NonNull ImageCaptureException exc) {
                    Log.e("CameraX", "Capture error", exc);
                }
            });
    }
    /** 正確處理 rowStride / pixelStride，把 YUV_420_888 轉成 NV21 */
    public static byte[] imageProxyToNv21(ImageProxy image) {
        int w = image.getWidth();
        int h = image.getHeight();

        int ySize  = w * h;
        int uvSize = w * h / 2;              // NV21: ¼ 像素數 ×2 色差 = ½ Y
        byte[] nv21 = new byte[ySize + uvSize];

        // ---------- Y ----------
        ImageProxy.PlaneProxy yPlane = image.getPlanes()[0];
        ByteBuffer yBuf = yPlane.getBuffer();
        int yRowStride = yPlane.getRowStride();

        int pos = 0;
        for (int r = 0; r < h; r++) {
            yBuf.position(r * yRowStride);
            yBuf.get(nv21, pos, w);
            pos += w;
        }

        // ---------- VU ----------
        ImageProxy.PlaneProxy uPlane = image.getPlanes()[1];
        ImageProxy.PlaneProxy vPlane = image.getPlanes()[2];
        ByteBuffer uBuf = uPlane.getBuffer();
        ByteBuffer vBuf = vPlane.getBuffer();
        int rowStrideUV   = uPlane.getRowStride();     // 兩平面相同
        int pixelStrideUV = uPlane.getPixelStride();   // 多數機型 = 2

        for (int r = 0; r < h / 2; r++) {
            int rowStart = r * rowStrideUV;
            for (int c = 0; c < w / 2; c++) {
                int uIdx = rowStart + c * pixelStrideUV;
                int vIdx = rowStart + c * pixelStrideUV;

                nv21[pos++] = vBuf.get(vIdx);   // ★ NV21：V 在前
                nv21[pos++] = uBuf.get(uIdx);   // ★          U 在後
            }
        }
        return nv21;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    private class EdgeAnalyzer implements ImageAnalysis.Analyzer {

        @Override
        public void analyze(@NonNull ImageProxy image) {
            // 1) 取 Y (灰階) → ByteArray
            byte[] nv21 = imageProxyToNv21(image);
            int width  = image.getWidth();
            int height = image.getHeight();
            image.close();

            // 2) 丟給 Python 做 Canny
            byte[] pngEdge;
            try {
                // ★ 你在 Python 寫的函式
                PyObject result = module.callAttr("process_nv21_for_age_gender", nv21, width, height);
                byte[] outPng = result.toJava(byte[].class);
                // 3) UI Thread 顯示

                Bitmap outBmp = BitmapFactory.decodeByteArray(outPng, 0, outPng.length);
                runOnUiThread(() -> {
                    resultView.setImageBitmap(outBmp);
                    resultView.setVisibility(View.VISIBLE);
                    resultView.bringToFront();
                    resultView.setRotation(90f);       // 視需要旋轉
                });

            } catch (Exception e) {
                Log.e("PY", "python error", e);
            }
        }
    }


    private String[] copyModelFiles(String[] modelFiles) {
        String[] paths = new String[modelFiles.length];
        for (int i = 0; i < modelFiles.length; i++) {
            File destFile = new File(getFilesDir(), modelFiles[i]);
            if (!destFile.exists()) {
                try (InputStream is = getAssets().open(modelFiles[i]);
                     FileOutputStream fos = new FileOutputStream(destFile)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = is.read(buffer)) > 0) {
                        fos.write(buffer, 0, length);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            paths[i] = destFile.getAbsolutePath();
        }
        return paths;
    }

    private boolean loadModel(String[] modelPaths) {
        try {
            PyObject result = module.callAttr("load_model",
                    modelPaths[0], modelPaths[1], // faceProto, faceModel
                    modelPaths[2], modelPaths[3], // ageProto, ageModel
                    modelPaths[4], modelPaths[5]  // genderProto, genderModel
            );
            return result.toBoolean();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}