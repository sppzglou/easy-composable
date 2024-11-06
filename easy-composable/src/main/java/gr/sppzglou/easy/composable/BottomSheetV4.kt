package gr.sppzglou.easy.composable

import androidx.compose.animation.SplineBasedFloatDecayAnimationSpec
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.generateDecayAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

val sheetAnimation1: AnimationSpec<Float> =
    tween(durationMillis = 300, easing = FastOutSlowInEasing)
val sheetAnimation2: FiniteAnimationSpec<IntSize> =
    tween(durationMillis = 300, easing = FastOutSlowInEasing)

enum class SheetValues {
    Hidden,
    HalfExpanded,
    Expanded;

    val draggableSpaceFraction: Float
        get() = when (this) {
            Hidden -> 0f
            HalfExpanded -> 0.5f
            Expanded -> 1f
        }
}

@Composable
fun rememberBottomSheetState(
    initialValue: SheetValues = SheetValues.Hidden,
    skipHalfExpanded: Boolean = false,
    isCancellable: Boolean = true,
    isAlwaysRunContent: Boolean = false,
    density: Density = LocalDensity.current
): BottomSheetStateV4 {
    return key(initialValue) {
        rememberSaveable(
            initialValue,
            saver = BottomSheetStateV4.Saver(
                isCancellable = isCancellable,
                skipHalfExpanded = skipHalfExpanded,
                isAlwaysRunContent = isAlwaysRunContent,
                density = density
            )
        ) {
            BottomSheetStateV4(
                initialValue = initialValue,
                skipHalfExpanded = skipHalfExpanded,
                isCancellable = isCancellable,
                isAlwaysRunContent = isAlwaysRunContent,
                density = density
            )
        }
    }
}

data class LayoutSizes(
    var displayedSheetSize: Int = 0,
    var containerSize: Int = 0,
    var sheetSize: Int = 0
)

