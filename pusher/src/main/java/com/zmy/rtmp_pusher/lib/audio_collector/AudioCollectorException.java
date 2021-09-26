package com.zmy.rtmp_pusher.lib.audio_collector;

import com.zmy.rtmp_pusher.lib.exception.DelegateException;

public class AudioCollectorException extends DelegateException {

    public AudioCollectorException(Exception exception) {
        super(exception);
    }
}
