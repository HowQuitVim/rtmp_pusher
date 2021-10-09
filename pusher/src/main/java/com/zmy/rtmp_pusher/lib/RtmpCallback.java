package com.zmy.rtmp_pusher.lib;

public interface RtmpCallback {
    void onAudioCaptureError(Exception e);

    void onVideoCaptureError(Exception e);

    void onVideoEncoderError(Exception e);

    void onAudioEncoderError(Exception e);

    void onPusherError(Exception e);

}
