package com.TFtest.RealtimeDetection;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

/**
 * 相机界面SurfaceView的Holder类
 */
public class CameraSurfaceHolder {
    private Context context;
    private SurfaceHolder surfaceHolder;
    private SurfaceView surfaceView;
    private SurfaceViewCallback callback;
    private InputBitmap bitmap;

    CameraSurfaceHolder(InputBitmap bitmap) {
        this.bitmap = bitmap;
        Log.i("CameraSurfaceHolder", "classifier已传递给CameraSurfaceHolder");
    }

    /**
    * 设置相机界面SurfaceView的Holder
     * @param context 从相机所在的Activity传入的context
     * @param surfaceView Holder所绑定的响应的SurfaceView
    * */
    public void setCameraSurfaceHolder(Context context, SurfaceView surfaceView) {
        this.context = context;
        this.surfaceView = surfaceView;
        callback = new SurfaceViewCallback(bitmap);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(callback);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        callback.setContext(context);
    }

}


