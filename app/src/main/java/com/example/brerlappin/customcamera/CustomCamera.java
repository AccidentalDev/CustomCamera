package com.example.brerlappin.customcamera;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

public class CustomCamera extends Activity implements SurfaceHolder.Callback{
    private static final String TAG = "brerlappin.hardware";
    private static final int FILTER_NONE = 0;
    private static final int FILTER_AQUA = 1;
    private static final int FILTER_SEPIA = 2;
    private static final int FILTER_PSYCHO = 3;
    private static final int FILTER_NEGA = 4;
    private static final int FILTER_SOLAR = 5;
    private static final int FILTER_HDR = 6;

    private LayoutInflater layInflater = null;
    Camera sefCamera;
    byte[] tempData;
    boolean previewRunning = false;
    private SurfaceHolder surfaceHolder;
    private SurfaceView surfaceView;
    Button screenshot;
    boolean sendResult = false;
    int selectedFilter = 0;
    private Button filterButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_custom_camera);

        surfaceView = (SurfaceView) findViewById(R.id.surface);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        //surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        layInflater = LayoutInflater.from(this);
        View overView = layInflater.inflate(R.layout.cameraoverlay, null);
        this.addContentView(overView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT
        ));

        screenshot = (Button) findViewById(R.id.button);
        screenshot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sefCamera.takePicture(shutterCallback, null, someJPEG);
            }
        });

        filterButton = (Button) findViewById(R.id.mode_button);
        filterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchFilter();
            }
        });
    }

    public void switchFilter(){
        selectedFilter++;
        if(selectedFilter > FILTER_SOLAR)
            selectedFilter = FILTER_NONE;

        Camera.Parameters tmpCP = sefCamera.getParameters();
        setFilter(tmpCP);
        sefCamera.setParameters(tmpCP);
    }

    public void setFilter(Camera.Parameters campar){
        switch(selectedFilter){
            case FILTER_NONE:
                campar.setColorEffect(Camera.Parameters.EFFECT_NONE);
                campar.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
                filterButton.setText("Filter");
                break;
            case FILTER_AQUA:
                campar.setColorEffect(Camera.Parameters.EFFECT_AQUA);
                filterButton.setText("Aqua");
                break;
            case FILTER_NEGA:
                campar.setColorEffect(Camera.Parameters.EFFECT_NEGATIVE);
                filterButton.setText("Nega");
                break;
            case FILTER_PSYCHO:
                campar.setColorEffect(Camera.Parameters.EFFECT_BLACKBOARD);
                filterButton.setText("Psy");
                break;
            case FILTER_SEPIA:
                campar.setColorEffect(Camera.Parameters.EFFECT_SEPIA);
                filterButton.setText("Sepia");
                break;
            case FILTER_SOLAR:
                campar.setColorEffect(Camera.Parameters.EFFECT_SOLARIZE);
                filterButton.setText("Solar");
                break;
            case FILTER_HDR:
                //NOTE: Check what else is needed to make HDR mode work properly
                campar.setColorEffect(Camera.Parameters.EFFECT_NONE);
                campar.setSceneMode(Camera.Parameters.SCENE_MODE_HDR);
                filterButton.setText("HDR");
                break;
            default:
                Log.e(TAG, "ERROR: filder ID not recognized");
        }
    }

    Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
        @Override
        public void onShutter() {}
    };
    Camera.PictureCallback someJPEG = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] bytes, Camera camera) {
            if(bytes != null){
                tempData = bytes;
                done();
            }

            if(!sendResult){
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        sefCamera.startPreview();
                    }
                }).start();
            }
        }
    };
    public void done(){
        Bitmap tmpBmp = BitmapFactory.decodeByteArray(tempData, 0, tempData.length);
        String url = MediaStore.Images.Media.insertImage(
                getContentResolver(), tmpBmp, null, null);
        tmpBmp.recycle();

        Bundle bundle = new Bundle();
        if(url != null){
            if (sendResult) {
                bundle.putString("url", url);

                Intent intent = new Intent();
                intent.putExtras(bundle);
                setResult(RESULT_OK, intent);
            }
        }else{
            Toast.makeText(this, "Picture can't be saved", Toast.LENGTH_SHORT).show();
        }

        if (sendResult) {
            finish();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h){
        Log.d(TAG, "surfaceChanged");

        try{
            if(previewRunning){
                sefCamera.stopPreview();
                previewRunning = false;
            }

            Log.d(TAG,   "Setting preview size: W="+w+", H="+h);
            Camera.Parameters camp = sefCamera.getParameters();
            camp.setPreviewSize(w, h);

            //JFF
            //camp.setColorEffect(Camera.Parameters.EFFECT_SEPIA);
            //camp.setJpegQuality(100);
            camp.setPictureSize(w, h);
            setFilter(camp);

            sefCamera.setParameters(camp);
            sefCamera.setPreviewDisplay(holder);
            sefCamera.startPreview();

            previewRunning = true;
        }catch (Exception aeiou){
            Log.e("", aeiou.toString());
        }
    }
    @Override
    public void surfaceCreated(SurfaceHolder holder){
        Log.d(TAG, "surfaceCreated");
        sefCamera = Camera.open();
    }
    @Override
    public void surfaceDestroyed(SurfaceHolder holder){
        Log.d(TAG, "surfaceDestroyed");
        sefCamera.stopPreview();
        previewRunning = false;
        sefCamera.release();
        sefCamera = null;
    }
}
