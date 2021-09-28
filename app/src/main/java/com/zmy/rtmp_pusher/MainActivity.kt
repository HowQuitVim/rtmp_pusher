package com.zmy.rtmp_pusher

import android.media.AudioFormat
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import com.zmy.rtmp_pusher.lib.audio_collector.AudioCapture
import com.zmy.rtmp_pusher.lib.audio_collector.AudioCaptureCallback
import com.zmy.rtmp_pusher.lib.audio_collector.MicAudioCapture
import com.zmy.rtmp_pusher.lib.encoder.*
import com.zmy.rtmp_pusher.lib.exception.Err
import com.zmy.rtmp_pusher.lib.pusher.Pusher
import com.zmy.rtmp_pusher.lib.pusher.PusherCallback
import com.zmy.rtmp_pusher.lib.queue.ByteQueue
import com.zmy.rtmp_pusher.lib.queue.LinkedQueue
import com.zmy.rtmp_pusher.lib.video_collector.VideoCapture
import com.zmy.rtmp_pusher.lib.video_collector.VideoCaptureCallback
import java.lang.Exception

@RequiresApi(Build.VERSION_CODES.M)
class MainActivity : AppCompatActivity(), AudioCaptureCallback, EncoderCallback, PusherCallback, VideoCaptureCallback {
    private val previewView by lazy { findViewById<PreviewView>(R.id.preview_view) }
    private var audioCapture: MicAudioCapture? = null
    private var audioEncoder: AACEncoder? = null

    private var videoCapture: VideoCapture? = null
    private var videoEncoder: AVCEncoder? = null


    private var pusher: Pusher? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        pusher = Pusher("rtmp://192.168.50.125:19350/live/livestream", LinkedQueue(100, RtmpPacket.DELETER), this)

        audioCapture = MicAudioCapture(
            this, AudioFormat.ENCODING_PCM_16BIT, 44100, AudioFormat.CHANNEL_IN_STEREO
        )
        audioCapture?.initialize()


        videoCapture = CameraXCapture(
            applicationContext, this, 1920, 1080, CameraSelector.DEFAULT_FRONT_CAMERA, Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }, this
        )
        videoCapture?.initialize()

    }

    override fun onDestroy() {
        super.onDestroy()
        release()
    }

    private fun release() {
        pusher?.release()
        audioCapture?.release()
        videoCapture?.release()
        audioEncoder?.release()
        videoEncoder?.release()
    }

    private fun onModuleReady() {
        if (audioCapture?.isReady == true && audioEncoder?.isReady == true && videoCapture?.isReady == true && videoEncoder?.isReady == true) {
            try {
                pusher?.initialize()
                pusher?.start()
            } catch (e: Exception) {
                e.printStackTrace()
                Log.d("rtmp", "fail to init pusher")
            }
        }
    }

    override fun onAudioCaptureInit(capture: AudioCapture, e: Exception?) {
        if (e == null) {
            capture.start(ByteQueue(2, 4096))
            audioEncoder =
                AACEncoder(64000, this, capture.channelCount, capture.sampleFormat, capture.sampleRate, capture.queue)
            audioEncoder?.setOutputQueue(pusher?.queue)
            audioEncoder?.init()
            audioEncoder?.start()
            onModuleReady()
        } else {
            Log.d("rtmp", "fail to init AudioCapture")
            release()
        }
    }

    override fun onAudioCaptureError(e: Exception?) {
        Log.e("rtmp", "fail to collect audio pcm", e)
        release()
    }

    override fun onPushError(errno: Int) {
        Log.e("rtmp", "fail to push a/v frame to server,errno=$errno,desc=${Err.errDescribe(errno)}")
        while (pusher?.isReady == true) {
            try {
                pusher?.initialize()
                break
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        Thread.sleep(100)
    }

    override fun onEncodeError(encoder: IEncoder, e: Exception?) {
        if (encoder is AVCEncoder) {
            Log.e("rtmp", "video encode error", e)
        }
        if (encoder is AACEncoder) {
            Log.e("rtmp", "audio encode error", e)
        }
        release()
    }

    override fun onVideoCaptureInit(capture: VideoCapture, exception: Exception?) {
        if (exception == null) {
            videoEncoder =
                AVCEncoder(1080 * 1920 * 3, this, capture.width, capture.height, 30, 5)
            videoEncoder?.setOutputQueue(pusher?.queue)
            videoEncoder?.init()
            videoEncoder?.start()
            videoEncoder?.let {
                capture.start(it.surface, it)
            }
            onModuleReady()
        } else {
            release()
        }
    }

    override fun onVideoCaptureError(e: Exception?) {
        Log.e("rtmp", "fail to open camera", e)
        release()
    }
}