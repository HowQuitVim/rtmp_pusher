package com.zmy.rtmp_pusher.lib.log;

import android.util.Log;

import androidx.annotation.Nullable;

public class RtmpLogManager {
    static RtmpLogger logger;

    public static RtmpLogger getLogger() {
        return logger;
    }

    public static void registerLogger(@Nullable RtmpLogger logger) {
        RtmpLogManager.logger = logger;
        native_register_logger(logger);
    }

    public static void d(String tag, String msg) {
        if (logger != null) logger.print(Log.DEBUG, tag, msg);
    }

    public static void w(String tag, String msg) {
        if (logger != null) logger.print(Log.WARN, tag, msg);
    }

    public static void i(String tag, String msg) {
        if (logger != null) logger.print(Log.INFO, tag, msg);
    }

    public static void e(String tag, String msg) {
        if (logger != null) logger.print(Log.ERROR, tag, msg);
    }

    public static void e(String tag, String msg, Exception exception) {
        if (logger != null) logger.print(Log.ERROR, tag, msg + "\n" + Log.getStackTraceString(exception));
    }

    private static native void native_register_logger(RtmpLogger logger);
}
