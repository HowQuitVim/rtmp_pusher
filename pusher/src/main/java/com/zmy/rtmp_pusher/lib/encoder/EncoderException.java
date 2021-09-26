package com.zmy.rtmp_pusher.lib.encoder;

import com.zmy.rtmp_pusher.lib.exception.DelegateException;

public class EncoderException extends DelegateException {

    public EncoderException(Exception exception) {
        super(exception);
    }
}

