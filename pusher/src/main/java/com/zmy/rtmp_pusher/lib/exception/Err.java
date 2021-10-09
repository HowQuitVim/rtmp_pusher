package com.zmy.rtmp_pusher.lib.exception;

public class Err {
    private native static int native_errno();

    private native static String native_err_describe(int err);

    public static int errno() {
        return native_errno();
    }

    public static String errDescribe(int err) {
        return native_err_describe(err);
    }
}