@OptIn(ExperimentalFoundationApi::class)
@Stable
class BottomSheetStateV4(
    initialValue: SheetValues,
    val skipHalfExpanded: Boolean,
    val isCancellable: Boolean,
    val isAlwaysRunContent: Boolean,
    density: Density
) {
    val sizes = mutableStateOf(LayoutSizes())
    
    val draggableState = AnchoredDraggableState(
        initialValue = initialValue,
        snapAnimationSpec = sheetAnimation1,
        positionalThreshold = { totalDistance -> totalDistance * 0.5f },
        velocityThreshold = { 50.dpToPx.toFloat() },
        decayAnimationSpec = SplineBasedFloatDecayAnimationSpec(density).generateDecayAnimationSpec()
    )

    val currentValue: SheetValues
        get() = draggableState.currentValue

    val targetValue: SheetValues
        get() = draggableState.targetValue

    val isVisible: Boolean
        get() = currentValue != SheetValues.Hidden

    val isVisibleReal: Boolean
        get() = sizes.value.displayedSheetSize > 0

    val isExpanded: Boolean
        get() = currentValue == SheetValues.Expanded

    val isHalfExpanded: Boolean
        get() = currentValue == SheetValues.HalfExpanded

    val isHidden: Boolean
        get() = currentValue == SheetValues.Hidden

    private val hasHalfExpandedState: Boolean
        get() = draggableState.anchors.hasAnchorFor(SheetValues.HalfExpanded)

    suspend fun show() {
        val targetValue = if (hasHalfExpandedState) SheetValues.HalfExpanded
        else SheetValues.Expanded
        animateTo(targetValue)
    }

    suspend fun expand() {
        if (draggableState.anchors.hasAnchorFor(SheetValues.Expanded)) {
            animateTo(SheetValues.Expanded)
        }
    }

    suspend fun halfExpand() {
        if (draggableState.anchors.hasAnchorFor(SheetValues.HalfExpanded)) {
            animateTo(SheetValues.HalfExpanded)
        }
    }

    suspend fun hide() {
        animateTo(SheetValues.Hidden)
    }

    fun requireOffset(): Float {
        val offset = try {
            draggableState.requireOffset()
        } catch (_: Exception) {
            0f
        }
        sizes.value.displayedSheetSize = sizes.value.sheetSize - offset.toInt()
        return offset
    }

    fun updateAnchors() {
        fun calcDragEndPoint(sheetValue: SheetValues): Float {
            val dragPoint =
                sizes.value.sheetSize - (sizes.value.containerSize * sheetValue.draggableSpaceFraction)
            return dragPoint.coerceAtLeast(0f)
        }

        val newAnchors = DraggableAnchors {
            SheetValues.Hidden at calcDragEndPoint(SheetValues.Hidden)
            if (!skipHalfExpanded) {
                SheetValues.HalfExpanded at calcDragEndPoint(SheetValues.HalfExpanded)
            }
            SheetValues.Expanded at calcDragEndPoint(SheetValues.Expanded)
        }
        draggableState.updateAnchors(newAnchors, draggableState.targetValue)
    }

    private suspend fun animateTo(targetValue: SheetValues) =
        draggableState.animateTo(targetValue)

    companion object {
        fun Saver(
            isCancellable: Boolean,
            skipHalfExpanded: Boolean,
            isAlwaysRunContent: Boolean,
            density: Density
        ): Saver<BottomSheetStateV4, SheetValues> =
            Saver(
                save = { it.currentValue },
                restore = {
                    BottomSheetStateV4(
                        initialValue = it,
                        skipHalfExpanded = skipHalfExpanded,
                        isCancellable = isCancellable,
                        isAlwaysRunContent = isAlwaysRunContent,
                        density = density
                    )
                }
            )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BottomSheet(
    state: BottomSheetStateV4,
    modifier: Modifier = Modifier.background(
        Color.Gray,
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ),
    scrimColor: Color = Color.Black.copy(0.5f),
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()

    val nestedScrollConnection = remember(state) {
        nestedScrollConnection(state)
    }

    val scrim by animateColorAsState(
        if (state.targetValue != SheetValues.Hidden && state.sizes.value.sheetSize > 0)
            scrimColor else Color.Transparent,
        tween(), label = ""
    )

    Box(
        Modifier
            .onSizeChanged {
                state.sizes.value.containerSize = it.height
            }
            .fillMaxSize()
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(scrim)
                .applyIf(state.isVisible && scrimColor != Color.Unspecified) {
                    pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            if (state.isCancellable) {
                                scope.launch {
                                    state.hide()
                                }
                            }
                        })
                    }
                }
        )
        if (state.sizes.value.containerSize != 0) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .nestedScroll(nestedScrollConnection)
                    .offset {
                        val sheetOffsetY = state
                            .requireOffset()
                            .toInt()
                        IntOffset(x = 0, y = sheetOffsetY)
                    }
                    .anchoredDraggable(state.draggableState, Orientation.Vertical)
                    .onSizeChanged {
                        if (it.height > 50.dpToPx) {
                            state.sizes.value.sheetSize = it.height
                            state.updateAnchors()
                        }
                    }
                    .then(modifier)
                    .Tap { }
                    .heightIn(min = 50.dp)
                    .animateContentSize(sheetAnimation2),
                color = Color.Transparent
            ) {
                if (state.isVisibleReal || state.sizes.value.sheetSize == 0) {
                    if (scrimColor != Color.Unspecified) {
                        BackPressHandler {
                            if (state.isCancellable) state.hide()
                        }
                    }
                }
                if (state.isAlwaysRunContent) {
                    content()
                } else if (state.isVisibleReal || state.sizes.value.sheetSize == 0) {
                    content()
                }
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
internal fun nestedScrollConnection(
    state: BottomSheetStateV4
): NestedScrollConnection =
    object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
            if (state.isCancellable) {
                val delta = available.offsetToFloat()
                if (delta < 0 && source == NestedScrollSource.UserInput) {
                    state.draggableState.dispatchRawDelta(delta).toOffset()
                } else {
                    Offset.Zero
                }
            } else {
                super.onPreScroll(available, source)
            }


        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset = if (state.isCancellable) {
            if (source == NestedScrollSource.UserInput) {
                state.draggableState.dispatchRawDelta(available.offsetToFloat()).toOffset()
            } else {
                Offset.Zero
            }
        } else super.onPostScroll(consumed, available, source)

        override suspend fun onPreFling(available: Velocity): Velocity = if (state.isCancellable) {
            val toFling = available.toFloat()
            val currentOffset = state.requireOffset()
            val minAnchor = state.draggableState.anchors.minAnchor()

            if (toFling < 0 && currentOffset > minAnchor) {
                state.draggableState.settle(toFling)
                available
            } else {
                Velocity.Zero
            }
        } else super.onPreFling(available)

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
            if (state.isCancellable) {
                val onFling = available.toFloat()
                state.draggableState.settle(onFling)
                available
            } else super.onPostFling(consumed, available)

        private fun Float.toOffset(): Offset = Offset(
            x = 0f,
            y = this
        )

        private fun Velocity.toFloat() = y
        private fun Offset.offsetToFloat(): Float = y

    }