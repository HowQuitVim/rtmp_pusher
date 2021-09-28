package com.zmy.rtmp_pusher.lib.audio_collector;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import androidx.annotation.NonNull;

import com.zmy.rtmp_pusher.lib.queue.ByteQueue;
import com.zmy.rtmp_pusher.lib.util.WorkerThread;

import java.nio.ByteBuffer;

public class MicAudioCapture extends AudioCapture {
    private AudioRecordThread recordThread;
    private AudioRecord audioRecord;
    private int bufferSize;
    private ByteBuffer audioBuffer;
    private final int format;
    private final int sampleRate;
    private final int channelConfig;

    public MicAudioCapture(AudioCaptureCallback callback, int format, int sampleRate, int channelConfig) {
        super(callback);
        this.format = format;
        this.sampleRate = sampleRate;
        this.channelConfig = channelConfig;
    }

    @Override
    public int getSampleRate() {
        return sampleRate;
    }

    @Override
    public int getSampleFormat() {
        return format;
    }

    @Override
    public void initialize() {
        try {
            if (audioRecord != null) throw new IllegalStateException("already exists a audio recorder");
            bufferSize = AudioRecord.getMinBufferSize(
                    sampleRate,
                    channelConfig,
                    format
            ); //audioRecord能接受的最小的buffer大小
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    sampleRate,
                    channelConfig,
                    format,
                    bufferSize
            );
            if (audioBuffer == null || audioBuffer.capacity() != bufferSize) {
                audioBuffer = ByteBuffer.allocateDirect(bufferSize);
            }
            int channels = getChannelCount();
            if (channels == 0) throw new IllegalStateException("illegal channel config");
            audioRecord.startRecording();
            if (callback != null) {
                setReady(true);
                callback.onAudioCaptureInit(this, null);
            }
        } catch (Exception e) {
            if (callback != null) {
                callback.onAudioCaptureInit(this, e);
            }
        }
    }

    @Override
    public int getChannelCount() {
        int channels = 0;
        switch (channelConfig) {
            case AudioFormat.CHANNEL_IN_DEFAULT:
            case AudioFormat.CHANNEL_IN_MONO:
            case AudioFormat.CHANNEL_CONFIGURATION_MONO:
                channels = 1;
                break;
            case AudioFormat.CHANNEL_IN_STEREO:
            case AudioFormat.CHANNEL_CONFIGURATION_STEREO:
            case AudioFormat.CHANNEL_IN_FRONT | AudioFormat.CHANNEL_IN_BACK:
                channels = 2;
                break;
        }
        return channels;
    }

    @Override
    public void start(@NonNull ByteQueue queue) {
        super.start(queue);
        if (audioRecord == null) return;
        if (recordThread != null) {
            recordThread.exit();
        }
        recordThread = new AudioRecordThread();
        recordThread.start();
    }


    @Override
    public void release() {
        setReady(false);
        if (queue != null) {
            queue.close();
        }
        if (recordThread != null) {
            recordThread.exit();
        }
        try {
            if (audioRecord != null) {
                audioRecord.release();
                audioRecord = null;
            }
        } catch (Exception e) {
            if (callback != null) {
                callback.onAudioCaptureError(e);
            }
        }
    }


    class AudioRecordThread extends WorkerThread {
        public AudioRecordThread() {
            super("AudioCaptureThread");
        }

        @Override
        protected boolean doMain() {
            audioBuffer.clear();
            try {
                int len = audioRecord.read(audioBuffer, bufferSize);
                if (len >= 0 && queue != null) {
                    audioBuffer.position(0);
                    audioBuffer.limit(len);
                    queue.enqueue(audioBuffer);
                } else {
                    callback.onAudioCaptureError(new RuntimeException("fail to record audio,code=" + len));
                }
            } catch (Exception e) {
                callback.onAudioCaptureError(e);
            }
            return false;
        }

        @Override
        protected void doOnExit() {
            super.doOnExit();
            synchronized (this) {
                audioRecord.stop();
            }
        }
    }
}
