package com.qiscus.streaming.sample;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.qiscus.sdk.Qiscus;
import com.qiscus.sdk.data.model.QiscusChatRoom;
import com.qiscus.sdk.data.model.QiscusComment;
import com.qiscus.sdk.ui.fragment.QiscusChatFragment;
import com.qiscus.streaming.QiscusStreaming;
import com.qiscus.streaming.data.QiscusStream;
import com.qiscus.streaming.data.VideoQuality;
import com.qiscus.streaming.util.CreateStreamListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static com.qiscus.sdk.data.model.QiscusComment.generateMessage;


public class SimpleCustomChatFragment extends QiscusChatFragment {
    private static final String TAG = SimpleCustomChatFragment.class.getSimpleName();
    private ImageView imageView;

    public static SimpleCustomChatFragment newInstance(QiscusChatRoom qiscusChatRoom) {
        SimpleCustomChatFragment fragment = new SimpleCustomChatFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(CHAT_ROOM_DATA, qiscusChatRoom);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    protected int getResourceLayout() {
        return R.layout.fragment_simple_custom_chat;
    }

    @Override
    protected void onLoadView(View view) {
        super.onLoadView(view);
        imageView = (ImageView) getActivity().findViewById(R.id.video_stream);
        imageView.setOnClickListener(v -> startStream());

    }

    public void startStream() {
        JSONObject tags = new JSONObject();
        try {
            tags.put("chatUser", Qiscus.getQiscusAccount().getEmail());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        QiscusStreaming.createStream("Stream " + (System.currentTimeMillis() / 1000L), tags, new CreateStreamListener() {
            @Override
            public void onCreateStreamSuccess(QiscusStream stream) {
                String message = Qiscus.getQiscusAccount().getUsername()+" Live Streaming Now!!!";
                String deeplinkUri = "qiscus://com.android.streamer/watch?watchUrl="+stream.getWatchUrl();
                QiscusStreaming.buildStream(stream.streamUrl)
                        .setVideoQuality(VideoQuality.VGA)
                        .start(getContext());
                sendStreamVideo(message, deeplinkUri);
            }

            @Override
            public void onCreateStreamError(String error) {
                getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "Create stream error: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }


    private void sendStreamVideo(String message, String deeplinkUrl) {
        JSONObject payload = new JSONObject();
        JSONObject buttonAction = new JSONObject();
        JSONObject payloadButton = new JSONObject();
        JSONArray buttons = new JSONArray();

        try {
            payloadButton.put("url", deeplinkUrl);
            buttonAction.put("label", "Watch It")
                    .put("type", "link")
                    .put("payload", payloadButton);

            buttons.put(buttonAction);

            payload.put("text", message)
                    .put("buttons", buttons);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.d("test", "sendStreamVideo: " + payload);
        QiscusComment comment = generateButtonMessage(message, "buttons", payload,
                qiscusChatRoom.getId());
        sendQiscusComment(comment);
    }

    public static QiscusComment generateButtonMessage(String text, String type, JSONObject content, long roomId) {
        QiscusComment qiscusComment = generateMessage(roomId, text);
        qiscusComment.setRawType("buttons");

        qiscusComment.setExtraPayload(content.toString());
        return qiscusComment;
    }

    @Override
    protected void onCreateChatComponents(Bundle savedInstanceState) {
        super.onCreateChatComponents(savedInstanceState);
    }

    @Override
    public void sendMessage(String message) {
        if (chatAdapter.isEmpty()) {
            Toast.makeText(getActivity(), "First message sent!", Toast.LENGTH_SHORT).show();
        }
        super.sendMessage(message);
    }
}
