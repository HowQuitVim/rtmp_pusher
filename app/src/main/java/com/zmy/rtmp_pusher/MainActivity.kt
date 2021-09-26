package com.zmy.rtmp_pusher

import android.media.AudioFormat
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.zmy.rtmp_pusher.lib.audio_collector.CollectorCallback
import com.zmy.rtmp_pusher.lib.audio_collector.MicAudioCollector
import com.zmy.rtmp_pusher.lib.encoder.*
import com.zmy.rtmp_pusher.lib.exception.Err
import com.zmy.rtmp_pusher.lib.pusher.Pusher
import com.zmy.rtmp_pusher.lib.pusher.PusherCallback
import com.zmy.rtmp_pusher.lib.queue.ByteQueue
import com.zmy.rtmp_pusher.lib.queue.LinkedQueue
import java.lang.Exception

@RequiresApi(Build.VERSION_CODES.M)
class MainActivity : AppCompatActivity(), CollectorCallback, EncoderCallback, PusherCallback {
    private val previewView by lazy { findViewById<PreviewView>(R.id.preview_view) }
    private lateinit var collector: MicAudioCollector
    private val audioQueue = ByteQueue(2, 4096)
    private lateinit var audioEncoder: AACEncoder
    private lateinit var videoEncoder: EncodeSurfaceProvider

    private lateinit var pusher: Pusher
    private val pushQueue = LinkedQueue<EncodeFrame>(100)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        pusher = Pusher("rtmp://192.168.50.125:19350/live/livestream", pushQueue, this)
        pusher.initize()
        pusher.start()
        collector = MicAudioCollector(this, AudioFormat.ENCODING_PCM_16BIT, 44100, AudioFormat.CHANNEL_IN_STEREO)
        collector.setQueue(audioQueue)
        collector.initialize()
        collector.start()
        audioEncoder =
            AACEncoder(64000, this, collector.channelCount, collector.format, collector.sampleRate, audioQueue)
        audioEncoder.setOutputQueue(pushQueue)
        audioEncoder.init()
        audioEncoder.start()
        bindPreview(ProcessCameraProvider.getInstance(applicationContext).get())
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        cameraProvider.unbindAll()
        val encodeCase = buildEncodeCase()
        val previewCase = buildPreviewCase()
        val cameraSelector: CameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()
        cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, previewCase, encodeCase)
    }

    private fun buildPreviewCase(): UseCase {
        val preview: Preview = Preview.Builder()
            .build()
        preview.setSurfaceProvider(previewView.surfaceProvider)
        return preview;
    }

    private fun buildEncodeCase(): UseCase {
        videoEncoder = EncodeSurfaceProvider(applicationContext, 1920 * 1080 * 3, 30, 1, this, pushQueue)
        val preview: Preview = Preview.Builder()
            .build()
        preview.setSurfaceProvider(videoEncoder)
        return preview
    }

    override fun onAudioCollectorError(e: Exception?) {
        Log.e("zmy", "fail to collect audio pcm", e)
    }

    override fun onPushError(errno: Int) {
        Log.e("zmy", "fail to push a/v frame to server,errno=$errno,desc=${Err.errDescribe(errno)}")
        while (true) {
            try {
                pusher.initize()
                break
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Thread.sleep(100)
        }
    }

    override fun onEncodeError(encoder: IEncoder, e: Exception?) {
        if (encoder is AVCEncoder) {
            Log.e("zmy", "video encode error", e)
        }
        if (encoder is AACEncoder) {
            Log.e("zmy", "audio encode error", e)
        }
    }

}