package com.zmy.rtmp_pusher

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.zmy.rtmp_pusher.capture.camerax_capture.CameraXCapture
import com.zmy.rtmp_pusher.lib.RtmpCallback
import com.zmy.rtmp_pusher.lib.RtmpPusher
import com.zmy.rtmp_pusher.lib.audio_capture.MicAudioCapture
import com.zmy.rtmp_pusher.lib.log.RtmpLogManager
import com.zmy.rtmp_pusher.lib.video_capture.VideoCapture

@RequiresApi(Build.VERSION_CODES.M)
class MainActivity : AppCompatActivity(), RtmpCallback {
    companion object {
        private const val TAG = "MainActivity"
    }

    private val previewView by lazy { findViewById<PreviewView>(R.id.preview_view) }
    private val audioCapture: MicAudioCapture by lazy {
        MicAudioCapture(AudioFormat.ENCODING_PCM_16BIT, 44100, AudioFormat.CHANNEL_IN_STEREO)
    }

    private val videoCapture: VideoCapture by lazy {
        CameraXCapture(applicationContext, this, 1920, 1080, CameraSelector.DEFAULT_FRONT_CAMERA, Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) })
    }
    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) {
            recordAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            Toast.makeText(applicationContext, "请同意权限", Toast.LENGTH_SHORT).show()
        }
    }
    private val recordAudioPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it)
            rtmpPusher.start()
        else
            Toast.makeText(applicationContext, "请同意权限", Toast.LENGTH_SHORT).show()

    }

    private val rtmpPusher by lazy {
        RtmpPusher.Builder()
            .url("rtmp://192.168.50.125:19350/live/livestream")
            .audioCapture(audioCapture)
            .videoCapture(videoCapture)
            .cacheSize(100)
            .callback(this)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        cameraPermission.launch(Manifest.permission.CAMERA)
    }

    override fun onDestroy() {
        super.onDestroy()
        rtmpPusher.release()
    }

    //无法录音
    override fun onAudioCaptureError(e: Exception?) {
        RtmpLogManager.e(TAG, "fail to collect audio pcm", e)
    }

    //无法调用摄像头
    override fun onVideoCaptureError(e: Exception?) {
        RtmpLogManager.e(TAG, "onVideoCaptureError", e)
    }

    //无法编码视频
    override fun onVideoEncoderError(e: Exception?) {
        RtmpLogManager.e(TAG, "onVideoEncoderError", e)
    }

    //无法编码音频
    override fun onAudioEncoderError(e: Exception?) {
        RtmpLogManager.e(TAG, "onAudioEncoderError", e)
    }

    //无法推流
    override fun onPusherError(e: Exception?) {
        RtmpLogManager.e(TAG, "onPusherError", e)
    }

}