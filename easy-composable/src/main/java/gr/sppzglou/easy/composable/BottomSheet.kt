package gr.sppzglou.easy.composable

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.SwipeableDefaults
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BottomSheet(
    state: BottomSheetState,
    backgroundColor: Color = Color.Gray,
    onStateChange: (Boolean, ModalBottomSheetState) -> Unit,
    lifecycle: LifecycleOwner = LocalLifecycleOwner.current,
    content: @Composable () -> Unit
) {
    val con = context()

    Launch {
        (con as? FragmentActivity)?.apply {
            val viewGroup = this.findViewById(android.R.id.content) as ViewGroup

            viewGroup.addView(
                ComposeView(viewGroup.context).apply {
                    setContent {
                        BottomSheetWrapper(
                            lifecycle,
                            viewGroup,
                            this,
                            state,
                            content,
                            backgroundColor,
                            onStateChange
                        )
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun BottomSheetWrapper(
    lifecycle: LifecycleOwner,
    parent: ViewGroup,
    composeView: ComposeView,
    sheetState: BottomSheetState,
    content: @Composable () -> Unit,
    backgroundColor: Color,
    onStateChange: (Boolean, ModalBottomSheetState) -> Unit
) {
    val act = context() as? FragmentActivity
    val lifecycleAct = lifecycle()
    val scope = rememberCoroutineScope()
    var lifecycleActEvent by rem<Lifecycle.Event?>(null)
    var lifecycleEvent by rem<Lifecycle.Event?>(null)
    var isSheetVisible by rem(false)

    val state = rememberModalBottomSheetState(
        ModalBottomSheetValue.Hidden,
        SwipeableDefaults.AnimationSpec, {
            true
        }, sheetState.skippHalf
    )

    LaunchedEffect(sheetState.isVisible) {
        if (sheetState.isVisible) {
            act?.hideKeyboard()
            isSheetVisible = true
        }
    }

    LaunchedEffect(isSheetVisible) {
        if (!isSheetVisible) {
            onStateChange(false, state)
        }
    }

    if (isSheetVisible) {
        ModalBottomSheetLayout(
            sheetGesturesEnabled = sheetState.isCancellable,
            sheetBackgroundColor = Color.Transparent,
            sheetElevation = 0.dp,
            sheetState = state,
            scrimColor = Color.Transparent,
            sheetContent = {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .clip(RoundedCornerShape(topEnd = 20.dp, topStart = 20.dp))
                        .background(backgroundColor)
                ) {
                    if (sheetState.isFullScreen) {
                        Box(Modifier.fillMaxSize()) {
                            content()
                        }
                    } else content()
                }
            }
        ) {}

        Launch {
            onStateChange(true, state)
            state.show()
        }
    }


    DisposableEffect(sheetState.isVisible) {
        onDispose {
            if (!sheetState.isVisible) {
                scope.launch {
                    state.hide()
                }
            }
        }
    }

    DisposableEffect(state.isVisible) {
        onDispose {
            if (!state.isVisible) {
                isSheetVisible = false
                sheetState.hide()
            }
        }
    }

    if (isSheetVisible || sheetState.isVisible || state.isVisible) {
        BackPressHandler {
            sheetState.hide()
        }
    }

    lifecycle.LifecycleEvents { event ->
        lifecycleEvent = event
    }
    lifecycleAct.LifecycleEvents { event ->
        lifecycleActEvent = event
    }

    LaunchedEffect(lifecycleEvent, lifecycleActEvent) {
        if (lifecycleEvent == Lifecycle.Event.ON_STOP && lifecycleActEvent != Lifecycle.Event.ON_STOP) {
            if (isSheetVisible || sheetState.isVisible || state.isVisible) {
                onStateChange(false, state)
            }
            parent.removeView(composeView)
        }
    }
}

@Composable
fun rememberBottomSheetState(
    skippHalf: Boolean = false,
    isFullScreen: Boolean = false,
    isCancellable: Boolean = true,
    initialValue: Boolean = false
): BottomSheetState {
    return rememberSaveable(
        skippHalf,
        isFullScreen,
        isCancellable,
        saver = BottomSheetState.Saver(skippHalf, isFullScreen, isCancellable)
    ) {
        BottomSheetState(initialValue, skippHalf, isFullScreen, isCancellable)
    }
}

@Stable
class BottomSheetState(
    initialValue: Boolean,
    val skippHalf: Boolean = false,
    val isFullScreen: Boolean = false,
    val isCancellable: Boolean = true
) {
    var isVisible by mutableStateOf(initialValue)

    fun show() {
        isVisible = true
    }

    fun hide() {
        isVisible = false
    }

    companion object {
        fun Saver(
            skippHalf: Boolean,
            isFullScreen: Boolean,
            isCancellable: Boolean
        ): Saver<BottomSheetState, *> = Saver(
            save = { it.isVisible },
            restore = { BottomSheetState(it, skippHalf, isFullScreen, isCancellable) }
        )
    }
}