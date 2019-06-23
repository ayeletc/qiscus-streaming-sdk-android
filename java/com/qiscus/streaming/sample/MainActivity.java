package com.qiscus.streaming.sample;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.qiscus.sdk.Qiscus;
import com.qiscus.streaming.QiscusStreaming;
import com.qiscus.streaming.data.QiscusStream;
import com.qiscus.streaming.data.VideoQuality;
import com.qiscus.streaming.util.CreateStreamListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
// ayelet - remove?
        Qiscus.setUser("ganjar2@ganjar.com", "12345678")
                .withUsername("Ganjar2")
                .save()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(qiscusAccount -> {
                    Log.i("MainActivity", "Login with account: " + qiscusAccount);
                }, throwable -> {
                    throwable.printStackTrace();
                    showError(throwable.getMessage());
                });

        startStream();
//
        overridePendingTransition(R.transition.slide_in, R.transition.slide_out); // ayelet - move?

        Button one = (Button) findViewById(R.id.basic);
//        one.setVisibility(View.INVISIBLE);

//      ayelet - START
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
//                one.setVisibility(View.VISIBLE);
                one.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, BasicStreamActivity.class)));
                startActivity(new Intent(MainActivity.this, BasicStreamActivity.class));

            }
        }, 3000);
/*

        Button two = (Button) findViewById(R.id.integration);
        two.setOnClickListener(v -> openSimpleCustomChat(v));

        END
  */

    }

    public void openSimpleCustomChat(View view) {
        showLoading();
        Qiscus.buildChatRoomWith("guest2@gg.com")
                .withTitle("Guest 2")
                .build()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(qiscusChatRoom -> SimpleCustomChatActivity.generateIntent(this, qiscusChatRoom))
                .subscribe(intent -> {
                    startActivity(intent);
                    dismissLoading();
                }, throwable -> {
                    throwable.printStackTrace();
                    showError(throwable.getMessage());
                    dismissLoading();
                });
    }

    public void startStream() {
        Intent intent = getIntent();
        String action = intent.getAction();
        Uri data = intent.getData();

        if (data != null) {
            Log.d(TAG, "startStream: " + data.toString());
            String statusRequest = data.getLastPathSegment();
            String streamUrl = data.getQueryParameter("streamUrl");
            if (statusRequest.toLowerCase().equals("request")) {

                JSONObject tags = new JSONObject();
                try {
                    tags.put("chatUser", Qiscus.getQiscusAccount().getEmail());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                QiscusStreaming.createStream("Stream " + (System.currentTimeMillis() / 1000L), tags, new CreateStreamListener() {
                    @Override
                    public void onCreateStreamSuccess(final QiscusStream stream) {
                        runOnUiThread(() -> QiscusStreaming.buildStream(stream.streamUrl)
                                .setVideoQuality(VideoQuality.QVGA)
                                .start(getApplicationContext()));
                    }

                    @Override
                    public void onCreateStreamError(final String error) {
                        Log.e(TAG, "Create stream error: " + error);
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Create stream error: " + error, Toast.LENGTH_SHORT).show());
                    }
                });
            } else {
                if (streamUrl != null) {
                    QiscusStreaming.buildStream(streamUrl)
                            .setVideoQuality(VideoQuality.QVGA)
                            .start(this);
                } else {
                    Toast.makeText(MainActivity.this, "invalid url stream", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public void showError(String errorMessage) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    }

    public void showLoading() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage("Please wait...");
        }
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    public void dismissLoading() {
        mProgressDialog.dismiss();
    }
}
