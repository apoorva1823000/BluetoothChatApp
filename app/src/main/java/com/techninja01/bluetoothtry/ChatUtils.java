package com.techninja01.bluetoothtry;

import android.content.Context;

import android.os.Handler;

public class ChatUtils {
    private Context context;
    private Handler handler;
    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    private int state;
    public ChatUtils(Context context, Handler handler){
        this.context = context;
        this.handler = handler;
        state = STATE_NONE;
    }

    public int getState() {
        return state;
    }

    public synchronized void setState(int state) {
        this.state = state;
        handler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGED,state,-1).sendToTarget();
    }
}
