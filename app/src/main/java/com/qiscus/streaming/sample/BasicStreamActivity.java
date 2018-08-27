package com.qiscus.streaming.sample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.qiscus.streaming.QiscusStreaming;
import com.qiscus.streaming.data.QiscusStream;
import com.qiscus.streaming.data.VideoQuality;
import com.qiscus.streaming.util.CreateStreamListener;

import org.json.JSONObject;

public class BasicStreamActivity extends AppCompatActivity {
    private static final String TAG = BasicStreamActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_stream);

        final EditText txtRtmpUrl = (EditText) findViewById(R.id.txt_rtmp_url);
        Button btnCreate = (Button) findViewById(R.id.b_create_stream);
        btnCreate.setOnClickListener(v -> {
            JSONObject tags = new JSONObject();
            QiscusStreaming.createStream("Stream " + (System.currentTimeMillis() / 1000L), tags, new CreateStreamListener() {
                @Override
                public void onCreateStreamSuccess(final QiscusStream stream) {
                    runOnUiThread(() -> txtRtmpUrl.setText(stream.streamUrl));
                }

                @Override
                public void onCreateStreamError(final String error) {
                    Log.e(TAG, "Create stream error: " + error);
                    runOnUiThread(() -> Toast.makeText(BasicStreamActivity.this, "Create stream error: " + error, Toast.LENGTH_SHORT).show());
                }
            });
        });
        Button btnStart = (Button) findViewById(R.id.b_start_stop);
        btnStart.setOnClickListener(v -> {
            String streamUrl = txtRtmpUrl.getText().toString();

            if (!streamUrl.isEmpty()) {
                QiscusStreaming.buildStream(streamUrl)
                        .setVideoQuality(VideoQuality.VGA)
                        .start(BasicStreamActivity.this);
            } else {
                Toast.makeText(BasicStreamActivity.this, "Publish stream URL can not be empty", Toast.LENGTH_SHORT);
            }
        });
    }
}
