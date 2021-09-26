package com.zmy.rtmp_pusher.lib.exception;

public class Err {
    public native static int errno();

    public native static String errDescribe(int err);
}
