# rtmp_pushr
一个基于librtmp的Android rtmp推流端library
# 使用
```kotlin
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
```
# 支持的编码
## 音频
  AAC编码
## 视频
  AVC编码
# 捕获器
## 音频
### MicAudioCapture
基于AudioRecord实现的麦克风音频捕获器
## 视频
### CameraXCapture
基于CameraX的摄像头捕获器
### 自定义捕获器
* 继承AudioCapture实现自定义音频捕获器
* 继承VideoCapture实现自定义视频捕获器
