package com.zmy.rtmp_pusher.lib;

import com.zmy.rtmp_pusher.lib.audio_collector.AudioCapture;
import com.zmy.rtmp_pusher.lib.encoder.AACEncoder;
import com.zmy.rtmp_pusher.lib.encoder.AVCEncoder;

public class RtmpPusher {
    private AudioCapture audioCollector;
    private AACEncoder audioEncoder;


    private AVCEncoder videoEncoder;
}
