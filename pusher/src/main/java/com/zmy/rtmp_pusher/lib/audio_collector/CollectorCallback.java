package com.zmy.rtmp_pusher.lib.audio_collector;

public interface CollectorCallback {
//    void onAudioCollectorInit( int format, int sampleRate, int channels);
    void onAudioCollectorError(Exception e );
}