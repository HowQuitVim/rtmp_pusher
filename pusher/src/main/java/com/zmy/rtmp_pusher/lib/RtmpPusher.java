package com.zmy.rtmp_pusher.lib;

import android.util.Log;

import androidx.annotation.Nullable;

import com.zmy.rtmp_pusher.lib.audio_capture.AudioCapture;
import com.zmy.rtmp_pusher.lib.audio_capture.AudioCaptureCallback;
import com.zmy.rtmp_pusher.lib.encoder.AACEncoder;
import com.zmy.rtmp_pusher.lib.encoder.AVCEncoder;
import com.zmy.rtmp_pusher.lib.encoder.EncoderCallback;
import com.zmy.rtmp_pusher.lib.encoder.EncoderException;
import com.zmy.rtmp_pusher.lib.encoder.IEncoder;
import com.zmy.rtmp_pusher.lib.encoder.RtmpPacket;
import com.zmy.rtmp_pusher.lib.log.DefaultLogger;
import com.zmy.rtmp_pusher.lib.log.RtmpLogManager;
import com.zmy.rtmp_pusher.lib.log.RtmpLogger;
import com.zmy.rtmp_pusher.lib.pusher.Pusher;
import com.zmy.rtmp_pusher.lib.pusher.PusherCallback;
import com.zmy.rtmp_pusher.lib.pusher.PusherException;
import com.zmy.rtmp_pusher.lib.queue.ByteQueue;
import com.zmy.rtmp_pusher.lib.queue.LinkedQueue;
import com.zmy.rtmp_pusher.lib.video_capture.VideoCapture;
import com.zmy.rtmp_pusher.lib.video_capture.VideoCaptureCallback;

import java.util.Locale;

public class RtmpPusher implements PusherCallback, AudioCaptureCallback, VideoCaptureCallback, EncoderCallback {
    static {
        System.loadLibrary("pusher");
    }

    public static void init() {
        init(Log.VERBOSE);
    }

    public static void init(@RtmpLogger.LogLevel int logThreshold) {
        RtmpLogManager.registerLogger(new DefaultLogger(logThreshold));
    }

    private static final String TAG = RtmpPusher.class.getSimpleName();
    private final String url;
    private final int cacheSize;
    private Pusher pusher;
    private final RtmpCallback callback;

    private final AudioCapture audioCapture;
    private final VideoCapture videoCapture;

    private AACEncoder audioEncoder;
    private AVCEncoder videoEncoder;


    private RtmpPusher(String url, int cacheSize, AudioCapture audioCapture, VideoCapture videoCapture, RtmpCallback callback) {
        this.url = url;
        this.cacheSize = cacheSize;
        this.audioCapture = audioCapture;
        this.videoCapture = videoCapture;
        this.callback = callback;
    }

    public void start() throws PusherException {
        pusher = new Pusher(url, new LinkedQueue<>(cacheSize, RtmpPacket.DELETER), this);
        pusher.start();
        audioCapture.initialize(this);
        videoCapture.initialize(this);
    }

    @Override
    public void onPushError(PusherException exception) {
        if (callback != null) callback.onPusherError(exception);
    }

    @Override
    public void onAudioCaptureInit(AudioCapture capture, @Nullable Exception exception) {
        if (exception != null) {
            RtmpLogManager.d(TAG, "fail to init AudioCapture");
            release();
            if (callback != null) callback.onAudioCaptureError(exception);
        } else {
            capture.start(new ByteQueue(1024, 4096));
            audioEncoder = new AACEncoder(64000, this, capture.getChannelCount(), capture.getSampleFormat(), capture.getSampleRate(), capture.getQueue());
            audioEncoder.setOutputQueue(pusher.getQueue());
            try {
                audioEncoder.init();
                audioEncoder.start();
            } catch (EncoderException e) {
                e.printStackTrace();
                onEncodeError(audioEncoder, e);
            }
        }
    }

    public void release() {
        RtmpLogManager.d(TAG, "release");
        if (pusher != null) pusher.release();
        if (audioCapture != null) audioCapture.release();
        if (videoCapture != null) videoCapture.release();
        if (audioEncoder != null) audioEncoder.release();
        if (videoEncoder != null) videoEncoder.release();
    }

    @Override
    public void onAudioCaptureError(Exception e) {
        Log.e(TAG, "fail to collect audio pcm", e);
        release();
        if (callback != null) callback.onAudioCaptureError(e);
    }

    @Override
    public void onVideoCaptureInit(VideoCapture capture, @Nullable Exception exception) {
        if (exception != null) {
            release();
            if (callback != null) callback.onVideoCaptureError(exception);
        } else {
            videoEncoder = new AVCEncoder(1080 * 1920 * 3, this, capture.getWidth(), capture.getHeight(), 30, 1);
            videoEncoder.setOutputQueue(pusher.getQueue());
            try {
                videoEncoder.init();
                videoEncoder.start();
                capture.start(videoEncoder.getSurface(), videoEncoder);
            } catch (EncoderException e) {
                e.printStackTrace();
                onEncodeError(videoEncoder, e);
            }
        }
    }

    @Override
    public void onVideoCaptureError(Exception e) {
        Log.e(TAG, "fail to open camera", e);
        release();
        if (callback != null) callback.onVideoCaptureError(e);
    }

    @Override
    public void onEncodeError(IEncoder encoder, Exception e) {
        release();
        if (encoder instanceof AVCEncoder) {
            Log.e(TAG, "video encode error", e);
            if (callback != null) callback.onVideoEncoderError(e);
        }
        if (encoder instanceof AACEncoder) {
            Log.e(TAG, "audio encode error", e);
            if (callback != null) callback.onAudioEncoderError(e);
        }
    }


    public static class Builder {
        private String url;
        private int cacheSize;
        private AudioCapture audioCapture;
        private VideoCapture videoCapture;
        private RtmpCallback callback;

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder cacheSize(int size) {
            this.cacheSize = size;
            return this;
        }

        public Builder audioCapture(AudioCapture audioCapture) {
            this.audioCapture = audioCapture;
            return this;
        }

        public Builder videoCapture(VideoCapture videoCapture) {
            this.videoCapture = videoCapture;
            return this;
        }

        public Builder callback(RtmpCallback callback) {
            this.callback = callback;
            return this;
        }

        public RtmpPusher build() {
            if (url == null || !url.toLowerCase(Locale.getDefault()).startsWith("rtmp://")) {
                throw new IllegalStateException("invalid url");
            }
            if (cacheSize <= 0) {
                throw new IllegalStateException("cacheSize must be more than 0");
            }
            if (audioCapture == null) {
                throw new IllegalStateException("audioCapture is null");
            }
            if (videoCapture == null) {
                throw new IllegalStateException("videoCapture is null");
            }
            return new RtmpPusher(url, cacheSize, audioCapture, videoCapture, callback);
        }

    }
}
