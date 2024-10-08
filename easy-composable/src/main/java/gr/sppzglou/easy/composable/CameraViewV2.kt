package gr.sppzglou.easy.composable

import android.annotation.SuppressLint
import android.content.Context
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
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@SuppressLint("ClickableViewAccessibility", "RestrictedApi")
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
    specialBtn: (@Composable ((File?, kotlin.Exception?) -> Unit) -> Unit)? = null,
    perfix: String = "",
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
    val cameraSelector = remember {
        CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()
    }
    var file by rem<File?>(null)

    fun handleImageCapture(f: File?, e: Exception?) {
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
                    val cameraProvider = context.getCameraProvider(::handleImageCapture)
                    if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycle,
                            cameraSelector,
                            cameraView,
                            imageCapture
                        )
                        cameraView.setSurfaceProvider(previewView.surfaceProvider)
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
                                                x = motionEvent.x.toInt().toDp
                                                y = motionEvent.y.toInt().toDp
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
                                .padding(start = 10.dp), Arrangement.Bottom
                        ) {
                            it.invoke(::handleImageCapture)
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
                        captureBtn {
                            takePhoto(
                                context = context,
                                perfix = perfix,
                                imageCapture = imageCapture,
                                executor = Executors.newSingleThreadExecutor(),
                                onImageCaptured = ::handleImageCapture
                            )
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                    GlideImg(
                        file,
                        Modifier
                            .padding(top = 55.dp)
                            .fillMaxWidth()
                            .aspectRatio(3 / 4f)
                    )
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

private fun takePhoto(
    context: Context,
    perfix: String,
    imageCapture: ImageCapture,
    executor: Executor,
    onImageCaptured: (f: File?, e: Exception?) -> Unit
) {
    val file = context.createPhotoFile(perfix)
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
                        onImageCaptured(null, e)
                    }
                }
            }
        })
}

private suspend fun Context.getCameraProvider(onImageCaptured: (f: File?, e: Exception?) -> Unit): ProcessCameraProvider =
    suspendCoroutine { continuation ->
        try {
            ProcessCameraProvider.getInstance(this.applicationContext).also { cameraProvider ->
                cameraProvider.addListener({
                    continuation.resume(cameraProvider.get())
                }, ContextCompat.getMainExecutor(this))
            }
        } catch (e: Exception) {
            onImageCaptured(null, e)
        }
    }