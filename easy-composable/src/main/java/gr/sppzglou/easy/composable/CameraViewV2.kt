package gr.sppzglou.easy.composable

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.lang.System.currentTimeMillis
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@SuppressLint("ClickableViewAccessibility", "RestrictedApi", "SetJavaScriptEnabled")
@Composable
fun CameraView(
    backBtn: @Composable () -> Unit,
    flashBtn: @Composable (Boolean, suspend () -> Unit) -> Unit,
    captureBtn: @Composable (suspend () -> Unit) -> Unit,
    acceptBtn: @Composable (suspend () -> Unit) -> Unit,
    dismissBtn: @Composable (suspend () -> Unit) -> Unit,
    close: suspend () -> Unit,
    progressView: @Composable () -> Unit,
    padding: Dp = 0.dp,
    corners: Dp = 0.dp,
    color1: Color = Color.Black,
    color2: Color = Color.White,
    specialBtn: (@Composable ((File?, Exception?) -> Unit) -> Unit)? = null,
    perfix: String = "",
    allowPhoto: Boolean = true,
    allowVideo: Boolean = false,
    handler: (f: File?, e: Exception?) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val lensFacing by rem(CameraSelector.LENS_FACING_BACK)
    val context = context()
    val act = context as? FragmentActivity
    val lifecycle = lifecycle()

    val cameraView = remember { Preview.Builder().build() }
    var camera by rem<Camera?>(null)
    var showPreview by rem(false)
    var showPreviewCase by rem(0)
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val videoCapture = remember {
        VideoCapture.withOutput(
            Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.LOWEST))
                .build()
        )
    }
    val currentRecording = rem<Recording?>(null)
    var isRecording by rem(false)
    var videoProgress by rem(0f)
    val videoProgressAnim by animateFloatAsState(videoProgress, tween(100, easing = LinearEasing))
    var isVideoFlow by rem(!allowPhoto)
    val cameraSelector = remember {
        CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()
    }
    var file by rem<File?>(null)

    LaunchedEffect(isRecording) {
        val start = currentTimeMillis()
        while (isRecording) {
            videoProgress = (currentTimeMillis() - start) / 15000f
            delay(100)
        }
        videoProgress = 0f
    }

    fun handleCapture(f: File?, e: Exception?) {
        if (f != null) {
            showPreviewCase = 0
            file = f
            showPreview = true
        } else {
            file = null
            showPreview = false
            act?.runOnUiThread {
                handler(null, e)
            }
        }
    }

    Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            if (!showPreview) {
                LaunchedEffect(lensFacing) {
                    val cameraProvider = context.getCameraProvider(::handleCapture)
                    if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycle,
                            cameraSelector,
                            cameraView,
                            imageCapture,
                            videoCapture
                        )
                        cameraView.surfaceProvider = previewView.surfaceProvider
                    }
                }
                Row(
                    Modifier
                        .padding(10.dp)
                        .fillMaxWidth(), Arrangement.SpaceBetween
                ) {
                    backBtn()
                    var torch by rem(false)

                    flashBtn(torch) {
                        if (camera?.cameraInfo?.hasFlashUnit() == true) {
                            torch = !torch
                            camera?.cameraControl?.enableTorch(torch)
                        }
                    }
                }
                Box {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = padding)
                            .aspectRatio(3 / 4f)
                            .clip(RoundedCornerShape(corners))
                    ) {
                        var x by rem(0)
                        var y by rem(0)
                        var focusClick by rem(false)
                        var zoom by rem(0f)
                        var currentZoom by rem(0f)
                        LaunchedEffect(currentZoom) {
                            zoom = currentZoom
                        }
                        AndroidView(
                            {
                                val listener =
                                    object :
                                        ScaleGestureDetector.SimpleOnScaleGestureListener() {
                                        override fun onScale(detector: ScaleGestureDetector): Boolean {
                                            val currentZoomRatio =
                                                camera?.cameraInfo?.zoomState?.value?.zoomRatio
                                                    ?: 0F
                                            val delta = detector.scaleFactor
                                            camera?.cameraControl?.setZoomRatio(currentZoomRatio * delta)
                                            currentZoom =
                                                camera?.cameraInfo?.zoomState?.value?.linearZoom
                                                    ?: 0f
                                            return true
                                        }
                                    }
                                val scaleGestureDetector =
                                    ScaleGestureDetector(context, listener)
                                previewView.apply {
                                    setOnTouchListener { _: View, motionEvent: MotionEvent ->
                                        when (motionEvent.action) {
                                            MotionEvent.ACTION_UP -> {
                                                val point = meteringPointFactory.createPoint(
                                                    motionEvent.x,
                                                    motionEvent.y
                                                )
                                                x = motionEvent.x.toDp.toInt()
                                                y = motionEvent.y.toDp.toInt()
                                                focusClick = true
                                                val action =
                                                    FocusMeteringAction.Builder(point).build()
                                                camera?.cameraControl?.startFocusAndMetering(
                                                    action
                                                )
                                                return@setOnTouchListener true
                                            }

                                            else -> {
                                                scaleGestureDetector.onTouchEvent(motionEvent)
                                                return@setOnTouchListener true
                                            }
                                        }
                                    }
                                }
                            },
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(3 / 4f)
                        )
                        val alpha by valueAnimation(
                            if (!focusClick) 0.5f else 0f,
                            tween(300, easing = LinearEasing)
                        ) {
                            focusClick = false
                        }
                        val size by valueAnimation(
                            if (!focusClick) 10.dp else 200.dp,
                            tween(300, easing = LinearEasing)
                        )

                        if (focusClick) {
                            Box(
                                Modifier
                                    .size(size)
                                    .offset(
                                        x.dp - (size.value * 0.5).dp,
                                        y.dp - (size.value * 0.5).dp
                                    )
                                    .alpha(alpha)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                        Column(
                            Modifier
                                .fillMaxSize()
                                .padding(10.dp), Arrangement.Bottom
                        ) {
                            SlideBar(
                                zoom, 0f..1f,
                                barColor = color1, circleColor = color2
                            ) {
                                zoom = it
                                camera?.cameraControl?.setLinearZoom(it)
                            }
                        }
                    }
                    specialBtn?.let {
                        Column(
                            Modifier
                                .fillMaxHeight()
                                .navigationBarsPadding()
                                .padding(bottom = 20.dp)
                                .padding(start = 10.dp), Arrangement.Bottom
                        ) {
                            it.invoke(::handleCapture)
                        }
                    }
                    if (allowVideo && allowPhoto && !isRecording) {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .navigationBarsPadding()
                                .padding(bottom = 30.dp)
                                .padding(end = 10.dp), Arrangement.Bottom, Alignment.End
                        ) {
                            Row(
                                Modifier
                                    .clip(CircleShape)
                                    .Click(color2) {
                                        isVideoFlow = !isVideoFlow
                                    }) {
                                GlideImg(
                                    R.drawable.baseline_photo_camera_24,
                                    Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(if (!isVideoFlow) color1 else Color.Transparent)
                                        .padding(5.dp),
                                    colorFilter = ColorFilter.tint(if (!isVideoFlow) color2 else color1)
                                )
                                SpacerH(20.dp)
                                GlideImg(
                                    R.drawable.baseline_videocam_24,
                                    Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(if (isVideoFlow) color1 else Color.Transparent)
                                        .padding(5.dp),
                                    colorFilter = ColorFilter.tint(if (isVideoFlow) color2 else color1)
                                )
                            }
                        }
                    }
                    Column(
                        Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .padding(bottom = 20.dp),
                        Arrangement.Bottom,
                        Alignment.CenterHorizontally
                    ) {
                        fun stopVideoRecording() {
                            currentRecording.value?.stop()
                            currentRecording.value = null
                            isRecording = false
                        }

                        LaunchedEffect(videoProgress) {
                            if (videoProgress >= 1f) {
                                stopVideoRecording()
                            }
                        }

                        var size by rem(IntSize.Zero)
                        Box(
                            Modifier
                                .size(
                                    if (size == IntSize.Zero) Dp.Unspecified
                                    else size.width.toDp.dp,
                                    if (size == IntSize.Zero) Dp.Unspecified
                                    else size.height.toDp.dp
                                )
                                .clip(CircleShape)
                                .background(color2)
                        ) {
                            if (size != IntSize.Zero) {
                                CircularProgressIndicator(
                                    progress = videoProgressAnim,
                                    Modifier.fillMaxSize(),
                                    strokeWidth = 4.dp,
                                    color = color1
                                )
                            }
                            Box(
                                Modifier
                                    .align(Alignment.Center)
                                    .onSizeChanged {
                                        size = it
                                    }
                                    .padding(4.dp)) {
                                captureBtn {
                                    if (!isVideoFlow) {
                                        takePhoto(
                                            context = context,
                                            perfix = perfix,
                                            imageCapture = imageCapture,
                                            executor = Executors.newSingleThreadExecutor(),
                                            onImageCaptured = ::handleCapture
                                        )
                                    } else {
                                        if (!isRecording) {
                                            startVideoRecording(
                                                context,
                                                perfix,
                                                videoCapture,
                                                currentRecording,
                                                {
                                                    isRecording = true
                                                },
                                                { file, error ->
                                                    stopVideoRecording()
                                                    handleCapture(file, error)
                                                })
                                        } else {
                                            stopVideoRecording()
                                        }
                                    }

                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        }
                    }
                }
            } else if (showPreview && file != null) {
                fun dismiss() {
                    file?.let {
                        if (it.exists() && showPreviewCase == 0) it.delete()
                    }
                    showPreview = false
                    file = null
                }
                BackPressHandler {
                    dismiss()
                }
                Box {

                    if (isVideoFlow) {
                        VideoView(
                            file,
                            Modifier
                                .padding(top = 55.dp)
                                .fillMaxWidth()
                                .aspectRatio(3 / 4f)
                                .background(Color.Black)
                        )
                    } else {
                        Box(
                            Modifier
                                .padding(top = 55.dp)
                                .fillMaxWidth()
                                .aspectRatio(3 / 4f)
                        ) {
                            GlideImg(file)
                        }
                    }

                    Row(
                        Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .padding(20.dp),
                        Arrangement.SpaceBetween,
                        Alignment.Bottom
                    ) {
                        dismissBtn {
                            dismiss()
                        }
                        acceptBtn {
                            handler(file, null)
                            close()
                            showPreview = false
                            file = null
                        }
                    }
                }
            } else {
                progressView()
            }
        }
    }
}

