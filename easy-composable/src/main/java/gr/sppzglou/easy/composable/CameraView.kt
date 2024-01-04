package gr.sppzglou.easy.composable

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class Camera {
    private val isOpen = mutableStateOf(false)
    private var perfix = ""
    private var handler: (f: File?) -> Unit = {}

    fun show() {
        isOpen.value = true
    }

    fun hide() {
        isOpen.value = false
    }

    fun setFileDescription(d: String) {
        perfix = d
    }

    fun imageHandler(h: (f: File?) -> Unit) {
        handler = h
    }

    fun setup(perfix: String, h: (f: File?) -> Unit) {
        this.perfix = perfix
        handler = h
        show()
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun SetupView(
        backBtn: @Composable (suspend () -> Unit) -> Unit,
        flashBtn: @Composable (Boolean, suspend () -> Unit) -> Unit,
        captureBtn: @Composable (suspend () -> Unit) -> Unit,
        acceptBtn: @Composable (suspend () -> Unit) -> Unit,
        dismissBtn: @Composable (suspend () -> Unit) -> Unit,
        progressView: @Composable () -> Unit,
        padding: Dp = 0.dp,
        corners: Dp = 0.dp,
        color1: Color = Color.Black,
        color2: Color = Color.White
    ) {
        val dialogState = rememberBottomSheetState(true, true, false)


        BottomSheet(dialogState, onStateChange = { _, _ -> }) {
            Box(
                Modifier
                    .fillMaxSize()
                    .Tap { }
            ) {
                Box(
                    Modifier
                        .statusBarsPadding()
                        .fillMaxSize()
                        .Tap { }
                ) {
                    if (isOpen.value) {
                        BackPressHandler {
                            isOpen.value = false
                        }
                    }
                    CameraView(
                        backBtn,
                        flashBtn,
                        captureBtn,
                        acceptBtn,
                        dismissBtn,
                        progressView,
                        padding,
                        corners
                    )
                }
            }
        }

        LaunchedEffect(isOpen.value) {
            if (isOpen.value) dialogState.show()
            else dialogState.hide()
        }
    }

    @SuppressLint("ClickableViewAccessibility", "RestrictedApi")
    @Composable
    private fun CameraView(
        backBtn: @Composable (suspend () -> Unit) -> Unit,
        flashBtn: @Composable (Boolean, suspend () -> Unit) -> Unit,
        captureBtn: @Composable (suspend () -> Unit) -> Unit,
        acceptBtn: @Composable (suspend () -> Unit) -> Unit,
        dismissBtn: @Composable (suspend () -> Unit) -> Unit,
        progressView: @Composable () -> Unit,
        padding: Dp = 0.dp,
        corners: Dp = 0.dp,
        color1: Color = Color.Black,
        color2: Color = Color.White
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
            } else {
                file = null
                showPreview = false
                act?.runOnUiThread {
                    handleImageCapture(null, e)
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
                        backBtn {
                            hide()
                        }
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
                            val zoom = rem(0f)
                            var currentZoom by rem(0f)
                            LaunchedEffect(currentZoom) {
                                zoom.value = currentZoom
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
                                    zoom.value = it
                                    camera?.cameraControl?.setLinearZoom(it)
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
                            captureBtn {
                                takePhoto(
                                    context = context,
                                    imageCapture = imageCapture,
                                    executor = Executors.newSingleThreadExecutor(),
                                    onImageCaptured = ::handleImageCapture
                                )
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showPreview = true
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
                            acceptBtn {
                                dismiss()
                            }
                            dismissBtn {
                                handler(file)
                                isOpen.value = false
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
}

fun Context.createPhotoFile(
    perfix: String,
): File {
    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    return File.createTempFile(
        "${perfix}_${timeStamp}_",
        ".jpg",
        this.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    )
}

fun compress(filePath: String) {
    //Keep exif information
    val exifInterface = ExifInterface(filePath)

    //Resize and save
    var bitmap = BitmapFactory.decodeFile(filePath)
    var width = bitmap.width
    var height = bitmap.height
    if (width > height) {
        val ratio = width.toFloat() / 1000
        width = 1000
        height = (height / ratio).toInt()
    } else {
        val ratio = height.toFloat() / 1000
        height = 1000
        width = (width / ratio).toInt()
    }
    bitmap = bitmap.scale(width, height)
    val fileOutputStream = FileOutputStream(filePath)
    bitmap.compress(Bitmap.CompressFormat.JPEG, 75, fileOutputStream)
    fileOutputStream.close()
    bitmap.recycle()

    //Store back exif information
    exifInterface.saveAttributes()
}