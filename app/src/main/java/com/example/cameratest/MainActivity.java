package com.example.cameratest;

import android.Manifest;                     // ← 加這行

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
    Button Go_btn;
    ImageView src_image, res_image;
    BitmapDrawable drawable;
    Bitmap bitmap;

    private PreviewView previewView;
    private ImageView  resultView;
    private ImageCapture imageCapture;
    private ImageAnalysis imageAnalysis;
    private ExecutorService cameraExecutor;

    private Python py;

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

        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        resultView  = findViewById(R.id.resultView);

        cameraExecutor = Executors.newSingleThreadExecutor();
        //startCamera();                      // 啟動 CameraX

        Button btn = findViewById(R.id.processBtn);
        btn.setOnClickListener(v -> captureAndProcess());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            cameraPermLauncher.launch(Manifest.permission.CAMERA);
        }

        //findViewById(R.id.processBtn).setOnClickListener(v -> captureAndProcess());

        /*
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        //初始化python环境
        if(!Python.isStarted()){
            Python.start(new AndroidPlatform(this));
        }
        Python python=Python.getInstance();
        //调用hello_python.py里面的Python_say_Hello函式
        PyObject pyObject=python.getModule("Hello");
        pyObject.callAttr("Python_say_Hello");
         */


        /*
        //初始化python环境
        if(!Python.isStarted()){
            Python.start(new AndroidPlatform(this));
        }
        //使用numpy计算两个矩阵的乘积
        Python py = Python.getInstance();
        //调用hello_python.py里面的matrix_multiply函式
        PyObject pyObj = py.getModule("Hello").get("matrix_multiply");
        //将matrix_multiply计算完的数值，换成java中的float类型
        float[][] result = pyObj.call().toJava(float[][].class);
        String resultStr = "";
        for (int i = 0; i < result.length; i++) {
            for (int j = 0; j < result[i].length; j++) {
                resultStr += result[i][j] + " ";
            }
            resultStr += "\n";
        }
        Log.d("Result", resultStr);
        */

        /*
        //初始化python环境
        if(!Python.isStarted()){
            Python.start(new AndroidPlatform(this));
        }
        //使用numpy生成随机数组，并计算其平均值和标准差。
        Python np = Python.getInstance();
        //调用hello_python.py里面的numpy_example函式
        PyObject npObj = np.getModule("Hello").get("numpy_example");
        PyObject npResult = npObj.call();
        //numpy_example的返回值有两个，将其分别转换成java中的float类型
        float mean = npResult.asList().get(0).toFloat();
        float std = npResult.asList().get(1).toFloat();
        Log.d("Result"," mean = "+ mean);
        Log.d("Result"," std = "+ std);
         */
        /*
        Go_btn = findViewById(R.id.Go_button);
        src_image = (ImageView) findViewById(R.id.source_imageview);
        res_image = (ImageView) findViewById(R.id.response_imageview);
        //初始化python环境
        if(!Python.isStarted()){
            Python.start(new AndroidPlatform(this));
        }
        Python python_cv = Python.getInstance();

        Go_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 获取源图片并转换为Bitmap对象
                drawable = (BitmapDrawable) src_image.getDrawable();
                bitmap = drawable.getBitmap();
                // 将Bitmap转换为byte[]对象
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] byteArray = stream.toByteArray();

                // 调用Python方法处理图片
                PyObject cvObject = python_cv.getModule("Hello");
                byte[] bytes = cvObject.callAttr("opencv_process_image",byteArray).toJava(byte[].class);

                // 将处理后的图片显示到画面上
                Bitmap bmp = BitmapFactory.decodeByteArray(bytes,0, bytes.length);
                res_image.setImageBitmap(bmp);
            }
        });

         */



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

                /*
                imageCapture = new ImageCapture.Builder()

                        .setBufferFormat(ImageFormat.YUV_420_888)   // ★ 需要 CameraX 1.3+
                        .setTargetResolution(new Size(640, 480))          // ← 新增
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                CameraSelector selector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this, selector, preview, imageCapture);

                 */
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

                CameraSelector selector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this, selector, preview, imageAnalysis);

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

                        /*
                        Bitmap bmp = nv21ToBitmap(nv21, width, height);
                        runOnUiThread(() -> {
                            resultView.setImageBitmap(bmp);
                            resultView.setVisibility(View.VISIBLE);
                            resultView.bringToFront();      // 讓它浮在 Preview 上
                            resultView.setRotation(90f);    // 如果需要
                        });

                        /*
                        runOnUiThread(() -> {
                            resultView.setImageBitmap(bmp);     // ← 不經過 Python
                            resultView.setVisibility(View.VISIBLE);
                        });
                        */

                        // 壓成 PNG bytes
                        //ByteArrayOutputStream os = new ByteArrayOutputStream();
                        //bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
                        //byte[] inputBytes = os.toByteArray();

                        // Python 處理 (背景執行)
                        cameraExecutor.execute(() -> {
                            try {
                                //PyObject mod = py.getModule("Hello");
                                //PyObject func = mod.get("process_image");
                                //PyObject result = func.call(inputBytes);

                                PyObject func = py.getModule("Hello").get("process_nv21");
                                // width、height 也一起傳給 Python
                                PyObject result = func.call(nv21, width, height);
                                byte[] outPng = result.toJava(byte[].class);

                                runOnUiThread(() -> {
                                    Bitmap outBmp = BitmapFactory.decodeByteArray(outPng, 0, outPng.length);
                                    resultView.setImageBitmap(outBmp);
                                    resultView.setVisibility(View.VISIBLE);
                                    resultView.bringToFront();   // ← 關鍵，把它蓋到最上層
                                    resultView.setRotation(90f);
                                });




                                //if (result == null) {
                                //    Log.e("PY", "process_image 回傳 null");
                                //    return;
                                //}

                                //byte[] outputBytes = result.toJava(byte[].class);
                                //Log.d("PY", "out bytes = " + outputBytes.length);

                                // 回到 UI Thread 更新畫面
                                //runOnUiThread(() -> {
                                //    Bitmap outBmp = BitmapFactory.decodeByteArray(
                                //            outputBytes, 0, outputBytes.length);
                                //    resultView.setImageBitmap(outBmp);
                                //    resultView.setVisibility(View.VISIBLE);
                                //    resultView.bringToFront();   // ← 關鍵，把它蓋到最上層
                                //    resultView.setRotation(90f);
                                //});

                                //runOnUiThread(() -> showResult(outputBytes));

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

    /** 把 ImageProxy (YUV_420_888) 轉為 Bitmap */
    private Bitmap imageProxyToBitmap(ImageProxy image) {
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        ByteBuffer yBuf = planes[0].getBuffer();
        ByteBuffer uBuf = planes[1].getBuffer();
        ByteBuffer vBuf = planes[2].getBuffer();

        int ySize = yBuf.remaining();
        int uSize = uBuf.remaining();
        int vSize = vBuf.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        yBuf.get(nv21, 0, ySize);
        vBuf.get(nv21, ySize, vSize);
        uBuf.get(nv21, ySize + vSize, uSize);

        YuvImage yuv = new YuvImage(
                nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuv.compressToJpeg(
                new Rect(0, 0, image.getWidth(), image.getHeight()), 100, out);
        byte[] jpegBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length);
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

    /**
     * 將一張 NV21 buffer 轉成 Bitmap（僅用於除錯顯示；正式可用 OpenCV Mat 直接處理）
     */
    public static Bitmap nv21ToBitmap(byte[] nv21, int width, int height) {
        YuvImage yuv = new YuvImage(nv21, ImageFormat.NV21, width, height, null);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // 品質 80 就很夠，100 反而慢
        yuv.compressToJpeg(new Rect(0, 0, width, height), 80, out);

        byte[] jpegBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    private void showResult(byte[] pngBytes) {
        if (pngBytes == null || pngBytes.length == 0) {
            Log.e("UI", "pngBytes is empty!");
            return;
        }
        Bitmap outBmp = BitmapFactory.decodeByteArray(pngBytes, 0, pngBytes.length);
        if (outBmp == null) {
            Log.e("UI", "BitmapFactory.decodeByteArray returned NULL");
            return;
        }
        Log.d("UI", "Bitmap w=" + outBmp.getWidth() + " h=" + outBmp.getHeight());
        resultView.setImageBitmap(outBmp);
        resultView.setVisibility(View.VISIBLE);
        // 若怕被 Preview 蓋到，保險起見叫它浮到最上層
        resultView.bringToFront();
    }

    private Bitmap downScale(Bitmap src, int maxSide) {
        int w = src.getWidth(), h = src.getHeight();
        float scale = Math.min(1f, maxSide / (float)Math.max(w, h));
        if (scale == 1f) return src;               // 已經夠小
        return Bitmap.createScaledBitmap(src,
                Math.round(w * scale), Math.round(h * scale), true);
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
                PyObject func = py.getModule("Hello").get("detect_gesture"); // ★ 你在 Python 寫的函式
                PyObject result = func.call(nv21, width, height);
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
                return;
            }
        }
    }

}


