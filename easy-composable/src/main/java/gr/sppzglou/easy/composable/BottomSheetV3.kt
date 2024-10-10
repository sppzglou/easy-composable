package gr.sppzglou.easy.composable

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.exponentialDecay
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun rememberBottomSheetState(
    initialValue: BottomSheetValueV3 = BottomSheetValueV3.Hidden,
    skipHalfExpanded: Boolean = false,
    isCancellable: Boolean = true,
    confirmValueChange: (BottomSheetValueV3) -> Boolean = { isCancellable },
): BottomSheetStateV3 {
    return key(initialValue) {
        rememberSaveable(
            initialValue, confirmValueChange,
            saver = BottomSheetStateV3.Saver(
                isCancellable = isCancellable,
                skipHalfExpanded = skipHalfExpanded,
                confirmValueChange = confirmValueChange
            )
        ) {
            BottomSheetStateV3(
                initialValue = initialValue,
                skipHalfExpanded = skipHalfExpanded,
                isCancellable = isCancellable,
                confirmValueChange = confirmValueChange
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Stable
class BottomSheetStateV3(
    initialValue: BottomSheetValueV3,
    val skipHalfExpanded: Boolean,
    val isCancellable: Boolean,
    private val confirmValueChange: (BottomSheetValueV3) -> Boolean = { isCancellable }
) {

    val draggableState = AnchoredDraggableState(
        initialValue = initialValue,
        positionalThreshold = { totalDistance -> totalDistance * 0.5f },
        velocityThreshold = { 500f },  // Καθορίζει την ταχύτητα για το σύρσιμο
        snapAnimationSpec = tween(),  // Η κινούμενη εικόνα
        decayAnimationSpec = exponentialDecay(),  // Decay animation
        confirmValueChange = confirmValueChange  // Λειτουργία που επιβεβαιώνει την αλλαγή της τιμής
    )
    val showSize = mutableFloatStateOf(0f)

    val layoutHeight = mutableIntStateOf(0)
    val sheetHeight = mutableIntStateOf(0)

    /**
     * The current value of the [BottomSheetStateV3].
     */
    val currentValue: BottomSheetValueV3
        get() = draggableState.currentValue

    val targetValue: BottomSheetValueV3
        get() = draggableState.targetValue

    /**
     * Whether the bottom sheet is visible.
     */
    val isVisible: Boolean
        get() = currentValue != BottomSheetValueV3.Hidden

    val isVisibleReal: Boolean
        get() = showSize.floatValue > 0

    /**
     * Whether the bottom sheet is expanded.
     */
    val isExpanded: Boolean
        get() = currentValue == BottomSheetValueV3.Expanded

    /**
     * Whether the bottom sheet is half expanded.
     */
    val isHalfExpanded: Boolean
        get() = currentValue == BottomSheetValueV3.HalfExpanded

    /**
     * Whether the bottom sheet is hidden.
     */
    val isHidden: Boolean
        get() = currentValue == BottomSheetValueV3.Hidden

    private val hasHalfExpandedState: Boolean
        get() = draggableState.anchors.hasAnchorFor(BottomSheetValueV3.HalfExpanded)

    /**
     * Show the bottom sheet with animation and suspend until it's shown.
     * If the sheet is taller than 50% of the parent's height, the bottom sheet will be half expanded.
     * Otherwise, it will be fully expanded.
     */
    suspend fun show() {
        val targetValue = if (skipHalfExpanded) {
            BottomSheetValueV3.Expanded
        } else {
            if (hasHalfExpandedState) BottomSheetValueV3.HalfExpanded
            else BottomSheetValueV3.Expanded
        }
        animateTo(targetValue)
    }

    /**
     * Expand the bottom sheet with an animation and suspend until the animation finishes or is cancelled.
     */
    suspend fun expand() {
        if (draggableState.anchors.hasAnchorFor(BottomSheetValueV3.Expanded)) {
            animateTo(BottomSheetValueV3.Expanded)
        }
    }

    /**
     * Half expand the bottom sheet with an animation and suspend until the animation finishes or is cancelled.
     */
    suspend fun halfExpand() {
        if (draggableState.anchors.hasAnchorFor(BottomSheetValueV3.HalfExpanded)) {
            animateTo(BottomSheetValueV3.HalfExpanded)
        }
    }

    /**
     * Hide the bottom sheet with an animation and suspend until the animation finishes or is cancelled.
     */
    suspend fun hide() {
        animateTo(BottomSheetValueV3.Hidden)
    }

    fun requireOffset(): Int {
        val offset = try {
            draggableState.requireOffset()
        } catch (_: Exception) {
            0f
        }
        showSize.floatValue = layoutHeight.intValue - offset
        return offset.roundToInt()
    }

    fun updateAnchors() {
        val newAnchors = DraggableAnchors {
            // Προσθήκη anchor για την κατάσταση "Hidden"
            BottomSheetValueV3.Hidden at layoutHeight.intValue.toFloat()

            // Αν δεν παρακάμπτουμε το HalfExpanded, προσθέτουμε anchor και για αυτή την κατάσταση
            if (!skipHalfExpanded) {
                BottomSheetValueV3.HalfExpanded at (layoutHeight.intValue * 0.5f)
            }

            // Προσθήκη anchor για την κατάσταση "Expanded"
            BottomSheetValueV3.Expanded at (layoutHeight.intValue - sheetHeight.intValue.toFloat())
        }
        draggableState.updateAnchors(newAnchors)
    }

    private suspend fun animateTo(
        targetValue: BottomSheetValueV3,
        velocity: Float = draggableState.lastVelocity
    ) = draggableState.animateTo(targetValue)

    companion object {
        /**
         * The default [Saver] implementation for [BottomSheetStateV3].
         */
        fun Saver(
            isCancellable: Boolean,
            skipHalfExpanded: Boolean,
            confirmValueChange: (BottomSheetValueV3) -> Boolean
        ): Saver<BottomSheetStateV3, BottomSheetValueV3> =
            Saver(
                save = { it.currentValue },
                restore = {
                    BottomSheetStateV3(
                        initialValue = it,
                        skipHalfExpanded = skipHalfExpanded,
                        isCancellable = isCancellable,
                        confirmValueChange = confirmValueChange
                    )
                }
            )
    }
}

enum class BottomSheetValueV3 {
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
    val scope = rememberCoroutineScope()
    val bottomSheetNestedScrollConnection = remember(state.draggableState) {
        consumeSwipeWithinBottomSheetBoundsNestedScrollConnection(
            state = state.draggableState
        )
    }
    val scrim by animateColorAsState(if (state.targetValue != BottomSheetValueV3.Hidden && state.sheetHeight.intValue > 0) scrimColor else Color.Transparent)

    LaunchedEffect(state.layoutHeight.intValue, state.sheetHeight.intValue) {
        if (state.layoutHeight.intValue > 0 && state.sheetHeight.intValue > 0) {
            state.updateAnchors()
        }
    }
    if (state.targetValue != BottomSheetValueV3.Hidden || state.isVisibleReal) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrim)
                .applyIf(state.isVisible && state.isCancellable) {
                    pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            scope.launch {
                                state.hide()
                            }
                        })
                    }
                }
                .onSizeChanged {
                    state.layoutHeight.intValue = it.height
                }
                .offset {
                    val yOffset = state.requireOffset()
                    IntOffset(x = 0, y = yOffset)
                }
                .anchoredDraggable(
                    state = state.draggableState,
                    orientation = Orientation.Vertical,
                    enabled = state.isCancellable
                )
                .nestedScroll(
                    if (state.isCancellable) bottomSheetNestedScrollConnection
                    else {
                        object : NestedScrollConnection {

                        }
                    }
                )
                .then(modifier)
        ) {
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .Tap { }
                    .onSizeChanged {
                        state.sheetHeight.intValue = it.height
                    }
            ) {
                if (state.isVisibleReal || state.sheetHeight.intValue == 0) {
                    BackPressHandler {
                        if (state.isCancellable) state.hide()
                    }
                    content()
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun consumeSwipeWithinBottomSheetBoundsNestedScrollConnection(
    state: AnchoredDraggableState<BottomSheetValueV3>
): NestedScrollConnection = object : NestedScrollConnection {

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val delta = available.offsetToFloat()
        return if (delta < 0 && source == NestedScrollSource.UserInput) {
            state.dispatchRawDelta(delta).toOffset()
        } else {
            Offset.Zero
        }
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        val delta = available.offsetToFloat()
        return if (source == NestedScrollSource.UserInput) {
            state.dispatchRawDelta(delta).toOffset()
        } else {
            Offset.Zero
        }
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        val toFling = available.velocityToFloat()
        val currentOffset = state.requireOffset()
        return if (toFling < 0 && currentOffset > state.anchors.minAnchor()) {
            state.settle(toFling)
            available
        } else {
            Velocity.Zero
        }
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        val toFling = available.velocityToFloat()
        state.settle(toFling)
        return available
    }


    private fun Offset.offsetToFloat(): Float = y

    private fun Float.toOffset(): Offset = Offset(
        x = 0f,
        y = this
    )

    private fun Velocity.velocityToFloat() = y
}