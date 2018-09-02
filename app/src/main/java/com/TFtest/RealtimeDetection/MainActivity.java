package com.TFtest.RealtimeDetection;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceView;
import android.widget.TextView;

import java.util.List;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.baidu.speech.EventListener;
import com.baidu.speech.EventManager;
import com.baidu.speech.EventManagerFactory;
import com.baidu.speech.asr.SpeechConstant;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.view.MotionEvent;


public class MainActivity extends AppCompatActivity implements EventListener, TextToSpeech.OnInitListener {
    private Context context = MainActivity.this;
    private SurfaceView surfaceView;
    private CameraSurfaceHolder mCameraSurfaceHolder;
    private TextView textView;
    private InputBitmap bitmap = new InputBitmap();
    private Handler handler = new Handler();
    private TextToSpeech textToSpeech;
    private static final String pattern = "\"final_result\",\"best_result\":\"[a-zA-Z ]+\"";
    private static final String pattern1 = "\"final_result\",\"best_result\":";
    private Classifier classifier;
    boolean isClassifierClosed = false;
    private static final String MODEL_PATH = "ssd_mobilenet_v1_android_export.pb";
    private static final String LABEL_PATH = "file:///android_asset/coco_labels_list.txt";
    private static final int INPUT_SIZE = 300;
    private SearchObjectName objectName;
//    private int maxX;
//    private int maxY;
    protected TextView txtLog;
    protected Button speak;
    protected Button stop;
    //protected TextView txtResult;
//    private static String DESC_TEXT = "精简版识别，带有SDK唤醒运行的最少代码，仅仅展示如何调用，\n" +
//            "也可以用来反馈测试SDK输入参数及输出回调。\n" +
//            "本示例需要自行根据文档填写参数，可以使用之前识别示例中的日志中的参数。\n" +
//            "需要完整版请参见之前的识别示例。\n" +
//            "需要测试离线命令词识别功能可以将本类中的enableOffline改成true，首次测试离线命令词请联网使用。之后请说出“打电话给张三”";

    private static String DESC_TEXT = "";
    private EventManager asr;
    private boolean logTime = true;
    private boolean enableOffline = false; // 测试离线命令词，需要改成true
    boolean isStart = false;

    private void start() {
        txtLog.setText("");
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        String event = null;
        event = SpeechConstant.ASR_START; // 替换成测试的event

        if (enableOffline){
            params.put(SpeechConstant.DECODER, 2);
        }
        params.put(SpeechConstant.VAD_ENDPOINT_TIMEOUT,2000);
        params.put(SpeechConstant.PID, 1737); // 默认1536
        params.put(SpeechConstant.ACCEPT_AUDIO_VOLUME, false);
        params.put(SpeechConstant.OUT_FILE, "/storage/emulated/0/baiduASR/outfile.pcm");
        params.put(SpeechConstant.ACCEPT_AUDIO_DATA, true);
        params.put(SpeechConstant.DISABLE_PUNCTUATION,false);
        //  params.put(SpeechConstant.NLU, "enable");
        // params.put(SpeechConstant.VAD_ENDPOINT_TIMEOUT, 800);
        // params.put(SpeechConstant.VAD, SpeechConstant.VAD_DNN);
        //  params.put(SpeechConstant.PROP ,20000);
        String json = null; //可以替换成自己的json
        json = new JSONObject(params).toString(); // 这里可以替换成你需要测试的json
        //printLog("输入参数：" + json);
        asr.send(event, json, null, 0, 0);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.ENGLISH);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "数据丢失或不支持");
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        textToSpeech.stop(); // 不管是否正在朗读TTS都被打断
        textToSpeech.shutdown(); // 关闭，释放资源
    }

    private void stop(){
        asr.send(SpeechConstant.ASR_STOP, null, null, 0, 0); //
    }

    private void loadOfflineEngine() {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put(SpeechConstant.DECODER, 2);
        params.put(SpeechConstant.ASR_OFFLINE_ENGINE_GRAMMER_FILE_PATH, "assets://baidu_speech_grammar.bsg");
        asr.send(SpeechConstant.ASR_KWS_LOAD_ENGINE, new JSONObject(params).toString(), null, 0, 0);
    }

    private void unloadOfflineEngine() {
        asr.send(SpeechConstant.ASR_KWS_UNLOAD_ENGINE, null, null, 0, 0); //
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.textView);
        objectName = new SearchObjectName();
        initTensorFlowAndLoadModel();
        mCameraSurfaceHolder = new CameraSurfaceHolder(bitmap);
        textToSpeech = new TextToSpeech(this, this);
        initView();
        detectObject();
        initPermission();
        asr = EventManagerFactory.create(this, "asr");
        asr.registerListener(this); //  EventListener 中 onEvent方法
        speak.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                start();
            }
        });
        stop.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                stop();
            }
        });
        if (enableOffline) {
            loadOfflineEngine(); // 测试离线命令词请开启, 测试 ASR_OFFLINE_ENGINE_GRAMMER_FILE_PATH 参数时开启
        }
