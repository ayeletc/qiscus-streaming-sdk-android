package com.qiscus.streaming.ui.activity;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
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

import java.util.Timer;
import java.util.TimerTask;

public class QiscusVideoStreamActivity extends AppCompatActivity implements ConnectCheckerRtmp, SurfaceHolder.Callback {
    private static final String TAG = QiscusVideoStreamActivity.class.getSimpleName();

    private String[] permissions = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
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

        if (rtmpCamera != null && newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            rtmpCamera.setPreviewOrientation(90);
        } else if (rtmpCamera != null && newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            rtmpCamera.setPreviewOrientation(0);
        }
    }

    private void toggleBroadcasting() {
        if (!rtmpCamera.isStreaming()) {
            if (rtmpCamera.isRecording() || rtmpCamera.prepareAudio() && rtmpCamera.prepareVideo(streamParameter.videoWidth, streamParameter.videoHeight, streamParameter.videoFps, streamParameter.videoBitrate, false, 90)) {
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
    }

    private void stopStream() {
        streamLiveStatus.setText("Offline");
        broadcast.setText("Start");
        broadcast.setBackground(getResources().getDrawable(R.drawable.round_button_white));
        broadcast.setTextColor(getResources().getColor(R.color.black));
        rtmpCamera.stopStream();
        stopTimer();
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

                if (rtmpCamera == null || !rtmpCamera.isStreaming()) {
                    timerHandler.obtainMessage(TimerHandler.CONNECTION_LOST).sendToTarget();
                }
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
                streamLiveStatus.setText("Offline");
                broadcast.setText("Start");
                broadcast.setBackground(getResources().getDrawable(R.drawable.round_button_white));
                broadcast.setTextColor(getResources().getColor(R.color.black));
                rtmpCamera.stopStream();
                stopTimer();
            }

            rtmpCamera.stopPreview();
        }
    }

    @Override
    public void onConnectionSuccessRtmp() {
        startTimer();
    }

    @Override
    public void onConnectionFailedRtmp(final String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Snackbar.make(rootView, "Streaming failed. Error: " + s, Snackbar.LENGTH_SHORT).show();
                stopStream();
            }
        });
    }

    @Override
    public void onDisconnectRtmp() {
        stopStream();
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
}
