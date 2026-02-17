package com.jarvis.ai.vision

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Surface
import androidx.core.app.NotificationCompat
import com.jarvis.ai.JarvisApplication
import com.jarvis.ai.ui.main.MainActivity
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.util.*

class LiveVisionService : Service() {

    companion object {
        private const val TAG = "LiveVisionService"
        private const val NOTIFICATION_ID = 3001
        private const val MAX_FRAME_CACHE_SIZE = 5

        var instance: LiveVisionService? = null
            private set

        val isActive: Boolean get() = instance != null
        val latestFrameCache = mutableListOf<Bitmap>()
        val visionEvents = kotlinx.coroutines.flow.MutableSharedFlow<VisionEvent>(extraBufferCapacity = 10)

        fun start(context: Context) {
            val intent = Intent(context, LiveVisionService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LiveVisionService::class.java))
        }

        fun captureFrame(): Bitmap? {
            return instance?.latestFrameCache?.lastOrNull()
        }

        fun analyzeScene(): String {
            val frame = instance?.latestFrameCache?.lastOrNull() ?: return "No frame available"
            val bitmap = frame
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val base64 = Base64.getEncoder().encodeToString(outputStream.toByteArray())
            
            val analysisPrompt = """
                Analyze this image as my girlfriend AI companion.
                Describe:
                1. What am I doing?
                2. My emotional state
                3. What objects are around me
                4. How I look
                5. Is anything concerning?
                
                Respond lovingly and caringly as my girlfriend would.
                Use "বাবু" or similar terms in Bengali, or "baby" in English.
            """.trimIndent()
            
            return "[VISION_ANALYSIS_BASE64:$base64]\n$analysisPrompt"
        }
    }

    data class VisionEvent(
        val type: String,
        val timestamp: Long = System.currentTimeMillis(),
        val description: String = "",
        val confidence: Float = 0f
    )

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var backgroundHandler: Handler? = null
    private var backgroundThread: HandlerThread? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        instance = this
        startBackgroundThread()
        Log.i(TAG, "LiveVisionService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        openCamera()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        closeCamera()
        stopBackgroundThread()
        instance = null
        scope.cancel()
        Log.i(TAG, "LiveVisionService destroyed")
        super.onDestroy()
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("VisionServiceThread").apply { start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        backgroundThread = null
        backgroundHandler = null
    }

    private fun openCamera() {
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraId = manager.cameraIdList.firstOrNull() ?: return
            manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    createCameraPreviewSession(camera)
                    Log.i(TAG, "Camera opened")
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e(TAG, "Camera error: $error")
                    cameraDevice?.close()
                    cameraDevice = null
                }
            }, backgroundHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open camera", e)
        }
    }

    private fun createCameraPreviewSession(cameraDevice: CameraDevice) {
        try {
            imageReader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 2)
            val surface = imageReader!!.surface
            val captureRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(surface)
            }

            cameraDevice.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        captureRequest.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                        session.setRepeatingRequest(captureRequest.build(), null, backgroundHandler)
                        startImageCapture()
                        Log.i(TAG, "Capture session started")
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Capture session configuration failed")
                    }
                },
                backgroundHandler
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create preview session", e)
        }
    }

    private fun startImageCapture() {
        imageReader?.setOnImageAvailableListener({ reader ->
            try {
                val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener

                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)

                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { bitmap ->
                    latestFrameCache.add(bitmap)
                    if (latestFrameCache.size > MAX_FRAME_CACHE_SIZE) {
                        latestFrameCache.removeAt(0)
                    }
                    
                    scope.launch {
                        try {
                            visionEvents.emit(VisionEvent(
                                type = "FRAME_CAPTURED",
                                description = "New frame available for analysis"
                            ))
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to emit vision event", e)
                        }
                    }
                }

                image.close()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process image", e)
            }
        }, backgroundHandler)
    }

    private fun closeCamera() {
        captureSession?.close()
        captureSession = null
        imageReader?.close()
        imageReader = null
        cameraDevice?.close()
        cameraDevice = null
        Log.d(TAG, "Camera closed")
    }

    private fun createNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, LiveVisionService::class.java).apply { action = "STOP" },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, JarvisApplication.CHANNEL_OVERLAY_SERVICE)
            .setContentTitle("Live Vision Active")
            .setContentText("Seeing you, baby... 👀")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopIntent)
            .build()
    }
}