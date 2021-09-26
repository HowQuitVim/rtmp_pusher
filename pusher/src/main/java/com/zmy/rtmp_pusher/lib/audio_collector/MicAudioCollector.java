package com.zmy.rtmp_pusher.lib.audio_collector;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.nio.ByteBuffer;

public class MicAudioCollector extends AudioCollector implements Runnable {
    private Thread recordThread;
    private AudioRecord audioRecord;
    private int bufferSize;
    private ByteBuffer audioBuffer;
    private boolean exit = false;
    private final int format;
    private final int sampleRate;
    private final int channelConfig;

    public MicAudioCollector(CollectorCallback callback, int format, int sampleRate, int channelConfig) {
        super(callback);
        this.format = format;
        this.sampleRate = sampleRate;
        this.channelConfig = channelConfig;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public int getFormat() {
        return format;
    }

    @Override
    public void initialize() throws AudioCollectorException {
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
            if (callback != null) {
                int channels = getChannelCount();
                if (channels == 0) throw new IllegalStateException("illegal channel config");
            }
        } catch (Exception e) {
            throw new AudioCollectorException(e);
        }
    }

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
    public void start() throws AudioCollectorException {
        if (audioRecord == null) return;
        if (recordThread != null) {
            try {
                recordThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        recordThread = new Thread(this);
        recordThread.start();
    }

    @Override
    public void stop() throws AudioCollectorException {
        synchronized (this) {
            exit = true;
        }
        if (recordThread != null)
            try {
                recordThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
    }

    @Override
    public void run() {
        synchronized (this) {
            exit = false;
            audioRecord.startRecording();
        }
        while (true) {
            synchronized (this) {
                if (exit) break;
            }
            audioBuffer.clear();
            int len = audioRecord.read(audioBuffer, bufferSize);
            if (len >= 0 && queue != null) {
                audioBuffer.position(0);
                audioBuffer.limit(len);
                queue.enqueue(audioBuffer);
            }
        }
        synchronized (this) {
            audioRecord.stop();
        }
    }

    @Override
    public void release() throws AudioCollectorException {
        stop();
        try {
            if (audioRecord != null)
                audioRecord.release();
        } catch (Exception e) {
            throw new AudioCollectorException(e);
        }
    }
}
