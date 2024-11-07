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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import kotlin.math.min
import kotlin.math.roundToInt

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

data class LayoutSizes(
    var displayedSheetSize: Int = 0,
    var containerSize: Int = 0,
    var sheetSize: Int = 0,
    var progress: Float = 0f
)

@Composable
fun rememberBottomSheetState(
    initialValue: SheetValues = SheetValues.Hidden,
    skipHalfExpanded: Boolean = false,
    isCancellable: Boolean = true,
    density: Density = LocalDensity.current
): BottomSheetStateV3 {
    return key(initialValue) {
        rememberSaveable(
            initialValue,
            saver = BottomSheetStateV3.Saver(
                isCancellable = isCancellable,
                skipHalfExpanded = skipHalfExpanded,
                density = density
            )
        ) {
            BottomSheetStateV3(
                initialValue = initialValue,
                skipHalfExpanded = skipHalfExpanded,
                isCancellable = isCancellable,
                density = density
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Stable
class BottomSheetStateV3(
    initialValue: SheetValues,
    val skipHalfExpanded: Boolean,
    val isCancellable: Boolean,
    density: Density
) {
    val sizes = mutableStateOf(LayoutSizes())
    fun setContainerSize(value: Int) {
        sizes.value.containerSize = value
    }

    fun setSheetSize(value: Int) {
//        sizes.value.sheetSize = value
        sheetSize.intValue = value
    }

    val draggableState = AnchoredDraggableState(
        initialValue = initialValue,
        snapAnimationSpec = sheetAnimation1,
        positionalThreshold = { totalDistance -> totalDistance * 0.5f },
        velocityThreshold = { 50.dpToPx.toFloat() },
        decayAnimationSpec = SplineBasedFloatDecayAnimationSpec(density).generateDecayAnimationSpec()
    )

    val sheetSize = mutableIntStateOf(0)

    /**
     * The current value of the [BottomSheetStateV3].
     */
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
        val targetValue = if (skipHalfExpanded) {
            SheetValues.Expanded
        } else {
            if (hasHalfExpandedState) SheetValues.HalfExpanded
            else SheetValues.Expanded
        }
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

    fun requireOffset(): Int {
        val offset = try {
            draggableState.requireOffset()
        } catch (_: Exception) {
            0f
        }
        with(sizes.value) {
            displayedSheetSize = (containerSize - offset).toInt()
            progress = if (sheetSize > 0) displayedSheetSize.toFloat() / sheetSize.toFloat()
            else 0f
        }
        return offset.roundToInt()
    }

    private fun calcDragEndPoint(state: SheetValues): Float {
        with(sizes.value) {
            val fractionatedMaxDragEndPoint =
                containerSize * state.draggableSpaceFraction
            return containerSize.toFloat() - min(
                fractionatedMaxDragEndPoint,
                this@BottomSheetStateV3.sheetSize.intValue.toFloat()
            )
        }
    }

    fun updateAnchors() {
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
            density: Density
        ): Saver<BottomSheetStateV3, SheetValues> =
            Saver(
                save = { it.currentValue },
                restore = {
                    BottomSheetStateV3(
                        initialValue = it,
                        skipHalfExpanded = skipHalfExpanded,
                        isCancellable = isCancellable,
                        density = density
                    )
                }
            )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BottomSheet(
    state: BottomSheetStateV3,
    modifier: Modifier = Modifier.background(
        Color.Gray,
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ),
    scrimColor: Color = Color.Black.copy(0.5f),
    content: @Composable () -> Unit
) {
    val sheetSize by remember(state.sheetSize.intValue) {
        mutableIntStateOf(state.sheetSize.intValue)
    }
    val scope = rememberCoroutineScope()
    val nestedScrollConnection = remember(state) {
        nestedScrollConnection(state)
    }
    val scrim by animateColorAsState(
        if (state.targetValue != SheetValues.Hidden && sheetSize > 0)
            scrimColor else Color.Transparent,
        tween(), label = ""
    )

    Box(
        Modifier
            .onSizeChanged {
                state.setContainerSize(it.height)
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
        if (state.targetValue != SheetValues.Hidden || state.isVisibleReal || sheetSize == 0) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset {
                        val yOffset = state.requireOffset()
                        IntOffset(x = 0, y = yOffset)
                    }
                    .anchoredDraggable(
                        state = state.draggableState,
                        orientation = Orientation.Vertical,
                        enabled = state.isCancellable
                    )
                    .nestedScroll(nestedScrollConnection)
                    .then(modifier)
            ) {
                Box(
                    modifier = Modifier
                        .alpha(if (sheetSize == 0) 0f else 1f)
                        .Tap { }
                        .onSizeChanged {
                            state.setSheetSize(it.height)
                            if (sheetSize > 0) {
                                state.updateAnchors()
                            }
                        }
                        .animateContentSize(sheetAnimation2)
                        .heightIn(min = 50.dp)
                ) {
                    if (state.isVisibleReal || sheetSize == 0) {
                        if (scrimColor != Color.Unspecified) {
                            BackPressHandler {
                                if (state.isCancellable) state.hide()
                            }
                        }
                        content()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal fun nestedScrollConnection(
    state: BottomSheetStateV3
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