package com.TFtest.RealtimeDetection;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.TextView;

public final class SurfaceViewCallback implements android.view.SurfaceHolder.Callback, Camera.PreviewCallback {

    private Context context;
    private static final String TAG = "Camera";
    private FrontCamera mFrontCamera = new FrontCamera();
    private boolean previewing = mFrontCamera.getPreviewing();
    private Camera mCamera;
    private FaceTask mFaceTask;
    private InputBitmap bitmap;

    SurfaceViewCallback(InputBitmap bitmap) {
        this.bitmap = bitmap;
        Log.i("SurfaceViewCallback", "classifier已传递给SurfaceViewCallback");
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
        if (previewing) {
            mCamera.stopPreview();
            Log.i(TAG, "停止预览");
        }

        try {
            mCamera.setPreviewDisplay(arg0);
            mCamera.startPreview();
            mCamera.setPreviewCallback(this);
            Log.i(TAG, "开始预览");
            //调用旋转屏幕时自适应
            //setCameraDisplayOrientation(MainActivity.this, mCurrentCamIndex, mCamera);
        } catch (Exception e) {
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        //初始化前置摄像头
        mFrontCamera.setCamera(mCamera);
        mCamera = mFrontCamera.initCamera();
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
//        for (int i = 0; i < parameters.getSupportedPreviewSizes().size(); i++) {
//            Log.e(TAG, "宽: " + parameters.getSupportedPreviewSizes().get(i).width + "高：" + parameters.getSupportedPreviewSizes().get(i).height);
//        }
        parameters.setPreviewSize(1280, 720);
        parameters.setPictureSize(1920, 1080);
        mCamera.setParameters(parameters);
        mCamera.setPreviewCallback(this);
        //适配竖排固定角度
        Log.i(TAG, "context: " + context.toString());
        Log.i(TAG, "mFrontCamera: " + mFrontCamera.toString());
        Log.i(TAG, "mCamera: " + mCamera.toString());
        FrontCamera.setCameraDisplayOrientation((Activity) context, mFrontCamera.getCurrentCamIndex(), mCamera);
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        mFrontCamera.StopCamera(mCamera);
    }

    /**
     * 相机实时数据的回调
     *
     * @param data   相机获取的数据，格式是YUV
     * @param camera 相应相机的对象
     */
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (mFaceTask != null) {
            switch (mFaceTask.getStatus()) {
                case RUNNING:
                    return;
                case PENDING:
                    mFaceTask.cancel(false);
                    break;
            }

        }
        mFaceTask = new FaceTask(data, camera, bitmap);
        mFaceTask.execute((Void) null);
        Log.i(TAG, "onPreviewFrame: 启动了Task");

    }

}