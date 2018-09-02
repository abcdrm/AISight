package com.TFtest.RealtimeDetection;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * 单独的任务类。继承AsyncTask，来处理从相机实时获取的耗时操作
 */
public class FaceTask extends AsyncTask{
    private byte[] mData;
    private Camera mCamera;
    private static final String TAG = "CameraTag";
    private static final int INPUT_SIZE = 300;
    private InputBitmap bitmap;

    //构造函数
    FaceTask(byte[] data, Camera camera, InputBitmap bitmap)
    {
        this.mData = data;
        this.mCamera = camera;
        this.bitmap = bitmap;
        Log.i("FaceTask", "classifier已传递给FaceTask");
    }
    @Override
    protected Object doInBackground(Object[] params) {
        Camera.Parameters parameters = mCamera.getParameters();
        int imageFormat = parameters.getPreviewFormat();
        int w = parameters.getPreviewSize().width;
        int h = parameters.getPreviewSize().height;

        Rect rect = new Rect(0, 0, w, h);
        YuvImage yuvImg = new YuvImage(mData, imageFormat, w, h, null);
        try {
            ByteArrayOutputStream outputstream = new ByteArrayOutputStream();
            yuvImg.compressToJpeg(rect, 100, outputstream);
            Bitmap rawbitmap = BitmapFactory.decodeByteArray(outputstream.toByteArray(), 0, outputstream.size());
            rawbitmap = Bitmap.createBitmap(rawbitmap, (w-h)/2, 0, h, h);
            rawbitmap = Bitmap.createScaledBitmap(rawbitmap, INPUT_SIZE, INPUT_SIZE, false);
            bitmap.setBitmap(rawbitmap);
//            if (classifier == null) {
//                //Log.e(TAG, "classifier为null");
//            } else {
//                final List<Classifier.Recognition> results = classifier.recognizeImage(rawbitmap);
//                if (results.get(0).getConfidence() >= 0.70f) {
//                    textView.setText(String.format("results: %s", results.get(0)));
//                }
//            }
            //若要存储可以用下列代码，格式为jpg
            /* BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(Environment.getExternalStorageDirectory().getPath()+"/fp.jpg"));
            img.compressToJpeg(rect, 100, bos);
            bos.flush();
            bos.close();
            mCamera.startPreview();
            */
        }
        catch (Exception e)
        {
            Log.e(TAG, "onPreviewFrame: 获取相机实时数据失败" + e.getLocalizedMessage());
        }
        return null;
    }
}
