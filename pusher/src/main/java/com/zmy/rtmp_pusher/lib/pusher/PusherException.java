package com.zmy.rtmp_pusher.lib.pusher;

import com.zmy.rtmp_pusher.lib.exception.DelegateException;

public class PusherException extends DelegateException {

    public PusherException(Exception exception) {
        super(exception);
    }
}
