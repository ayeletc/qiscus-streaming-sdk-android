package com.qiscus.streaming.ui.activity;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.pedro.rtplibrary.rtmp.RtmpCamera1;
import com.qiscus.streaming.R;
import com.qiscus.streaming.data.QiscusStreamParameter;

import net.ossrs.rtmp.ConnectCheckerRtmp;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

//ayelet
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
//
public class QiscusVideoStreamActivity extends AppCompatActivity implements ConnectCheckerRtmp, SurfaceHolder.Callback {
    private static final String TAG = QiscusVideoStreamActivity.class.getSimpleName();
    private static final String HTTP_PORT = "1937";

    private Socket mSocket;

    private String[] permissions = {
            Manifest.permission.CAMERA,
//            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private static String streamUrl;
    private static QiscusStreamParameter streamParameter;
    private RtmpCamera1 rtmpCamera;
    private ViewGroup rootView;
    private SurfaceView surfaceView;
    private Button broadcast;
    private TextView streamLiveStatus;
    private TimerHandler timerHandler;
    private Timer timer;
    private long elapsedTime;


    //ayelet - TTS
    private TextToSpeech myTTS;
    private int MY_DATA_CHECK_CODE = 0;
    private boolean absEnableTTS = true; //enable the voice notification absolutely
    private boolean enableTTS = false; //disable the voice notification in case
//    private boolean speaking = false; // to enable speaking once in 2 sec
    // that the app is not processing
    private boolean inGreenLight = false;
    private boolean inRedLight = false;
    private boolean changeEvents = true; // to enable speaking only when switching events
    private MediaPlayer mMediaPlayerWalk; // media player for walk sound
    private MediaPlayer mMediaPlayerDontWalk; // media player for don't-walk sound

    //

    public static Intent generateIntent(Context context, String url, QiscusStreamParameter parameter) {
        Intent intent = new Intent(context, QiscusVideoStreamActivity.class);
        intent.putExtra("STREAM_PARAMETER", parameter);
        streamUrl = url;
        streamParameter = parameter;
        return intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestPermission(permissions);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_qiscus_video_stream);

        rootView = (ViewGroup) findViewById(R.id.root_layout);
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        streamLiveStatus = (TextView) findViewById(R.id.stream_live_status);
        broadcast = (Button) findViewById(R.id.broadcast);
        timerHandler = new TimerHandler();
        ImageButton switchCamera = (ImageButton) findViewById(R.id.switchCamera);

        if (checkPermission()) {
            rtmpCamera = new RtmpCamera1(surfaceView, this);
        }

        switchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchCamera();
            }
        });

        broadcast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleBroadcasting();
            }
        });

        surfaceView.getHolder().addCallback(this);
        //ayelet
        if(absEnableTTS){
            initTTS();
        }

        String HttpSocketUrl = getHttpUrl(streamUrl);
        try{

            Log.i("Ayelet", "Socket URL: " + HttpSocketUrl);
            IO.Options opts = new IO.Options();

            mSocket = IO.socket(HttpSocketUrl);
            Log.i("Ayelet", "Socket is now set");

            mSocket.on("match", onMatch);
            Log.i("Ayelet", "Match event is now set");
            mSocket.on("clientConnected", onClientConnected);
            Log.i("Ayelet", "ClientConnected event is now set");
            mSocket.on("greenLight", onGreenLight);
            Log.i("Ayelet", "GreenLight event is now set");
            mSocket.on("redLight", onRedLight);
            Log.i("Ayelet", "redLight event is now set");
        }catch (URISyntaxException e){}

        mMediaPlayerWalk = MediaPlayer.create(this, R.raw.walksound);
        mMediaPlayerWalk.setLooping(true);
        mMediaPlayerDontWalk = MediaPlayer.create(this, R.raw.dontwalksound);
        mMediaPlayerDontWalk.setLooping(true);

        //
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 101 && permissions.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (rtmpCamera == null) {
                rtmpCamera = new RtmpCamera1(surfaceView, this);
                rtmpCamera.startPreview();
            }
        } else {
            new AlertDialog.Builder(QiscusVideoStreamActivity.this)
                    .setTitle("Qiscus Streaming SDK")
                    .setMessage("Permission error.")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
                                startActivity(intent);
                            } catch (ActivityNotFoundException e) {
                                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
                                startActivity(intent);
                            }
                        }
                    })
                    .show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (rtmpCamera != null) {
            if (rtmpCamera.isStreaming()) {
                rtmpCamera.stopStream();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (rtmpCamera != null) {
            if (rtmpCamera.isStreaming()) {
                rtmpCamera.stopStream();
                rtmpCamera.stopPreview();
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private void toggleBroadcasting() {
        if (!rtmpCamera.isStreaming()) {
            //if (rtmpCamera.isRecording() || rtmpCamera.prepareAudio() && rtmpCamera.prepareVideo(streamParameter.videoWidth,
            // streamParameter.videoHeight, streamParameter.videoFps, streamParameter.videoBitrate, false, 90)) {
            if (rtmpCamera.isRecording() || rtmpCamera.prepareAudio() && rtmpCamera.prepareVideo()) {
                startStream();
            } else {
                Snackbar.make(rootView, "Error preparing stream.", Snackbar.LENGTH_SHORT).show();
            }
        } else {
            stopStream();
        }
    }

    public void switchCamera() {
        if (rtmpCamera != null) {
            rtmpCamera.switchCamera();
        }
    }

    private void startStream() {
        streamLiveStatus.setText("Connecting");
        broadcast.setText("Stop");
        broadcast.setBackground(getResources().getDrawable(R.drawable.round_button_red));
        broadcast.setTextColor(getResources().getColor(R.color.white));
        rtmpCamera.startStream(streamUrl);
        //ayelet
        if(absEnableTTS){
            enableTTS = true;
        }
        mSocket.connect();
        //
    }

    private void stopStream() {
        stopTimer();
        streamLiveStatus.setText("Offline");
        broadcast.setText("Start");
        broadcast.setBackground(getResources().getDrawable(R.drawable.round_button_white));
        broadcast.setTextColor(getResources().getColor(R.color.black));
        rtmpCamera.stopStream();
        rtmpCamera.startPreview();
        //ayelet
        enableTTS = false;
        myTTS.shutdown();
//        reset media players

        mMediaPlayerWalk.pause();
        mMediaPlayerWalk.seekTo(0);
        mMediaPlayerDontWalk.pause();
        mMediaPlayerDontWalk.seekTo(0);

    }

    public void startTimer() {
        if (timer == null) {
            timer = new Timer();
        }

        elapsedTime = 0;
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                elapsedTime += 1; //increase every sec
                timerHandler.obtainMessage(TimerHandler.INCREASE_TIMER).sendToTarget();

            }
        }, 0, 1000);
    }

    public void stopTimer() {
        if (timer != null) {
            timer.cancel();
        }

        timer = null;
        elapsedTime = 0;
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        if (rtmpCamera != null) {
            rtmpCamera.startPreview();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        if (rtmpCamera != null) {
            rtmpCamera.startPreview();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (rtmpCamera != null) {
            if (rtmpCamera.isStreaming()) {
                stopTimer();
                streamLiveStatus.setText("Offline");
                broadcast.setText("Start");
                broadcast.setBackground(getResources().getDrawable(R.drawable.round_button_white));
                broadcast.setTextColor(getResources().getColor(R.color.black));
                rtmpCamera.stopStream();
            }

            rtmpCamera.stopPreview();
        }
    }

    @Override
    public void onConnectionSuccessRtmp() {
        startTimer();
    }

    @Override
    public void onConnectionFailedRtmp() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Snackbar.make(rootView, "Streaming failed.", Snackbar.LENGTH_SHORT).show();

                if (rtmpCamera.isStreaming()) {
                    stopStream();
                }
            }
        });
    }

    @Override
    public void onDisconnectRtmp() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (rtmpCamera.isStreaming()) {
                    stopStream();
                }
            }
        });
    }

    @Override
    public void onAuthErrorRtmp() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Snackbar.make(rootView, "Invalid credential.", Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onAuthSuccessRtmp() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //
            }
        });
    }

    private class TimerHandler extends Handler {
        static final int CONNECTION_LOST = 2;
        static final int INCREASE_TIMER = 1;

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case INCREASE_TIMER:
                    streamLiveStatus.setText("Live - " + getDurationString((int) elapsedTime));
                    break;
                case CONNECTION_LOST:
                    try {
                        new AlertDialog.Builder(QiscusVideoStreamActivity.this)
                                .setMessage("Connection to RTMP server is lost.")
                                .setPositiveButton(android.R.string.yes, null)
                                .show();
                    } catch (Exception e) {
                        //
                    }

                    break;
            }
        }
    }

    private static String getDurationString(int seconds) {
        if (seconds < 0 || seconds > 2000000) {
            seconds = 0;
        }

        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        seconds = seconds % 60;

        if (hours == 0) {
            return twoDigitString(minutes) + " : " + twoDigitString(seconds);
        } else {
            return twoDigitString(hours) + " : " + twoDigitString(minutes) + " : " + twoDigitString(seconds);
        }
    }

    private static String twoDigitString(int number) {
        if (number == 0) {
            return "00";
        }

        if (number / 10 == 0) {
            return "0" + number;
        }

        return String.valueOf(number);
    }

    private boolean checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    private void requestPermission(String[] requestedPermission) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, requestedPermission, 101);
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            //
        }
    }
    //ayelet - Server Respond
    private Emitter.Listener onMatch = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            Log.i("Ayelet", "inside onMatch");

            QiscusVideoStreamActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String message;
                    try {
                        message = data.getString("data");
                        Log.i("Ayelet", "onMatch got a new message from the server: " + message);
                        speakWords(message);
                    } catch (JSONException e) {
                        return;
                    }

                }
            });
        }
    };
    private Emitter.Listener onClientConnected = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            Log.i("Ayelet", "inside onClientConnected");
            QiscusVideoStreamActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject messageObj = (JSONObject) args[0];
                    String message;
                    try {
                        message = messageObj.getString("data");
                        Log.i("Ayelet", "onClientConnected got data from the server: " + message);
                        speakWords(message);
                    } catch (JSONException e) {
                        return;
                    }
                }
            });
        }
    };


    private Emitter.Listener onGreenLight = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            Log.i("Ayelet", "inside onGreenLight");

            if(mMediaPlayerDontWalk.isPlaying()) {
                mMediaPlayerDontWalk.pause();
                mMediaPlayerDontWalk.seekTo(0);
            }

            if(!inGreenLight) { // was in red light event or first event in streaming
                inGreenLight = true;
                if(!mMediaPlayerWalk.isPlaying())
                    mMediaPlayerWalk.start();


                if(inRedLight){ // change from red to green
                    inRedLight = false;
                    changeEvents = true;
                }
            }
            else // green after green
                changeEvents = false;

            QiscusVideoStreamActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject messageObj = (JSONObject) args[0];
                    String message;
                    try {
                        message = messageObj.getString("data");
                        Log.i("Ayelet", "onGreenLight got data from the server: " + message);
                        if(changeEvents) {
                            speakWords(message);
                        }
                    } catch (JSONException e) {
                        return;
                    }
                }
            });
        }
    };

    private Emitter.Listener onRedLight = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            Log.i("Ayelet", "inside onRedLight");
            if(mMediaPlayerWalk.isPlaying()) {
                mMediaPlayerWalk.pause();
                mMediaPlayerWalk.seekTo(0);
            }

            if(!inRedLight) { // was in red light event or first event in streaming
                inRedLight = true;

                if(!mMediaPlayerDontWalk.isPlaying())
                    mMediaPlayerDontWalk.start();


                if(inGreenLight){ // change from green to red
                    inGreenLight = false;
                    changeEvents = true;
                }
            }
            else // red after red
                changeEvents = false;

            QiscusVideoStreamActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject messageObj = (JSONObject) args[0];
                    String message;
                    try {
                        message = messageObj.getString("data");
                        Log.i("Ayelet", "onRedLight got data from the server: " + message);

                        if(changeEvents) {
                            speakWords(message);
                        }

                    } catch (JSONException e) {
                        return;
                    }
                }
            });
        }
    };

    private String getHttpUrl(String rtmpUrl)
    {
        Log.i("Ayelet", "inside getHttpUrl");
        String httpUrl = "";
        String[] parts = rtmpUrl.split(":");
        httpUrl = "http:" + parts[1] + ":" + HTTP_PORT;
        Log.i("Ayelet", "HTTP Url: " + httpUrl);
        return httpUrl;
    }
    //
    //ayelet - TTS - Text To Speech
    // Voice notification from the server, emitted on events
    private void initTTS() {
        //Init the object in the oncreate
        myTTS = new TextToSpeech(QiscusVideoStreamActivity.this, new TextToSpeech.OnInitListener() {

            @Override
            public void onInit(int status) {

                if(status == TextToSpeech.SUCCESS){
                    int result=myTTS.setLanguage(Locale.US);
                    if(result==TextToSpeech.LANG_MISSING_DATA ||
                            result==TextToSpeech.LANG_NOT_SUPPORTED){
                        Log.e("error", "This Language is not supported");
                    }
                    else{
                        //speakWords("application is speaking");
                    }
                }
                else
                    Log.e("error", "Initilization Failed!");
            }
        });
        Intent checkTTSIntent = new Intent();
        checkTTSIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkTTSIntent, MY_DATA_CHECK_CODE);
    }

    private void speakWords(String speech) {
        //speak straight away
        Log.i("Ayelet", speech + "TTS" );
        if(enableTTS) {
            myTTS.speak(speech, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    //
}
