package com.qiscus.streaming.sample;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.qiscus.sdk.data.model.QiscusChatRoom;
import com.qiscus.sdk.ui.QiscusBaseChatActivity;
import com.qiscus.sdk.ui.fragment.QiscusBaseChatFragment;

import java.util.Date;

public class SimpleCustomChatActivity extends QiscusBaseChatActivity {
    private TextView mTitle;

    public static Intent generateIntent(Context context, QiscusChatRoom qiscusChatRoom) {
        Intent intent = new Intent(context, SimpleCustomChatActivity.class);
        intent.putExtra(CHAT_ROOM_DATA, qiscusChatRoom);
        return intent;
    }

    @Override
    protected void onSetStatusBarColor() {

    }

    @Override
    protected int getResourceLayout() {
        return R.layout.activity_simple_custom_chat;
    }

    @Override
    protected void onLoadView() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mTitle = (TextView) findViewById(R.id.tv_title);
    }

    @Override
    protected QiscusBaseChatFragment onCreateChatFragment() {
        return SimpleCustomChatFragment.newInstance(qiscusChatRoom);
    }

    @Override
    protected void onViewReady(Bundle savedInstanceState) {
        super.onViewReady(savedInstanceState);
        mTitle.setText(qiscusChatRoom.getName());
    }

    @Override
    public void onUserStatusChanged(String user, boolean online, Date lastActive) {

    }

    @Override
    public void onUserTyping(String user, boolean typing) {

    }
}
