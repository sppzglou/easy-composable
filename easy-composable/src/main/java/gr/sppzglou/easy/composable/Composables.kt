package gr.sppzglou.easy.composable

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.request.RequestListener
import com.github.krottv.compose.sliders.DefaultThumb
import com.github.krottv.compose.sliders.DefaultTrack
import com.github.krottv.compose.sliders.SliderValueHorizontal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun <T> rem(v: T) = remember { mutableStateOf(v) }

@Composable
fun <T> remS(v: T) = rememberSaveable { mutableStateOf(v) }

@Composable
fun <T> remList() = remember { mutableStateListOf<T>() }

@Composable
fun <K, V> remMap() = remember { mutableStateMapOf<K, V>() }

@Composable
fun <T> rem(vararg v: T) = remember { mutableStateOf(v) }

@Composable
fun context() = LocalContext.current

@Composable
fun lifecycle() = LocalLifecycleOwner.current

@Composable
fun SpacerV(dp: Dp) {
    Spacer(Modifier.padding(top = safeDp(dp)))
}

@Composable
fun SpacerStatusBar(plus: Number = 0) {
    Spacer(
        Modifier
            .statusBarsPadding()
            .padding(top = plus.toFloat().dp)
    )
}

@Composable
fun SpacerNavBar(plus: Number = 0) {
    Spacer(
        Modifier
            .navigationBarsPadding()
            .padding(top = plus.toFloat().dp)
    )
}

@Composable
fun SpacerH(dp: Dp) {
    Spacer(Modifier.padding(start = safeDp(dp)))
}

@Composable
fun navBarSize(): Int {
    var h by rem(0)
    Box(
        Modifier
            .onSizeChanged { h = it.height.toDp }
            .navigationBarsPadding())
    return h
}

@Composable
fun statusBarSize(): Int {
    var h by rem(0)
    Box(
        Modifier
            .onSizeChanged { h = it.height.toDp }
            .statusBarsPadding())
    return h
}


@Composable
fun screenHeightDp() = LocalConfiguration.current.screenHeightDp

@Composable
fun screenWidthDp() = LocalConfiguration.current.screenWidthDp

@Composable
fun Launch(block: suspend CoroutineScope.() -> Unit) {
    LaunchedEffect(Unit) {
        block()
    }
}

@Composable
fun Dispose(onDisposeEffect: () -> Unit) {
    DisposableEffect(Unit) {
        onDispose {
            onDisposeEffect()
        }
    }
}

@Composable
fun LifecycleOwner.LifecycleEvents(listener: (Lifecycle.Event) -> Unit = {}) {
    DisposableEffect(this) {
        val observer = LifecycleEventObserver { _, event ->
            listener(event)
        }
        this@LifecycleEvents.lifecycle.addObserver(observer)
        onDispose {
            this@LifecycleEvents.lifecycle.removeObserver(observer)
        }
    }
}

@Composable
fun BackPressHandler(
    backPressedDispatcher: OnBackPressedDispatcher? =
        LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher,
    onBackPressed: suspend () -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentOnBackPressed by rememberUpdatedState(newValue = onBackPressed)

    val backCallback = remember {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                scope.launch {
                    currentOnBackPressed()
                }
            }
        }
    }
    val lifecycle = lifecycle()

    lifecycle.LifecycleEvents { event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            backPressedDispatcher?.addCallback(backCallback)
        }
        if (event == Lifecycle.Event.ON_PAUSE) {
            backCallback.remove()
        }
    }

    DisposableEffect(backPressedDispatcher) {
        backPressedDispatcher?.addCallback(backCallback)

        onDispose {
            backCallback.remove()
        }
    }
}

@Composable
fun OnBackPress(
    isBack: MutableState<Boolean>, backPressedDispatcher: OnBackPressedDispatcher? =
        LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
) {
    LaunchedEffect(isBack.value) {
        if (isBack.value) {
            backPressedDispatcher?.onBackPressed()
            isBack.value = false
        }
    }
}

