package gr.sppzglou.easy.composable

import androidx.compose.animation.SplineBasedFloatDecayAnimationSpec
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.min

enum class BottomSheetValues {
    Hidden,
    HalfExpanded,
    Expanded;

    val draggableSpaceFraction: Float
        get() = when (this) {
            Hidden -> -0.3f
            HalfExpanded -> 0.5f
            Expanded -> 1f
        }
}

@Composable
fun rememberBottomSheetState(
    initialValue: BottomSheetValues = BottomSheetValues.Hidden,
    skipHalfExpanded: Boolean = false,
    isCancellable: Boolean = true,
    density: Density = LocalDensity.current
): BottomSheetStateV4 {
    return key(initialValue) {
        rememberSaveable(
            initialValue,
            saver = BottomSheetStateV4.Saver(
                isCancellable = isCancellable,
                skipHalfExpanded = skipHalfExpanded,
                density = density
            )
        ) {
            BottomSheetStateV4(
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
class BottomSheetStateV4(
    initialValue: BottomSheetValues,
    val skipHalfExpanded: Boolean,
    val isCancellable: Boolean,
    density: Density
) {

    val draggableState = AnchoredDraggableState(
        initialValue = initialValue,
        positionalThreshold = { totalDistance -> totalDistance * 0.5f },
        velocityThreshold = { 200.dpToPx.toFloat() },
        snapAnimationSpec = tween(),  // Η κινούμενη εικόνα
        decayAnimationSpec = SplineBasedFloatDecayAnimationSpec(density).generateDecayAnimationSpec(),  // Decay animation
    )
    val showSize = mutableFloatStateOf(0f)

    val layoutHeight = mutableIntStateOf(0)
    val sheetHeight = mutableIntStateOf(0)

    /**
     * The current value of the [BottomSheetStateV4].
     */
    val currentValue: BottomSheetValues
        get() = draggableState.currentValue

    val targetValue: BottomSheetValues
        get() = draggableState.targetValue

    /**
     * Whether the bottom sheet is visible.
     */
    val isVisible: Boolean
        get() = currentValue != BottomSheetValues.Hidden

    val isVisibleReal: Boolean
        get() = showSize.floatValue > 0

    /**
     * Whether the bottom sheet is expanded.
     */
    val isExpanded: Boolean
        get() = currentValue == BottomSheetValues.Expanded

    /**
     * Whether the bottom sheet is half expanded.
     */
    val isHalfExpanded: Boolean
        get() = currentValue == BottomSheetValues.HalfExpanded

    /**
     * Whether the bottom sheet is hidden.
     */
    val isHidden: Boolean
        get() = currentValue == BottomSheetValues.Hidden

    private val hasHalfExpandedState: Boolean
        get() = draggableState.anchors.hasAnchorFor(BottomSheetValues.HalfExpanded)

    /**
     * Show the bottom sheet with animation and suspend until it's shown.
     * If the sheet is taller than 50% of the parent's height, the bottom sheet will be half expanded.
     * Otherwise, it will be fully expanded.
     */
    suspend fun show() {
        val targetValue = if (hasHalfExpandedState) BottomSheetValues.HalfExpanded
        else BottomSheetValues.Expanded
        animateTo(targetValue)
    }

    /**
     * Expand the bottom sheet with an animation and suspend until the animation finishes or is cancelled.
     */
    suspend fun expand() {
        if (draggableState.anchors.hasAnchorFor(BottomSheetValues.Expanded)) {
            animateTo(BottomSheetValues.Expanded)
        }
    }

    /**
     * Half expand the bottom sheet with an animation and suspend until the animation finishes or is cancelled.
     */
    suspend fun halfExpand() {
        if (draggableState.anchors.hasAnchorFor(BottomSheetValues.HalfExpanded)) {
            animateTo(BottomSheetValues.HalfExpanded)
        }
    }

    /**
     * Hide the bottom sheet with an animation and suspend until the animation finishes or is cancelled.
     */
    suspend fun hide() {
        animateTo(BottomSheetValues.Hidden)
    }
    fun requireOffset(): Float {
        val offset = try {
            draggableState.requireOffset()
        } catch (_: Exception) {
            0f
        }
        showSize.floatValue = layoutHeight.intValue - offset
        return offset
    }

    private fun calcDragEndPoint(state: BottomSheetValues): Float {
        val fractionatedMaxDragEndPoint = layoutHeight.intValue * state.draggableSpaceFraction
        return layoutHeight.intValue.toFloat() - min(
            fractionatedMaxDragEndPoint,
            sheetHeight.intValue.toFloat()
        )
    }

    fun updateAnchors() {
        val newAnchors = DraggableAnchors {
            BottomSheetValues.Hidden at calcDragEndPoint(BottomSheetValues.Hidden)
            if (!skipHalfExpanded) {
                BottomSheetValues.HalfExpanded at calcDragEndPoint(BottomSheetValues.HalfExpanded)
            }
            BottomSheetValues.Expanded at calcDragEndPoint(BottomSheetValues.Expanded)
        }
        draggableState.updateAnchors(newAnchors)
    }

    private suspend fun animateTo(targetValue: BottomSheetValues) =
        draggableState.animateTo(targetValue)

    companion object {
        /**
         * The default [Saver] implementation for [BottomSheetStateV4].
         */
        fun Saver(
            isCancellable: Boolean,
            skipHalfExpanded: Boolean,
            density: Density
        ): Saver<BottomSheetStateV4, BottomSheetValues> =
            Saver(
                save = { it.currentValue },
                restore = {
                    BottomSheetStateV4(
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
    state: BottomSheetStateV4,
    modifier: Modifier = Modifier.background(
        Color.Gray,
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ),
    scrimColor: Color = Color.Black.copy(0.5f),
    content: @Composable () -> Unit
) {
    val offset by remember(state.draggableState.offset) {
        mutableStateOf(state.requireOffset().toDp.dp)
    }
    var alpha by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val bottomSheetNestedScrollConnection = remember(state.draggableState) {
        nestedScrollConnection(state)
    }
    val scrim by animateColorAsState(
        if (state.targetValue != BottomSheetValues.Hidden && state.sheetHeight.intValue > 0)
            scrimColor else Color.Transparent,
        tween(), label = ""
    )

    LaunchedEffect(state.layoutHeight.intValue, state.sheetHeight.intValue) {
        println(state.sheetHeight.intValue)
        if (state.layoutHeight.intValue > 0 && state.sheetHeight.intValue > 0) {
            alpha = if (state.sheetHeight.intValue == 0) 0f else 1f

            // Αποθήκευση της τρέχουσας θέσης πριν την ενημέρωση των anchors
            //val currentOffset = state.requireOffset()
            //val doReset = state.showSize.floatValue > 0
            // Ενημέρωση των anchors
            state.updateAnchors()

            /*if (doReset) {
                // Υπολογισμός των offset τιμών για κάθε κατάσταση
                val expandedOffset =
                    state.layoutHeight.intValue * BottomSheetValues.Expanded.draggableSpaceFraction
                val halfExpandedOffset =
                    state.layoutHeight.intValue * BottomSheetValues.HalfExpanded.draggableSpaceFraction

                // Επαναφορά στην αρχική θέση βάσει του currentOffset
                if (currentOffset >= expandedOffset)
                    state.draggableState.animateTo(BottomSheetValues.Expanded)
                else if (currentOffset >= halfExpandedOffset)
                    state.draggableState.animateTo(BottomSheetValues.HalfExpanded)
            }*/
        }
    }
    if (state.targetValue != BottomSheetValues.Hidden || state.isVisibleReal || state.sheetHeight.intValue == 0) {
    Box(
        modifier = Modifier
            .alpha(alpha)
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
            .onSizeChanged {
                state.layoutHeight.intValue = it.height
            }
            .offset(y = offset)
            .anchoredDraggable(
                state = state.draggableState,
                orientation = Orientation.Vertical,
                enabled = state.isCancellable
            )
            .nestedScroll(bottomSheetNestedScrollConnection)
            .then(modifier)
    ) {
        Box(
            modifier = Modifier
                .Tap { }
                .onSizeChanged {
                    if (it.height > 0) state.sheetHeight.intValue = it.height
                }
                .heightIn(min = 20.dp)
        ) {

            if (state.isVisibleReal || state.sheetHeight.intValue == 0) {
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

@OptIn(ExperimentalFoundationApi::class)
private fun nestedScrollConnection(
    state: BottomSheetStateV4
): NestedScrollConnection = object : NestedScrollConnection {

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (state.isCancellable) {
            val delta = available.offsetToFloat()
            return if (delta < 0 && source == NestedScrollSource.UserInput) {
                state.draggableState.dispatchRawDelta(delta).toOffset()
            } else {
                Offset.Zero
            }
        } else return super.onPreScroll(available, source)
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        if (state.isCancellable) {
            val delta = available.offsetToFloat()
            return if (source == NestedScrollSource.UserInput) {
                state.draggableState.dispatchRawDelta(delta).toOffset()
            } else {
                Offset.Zero
            }
        } else return super.onPostScroll(consumed, available, source)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (state.isCancellable) {
            val toFling = available.velocityToFloat()
            val currentOffset = state.requireOffset()
            return if (toFling < 0 && currentOffset > state.draggableState.anchors.minAnchor()) {
                state.draggableState.settle(toFling)
                available
            } else {
                Velocity.Zero
            }
        } else return super.onPreFling(available)
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        if (state.isCancellable) {
            val toFling = available.velocityToFloat()
            state.draggableState.settle(toFling)
            return available
        } else return super.onPostFling(consumed, available)
    }


    private fun Offset.offsetToFloat(): Float = y

    private fun Float.toOffset(): Offset = Offset(
        x = 0f,
        y = this
    )

    private fun Velocity.velocityToFloat() = y
}