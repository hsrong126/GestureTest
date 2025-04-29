package com.example.cameratest;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.chaquo.python.Python;
import com.chaquo.python.PyObject;
import com.chaquo.python.android.AndroidPlatform;

public class MainActivity extends AppCompatActivity {
    Button Go_btn;
    ImageView src_image, res_image;
    BitmapDrawable drawable;
    Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        /*
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



    }
}