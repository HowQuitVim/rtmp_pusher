package com.zmy.rtmp_pusher.lib.encoder;

public  interface EOFHandle {
    void signalEndOfInputStream();
}