@Composable
fun VideoView(videoFile: File?, modifier: Modifier) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(Uri.fromFile(videoFile))
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    // Χρησιμοποίησε το AndroidView ως κανονικό Composable
    AndroidView(
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = true // Ενεργοποίηση ελέγχων αναπαραγωγής
            }
        },
        modifier = modifier
    )

    // Διαχείριση καθαρισμού του ExoPlayer
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
}

@SuppressLint("MissingPermission", "CheckResult")
private fun startVideoRecording(
    context: Context,
    perfix: String,
    videoCapture: VideoCapture<Recorder>,
    currentRecording: MutableState<Recording?>,
    onRecordingStarted: () -> Unit,
    onRecordingFinished: (f: File?, e: Exception?) -> Unit
) {
    val file = context.createVideoFile(perfix)
    val outputOptions = FileOutputOptions.Builder(file).build()

    currentRecording.value = videoCapture.output
        .prepareRecording(context, outputOptions)
        .withAudioEnabled()
        .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
            when (recordEvent) {
                is VideoRecordEvent.Start -> {
                    onRecordingStarted()
                    Log.d("CameraX", "Recording started")
                }

                is VideoRecordEvent.Finalize -> {
                    if (!recordEvent.hasError()) {
                        Log.d(
                            "CameraX",
                            "Recording finished: ${recordEvent.outputResults.outputUri}"
                        )
                        onRecordingFinished(file, null)
                    } else {
                        Log.e("CameraX", "Recording failed: ${recordEvent.error}")
                        try {
                            file.delete()
                        } catch (_: Exception) {
                        }
                        onRecordingFinished(
                            null,
                            Exception("Recording failed! Code: ${recordEvent.error}")
                        )
                    }
                }
            }
        }
}

private fun takePhoto(
    context: Context,
    perfix: String,
    imageCapture: ImageCapture,
    executor: Executor,
    onImageCaptured: (f: File?, e: Exception?) -> Unit
) {
    val file = context.createPhotoFile2(perfix)
    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exception: ImageCaptureException) {
                onImageCaptured(null, exception)
            }

            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        compress(file.path)
                        onImageCaptured(file, null)
                    } catch (e: Exception) {
                        try {
                            file.delete()
                        } catch (_: Exception) {
                        }
                        onImageCaptured(null, e)
                    }
                }
            }
        })
}

private suspend fun Context.getCameraProvider(onCaptured: (f: File?, e: Exception?) -> Unit): ProcessCameraProvider =
    suspendCoroutine { continuation ->
        try {
            ProcessCameraProvider.getInstance(this.applicationContext).also { cameraProvider ->
                cameraProvider.addListener({
                    continuation.resume(cameraProvider.get())
                }, ContextCompat.getMainExecutor(this))
            }
        } catch (e: Exception) {
            onCaptured(null, e)
        }
    }