@Composable
fun Modifier.Click(
    ripple: Color = Color.Black,
    interaction: MutableInteractionSource = remember { MutableInteractionSource() },
    bounded: Boolean = true,
    click: suspend () -> Unit
): Modifier {
    val scope = rememberCoroutineScope()
    return this.clickable(
        interactionSource = interaction,
        indication = rememberRipple(bounded = bounded, color = ripple),
        onClick = {
            scope.launch {
                click()
            }
        }
    )
}

@Composable
fun Modifier.Tap(function: () -> Unit) = this.pointerInput(Unit) {
    detectTapGestures(onTap = {
        function()
    })
}

@SuppressLint("UnrememberedMutableState")
@Composable
fun <T> valueAnimation(
    targetValue: T,
    animationSpec: AnimationSpec<T> = tween(500),
    finishedListener: ((T) -> Unit)? = null
): State<T> = when (targetValue) {
    is Float -> animateFloatAsState(
        targetValue,
        animationSpec as AnimationSpec<Float>,
        label = "FloatAnim",
        finishedListener = finishedListener as ((Float) -> Unit)?
    ) as State<T>

    is Int -> animateIntAsState(
        targetValue,
        animationSpec as AnimationSpec<Int>,
        "IntAnim",
        finishedListener as ((Int) -> Unit)?
    ) as State<T>

    is Dp -> animateDpAsState(
        targetValue,
        animationSpec as AnimationSpec<Dp>,
        "DpAnim",
        finishedListener as ((Dp) -> Unit)?
    ) as State<T>

    is Color -> animateColorAsState(
        targetValue,
        animationSpec as AnimationSpec<Color>,
        "ColorAnim",
        finishedListener as ((Color) -> Unit)?
    ) as State<T>

    else -> mutableStateOf(targetValue)
}

@Composable
fun AutoSizeText(
    text: String,
    color: Color,
    textStyle: TextStyle,
    modifier: Modifier = Modifier
) {
    var scaledTextStyle by remember { mutableStateOf(textStyle) }
    var readyToDraw by remember { mutableStateOf(false) }

    LaunchedEffect(color) {
        scaledTextStyle = scaledTextStyle.copy(color)
    }

    Text(
        text,
        modifier.drawWithContent {
            if (readyToDraw) {
                drawContent()
            }
        },
        style = scaledTextStyle,
        softWrap = false,
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.didOverflowWidth) {
                scaledTextStyle =
                    scaledTextStyle.copy(fontSize = scaledTextStyle.fontSize * 0.9)
            } else {
                readyToDraw = true
            }
        }
    )
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun GlideImg(
    imageModel: Any?,
    modifier: Modifier = Modifier,
    colorFilter: ColorFilter? = null,
    contentScale: ContentScale = ContentScale.Crop,
    requestListener: RequestListener<Drawable>? = null
) {

    GlideImage(
        model = imageModel,
        contentDescription = "",
        modifier = modifier,
        contentScale = contentScale,
        colorFilter = colorFilter
    ) {
        it.addListener(requestListener)
    }
}


@Composable
fun <T : Number> SlideBar(
    distance: T,
    closedFloatingPointRange: ClosedFloatingPointRange<Float>,
    onFinish: () -> Unit = {},
    barColor: Color = Color.White,
    barColor2: Color = Color.Gray,
    circleColor: Color = Color.Black,
    circleScale: Float = 1f,
    scaleOnPress: Float = 2f,
    function: (Float) -> Unit
) {
    SliderValueHorizontal(
        distance.toFloat(), { function(it) },
        onValueChangeFinished = {
            onFinish()
        },
        modifier = Modifier.fillMaxWidth(),
        valueRange = closedFloatingPointRange,
        thumbSizeInDp = DpSize(8.dp, 8.dp),
        thumb = { modifier, offset, interactionSource, enabled, thumbSize ->
            DefaultThumb(
                modifier.scale(circleScale), offset, interactionSource, enabled, thumbSize,
                color = circleColor,
                scaleOnPress = scaleOnPress
            )
        },
        track = { modifier,
                  fraction,
                  interactionSource,
                  tickFractions,
                  enabled ->

            DefaultTrack(
                modifier,
                fraction,
                interactionSource,
                tickFractions,
                enabled,
                colorTrack = barColor2,
                colorProgress = barColor
            )
        })
}