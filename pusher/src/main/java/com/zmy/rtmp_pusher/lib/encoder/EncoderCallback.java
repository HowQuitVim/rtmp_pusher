package com.zmy.rtmp_pusher.lib.encoder;

public interface EncoderCallback {
    void onEncodeError(IEncoder encoder,Exception e);
}
