package com.zmy.rtmp_pusher.lib.log;

import android.util.Log;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public abstract class RtmpLogger {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            Log.VERBOSE,
            Log.DEBUG,
            Log.INFO,
            Log.WARN,
            Log.ERROR
    })
    public @interface LogLevel {
    }

    @LogLevel
    private int threshold;

    public RtmpLogger(@LogLevel int threshold) {
        this.threshold = threshold;
    }

    public abstract void print(@LogLevel int level, String tag, String msg);
}
