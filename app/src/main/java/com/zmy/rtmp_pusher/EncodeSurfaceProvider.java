package com.zmy.rtmp_pusher;


import android.content.Context;
import android.graphics.ImageFormat;
import android.media.ImageReader;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceRequest;
import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;

import com.zmy.rtmp_pusher.lib.encoder.AVCEncoder;
import com.zmy.rtmp_pusher.lib.encoder.EncodeFrame;
import com.zmy.rtmp_pusher.lib.encoder.EncoderCallback;
import com.zmy.rtmp_pusher.lib.encoder.EncoderException;
import com.zmy.rtmp_pusher.lib.queue.LinkedQueue;


public class EncodeSurfaceProvider implements Preview.SurfaceProvider, Consumer<SurfaceRequest.Result>, SurfaceRequest.TransformationInfoListener {
    private final Context context;
    private AVCEncoder encoder;
    private final int bitrate;
    private final int fps;
    private final int keyFrameInternal;
    private final EncoderCallback callback;
    private final LinkedQueue<EncodeFrame> queue;

    public EncodeSurfaceProvider(Context context, int bitrate, int fps, int keyFrameInternal, EncoderCallback callback, LinkedQueue<EncodeFrame> queue) {
        this.context = context;
        this.bitrate = bitrate;
        this.fps = fps;
        this.keyFrameInternal = keyFrameInternal;
        this.callback = callback;
        this.queue = queue;
    }

    @Override
    public void onSurfaceRequested(@NonNull SurfaceRequest request) {
        request.setTransformationInfoListener(ContextCompat.getMainExecutor(context), this);
        Size resolution = request.getResolution();
        encoder = new AVCEncoder(bitrate, callback, resolution.getWidth(), resolution.getHeight(), fps, keyFrameInternal);
        encoder.setOutputQueue(queue);
        try {
            encoder.init();
            encoder.start();
            request.provideSurface(encoder.getSurface(), ContextCompat.getMainExecutor(context), this);
        } catch (EncoderException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void accept(SurfaceRequest.Result result) {

    }

    @Override
    public void onTransformationInfoUpdate(@NonNull SurfaceRequest.TransformationInfo transformationInfo) {
    }
}
