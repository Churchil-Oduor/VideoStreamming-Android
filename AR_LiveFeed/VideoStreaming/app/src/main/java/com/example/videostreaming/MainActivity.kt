package com.example.videostreaming

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.videostreaming.databinding.ActivityMainBinding
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var wsManager: WebSocketManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)


        cameraExecutor = Executors.newSingleThreadExecutor()
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        wsManager = WebSocketManager{
            frameBytes -> displayProcessedFrame(frameBytes)
        }

        wsManager.connect()

    }

    private fun displayProcessedFrame(bytes: ByteArray) {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        print(bytes.size)
        runOnUiThread{
            viewBinding.processedView.setImageBitmap(bitmap)
        }
    }
    companion object {
        private const val TAG = "CameraXApp"
        private val REQUIRED_PERMISSIONS = mutableListOf (
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).toTypedArray()
    }
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera(){
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()


            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(android.util.Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalyzer.setAnalyzer(cameraExecutor) {imageProxy ->
                try {
                    val jpegBytes = imageProxy.toJpegByteArray(60)
                    wsManager.sendFrame(jpegBytes)
                    Log.d(TAG, "Frame Size: ${jpegBytes.size}")
                } catch (e: Exception) {
                    Log.e(TAG, "Frame Processing Error", e)
                } finally {
                    imageProxy.close()
                }
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    imageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions())
    {
        permissions ->
        var permissionGranted = true
        permissions.entries.forEach {
            if (it.key in REQUIRED_PERMISSIONS && it.value == false)
                permissionGranted = false
        }

        if (!permissionGranted) {
            Toast.makeText(baseContext,
                "Permission request Denied",
                Toast.LENGTH_SHORT).show()
        } else {
            startCamera()
        }
    }



    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()

    }


    //image conversion
    fun ImageProxy.toJpegByteArray(quality: Int = 80): ByteArray {
        val nv21 = yuv420888ToNv21(this)
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), quality, out)
        return out.toByteArray()
    }


    private fun yuv420888ToNv21(image: ImageProxy): ByteArray {
        val width = image.width
        val height = image.height
        val ySize = width * height
        val uvSize = width * height / 4

        val nv21 = ByteArray(ySize + uvSize * 2)

        // Y plane
        val yBuffer = image.planes[0].buffer
        var rowStride = image.planes[0].rowStride
        var pos = 0
        for (row in 0 until height) {
            yBuffer.position(row * rowStride)
            yBuffer.get(nv21, pos, width)
            pos += width
        }

        // UV planes (interleaved VU for NV21)
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer
        rowStride = image.planes[1].rowStride
        val pixelStride = image.planes[1].pixelStride

        val chromaHeight = height / 2
        val chromaWidth = width / 2

        for (row in 0 until chromaHeight) {
            var uvPos = row * rowStride
            for (col in 0 until chromaWidth) {
                nv21[pos++] = vBuffer.get(uvPos)
                nv21[pos++] = uBuffer.get(uvPos)
                uvPos += pixelStride
            }
        }
        return nv21
    }
}
