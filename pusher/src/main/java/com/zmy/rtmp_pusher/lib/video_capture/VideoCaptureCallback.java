package com.zmy.rtmp_pusher.lib.video_capture;

import androidx.annotation.Nullable;

public interface VideoCaptureCallback {
    void onVideoCaptureInit(VideoCapture capture,@Nullable Exception exception);

    void onVideoCaptureError(Exception e);
}
