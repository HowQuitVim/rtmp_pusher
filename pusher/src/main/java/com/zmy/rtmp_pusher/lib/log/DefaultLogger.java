package com.zmy.rtmp_pusher.lib.log;

import android.util.Log;

public class DefaultLogger extends RtmpLogger {
    public DefaultLogger(@LogLevel int printThreshold) {
        super(printThreshold);
    }

    @Override
    public void print(@LogLevel int level, String tag, String msg) {
        Log.println(level, tag, msg);
    }
}
