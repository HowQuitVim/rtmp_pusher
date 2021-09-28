package com.zmy.rtmp_pusher.lib.exception;

public class Err {
    private native static int nativeErrno();

    private native static String nativeErrDescribe(int err);

    public static int errno() {
        return nativeErrno();
    }

    public static String errDescribe(int err) {
        return nativeErrDescribe(err);
    }
}