//        Display display = getWindowManager().getDefaultDisplay();
//        Point size = new Point();
//        display.getSize(size);
//        maxX = size.x;
//        maxY = size.y;
    }

    public void detectObject(){

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                if (objectName.getObjectName() != null) {
                    if (bitmap.getBitmap() != null && !isClassifierClosed) {
                        final List<Classifier.Recognition> results = classifier.recognizeImage(bitmap.getBitmap());
                        bitmap.setBitmap(null);
                        int i;
                        boolean isFind = false;
                        for (i = 0; i < results.size(); i++) {
                            if (results.get(i).getConfidence() > 0.4f && results.get(i).getTitle().equalsIgnoreCase(objectName.getObjectName())) {
                                Pattern regExp = Pattern.compile(results.get(i).getTitle());
                                Matcher matcher = regExp.matcher(objectName.getObjectName());
                                if (matcher.find()) {
                                    isFind = true;
                                    break;
                                }
                            } else if (results.get(i).getConfidence() <= 0.4f) {
                                textToSpeech.speak("not find", TextToSpeech.QUEUE_FLUSH, null);
                                break;
                            }
                            //textView.setText(String.format("results: %s", results.get(0).getTitle()));
                        }

                        if (isFind) {
                            //txtLog.setText(results.get(i).getTitle() + "/" + objectName.getObjectName());
                            textView.setText(String.format("results: %s", results.get(i)));
                            String position = null;
                            String walkforward = null;
                            float X = results.get(i).getLocation().centerY();

                            float W = results.get(i).getLocation().width();
                            float H = results.get(i).getLocation().height();

                            float P = (W * H) / 90000;
                            if (0 <= X && X <= 50) {
                                position = "two";
                            } else if (50 <= X && X <= 125) {
                                position = "one";
                            } else if (125 <= X && X <= 175) {
                                position = "twelve";
                            } else if (175 <= X && X <= 250) {
                                position = "eleven";
                            } else if (250 <= X && X <= 300) {
                                position = "ten";
                            }

                            if(0.8 < P && P < 1){
                                walkforward = " is nearby";
                            }
                            else if(0 < P && P < 0.8){
                                walkforward = " You need to get closer";
                            }
                            textToSpeech.speak(results.get(i).getTitle() + " is at " + position + walkforward, TextToSpeech.QUEUE_FLUSH, null);
                        } else {
                            //textView.setText(String.format("results: %s", results.get(0)));
                        }
//                        if (results.get(0).getConfidence() > 0.8f) {
//                            textView.setText(String.format("results: %s", results.get(0)));
//                            String position = null;
//                            float X = results.get(0).getLocation().centerX();
//                            if (0 <= X && X <= 37.5) {
//                                position = "eleven";
//                            } else if (37.5 <= X && X <= 112.5) {
//                                position = "eleven-thrity";
//                            } else if (112.5 <= X && X <= 187.5) {
//                                position = "twelve";
//                            } else if (187.5 <= X && X <= 262.5) {
//                                position = "twelve-thirty";
//                            } else if (262.5 <= X && X <= 300) {
//                                position = "one";
//                            }
//                            textToSpeech.speak(results.get(0).getTitle() + " is at " + position, TextToSpeech.QUEUE_FLUSH, null);
//                        }
                        Log.e("test", "启动了图像识别");
                    } else {
                        Log.e("test", "bitmap为null");
                    }
                }
                if (isClassifierClosed)
                    handler.removeCallbacks(this);
                else
                    handler.postDelayed(this, 3000);
            }
        };

        handler.postDelayed(runnable, 3000);
    }



    private void initTensorFlowAndLoadModel() {
        try {
            classifier = TensorFlowObjectDetectionAPIModel.create(
                    getAssets(),
                    MODEL_PATH,
                    LABEL_PATH,
                    INPUT_SIZE);
        } catch (final Exception e) {
            throw new RuntimeException("Error initializing TensorFlow!", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        classifier.close();
        isClassifierClosed = true;
        asr.send(SpeechConstant.ASR_CANCEL, "{}", null, 0, 0);
        if (enableOffline) {
            unloadOfflineEngine(); // 测试离线命令词请开启, 测试 ASR_OFFLINE_ENGINE_GRAMMER_FILE_PATH 参数时开启
        }
    }

    @Override
    public void onEvent(String name, String params, byte[] data, int offset, int length) {
        String logTxt = "name: " + name;


        if (params != null && !params.isEmpty()) {
            logTxt += " ;params :" + params;
        }
        if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_PARTIAL)) {
            if (params.contains("\"nlu_result\"")) {
                if (length > 0 && data.length > 0) {
                    //logTxt += ", 语义解析结果：" + new String(data, offset, length);
                }
            }
        } else if (data != null) {
            //logTxt += " ;data length=" + data.length;
        }
        processString(logTxt);
        //printLog(logTxt);
    }

    private void processString(String logTxt) {
        if (!logTxt.isEmpty()) {
            Pattern regExp = Pattern.compile(pattern);
            Matcher matcher = regExp.matcher(logTxt);
            if (matcher.find()) {
                String result = matcher.group(0);
                regExp = Pattern.compile(pattern1);
                matcher = regExp.matcher(result);
                result = matcher.replaceAll("");
                regExp = Pattern.compile("\"");
                matcher = regExp.matcher(result);
                result = matcher.replaceAll("");
                //printLog(result);
                objectName.setObjectName(result);
                txtLog.setText(objectName.getObjectName());
            }
        }
    }

    private void printLog(String text)
    {
//        if (logTime) {
//            text += "  ;time=" + System.currentTimeMillis();
//        }
        text += "\n";
        Log.i(getClass().getName(), text);
        txtLog.append(text + "\n");
    }

    public void initView()
    {
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView1);
        mCameraSurfaceHolder.setCameraSurfaceHolder(context,surfaceView);
        //txtResult = (TextView) findViewById(R.id.txtResult);
        txtLog = (TextView) findViewById(R.id.txtLog);
        speak = (Button) findViewById(R.id.speak);
        stop = (Button) findViewById(R.id.stop);
        txtLog.setText(DESC_TEXT + "\n");
    }

    private void initPermission()
    {
        String permissions[] = {Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        ArrayList<String> toApplyList = new ArrayList<String>();

        for (String perm : permissions) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, perm)) {
                toApplyList.add(perm);
                // 进入到这里代表没有权限.

            }
        }
        String tmpList[] = new String[toApplyList.size()];
        if (!toApplyList.isEmpty()) {
            ActivityCompat.requestPermissions(this, toApplyList.toArray(tmpList), 123);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // 此处为android 6.0以上动态授权的回调，用户自行实现。
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        float x;
        float y;
        try
        {
            if(event.getAction() == MotionEvent.ACTION_DOWN)
            {
                textToSpeech.speak("Start", TextToSpeech.QUEUE_FLUSH, null);
                start();
//                x = event.getX();
//                y = event.getY();
//                voiceRecg(x, y);
            }
            if (event.getAction() == MotionEvent.ACTION_UP)
            {
                stop();
                textToSpeech.speak("Stop", TextToSpeech.QUEUE_FLUSH, null);
            }
            return true;
        }
        catch(Exception e)
        {
            Log.v("touch", e.toString());
            return false;
        }
    }

//    private void voiceRecg(float x, float y){
//
//        if (x<maxX/2.0f){
//            textToSpeech.speak("Start", TextToSpeech.QUEUE_FLUSH, null);
//            start();
//            isStart = true;
//        }
//        if (isStart && x>=maxX/2.0f){
//
//        }
//    }

}
