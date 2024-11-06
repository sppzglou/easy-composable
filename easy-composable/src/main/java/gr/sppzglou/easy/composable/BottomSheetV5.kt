package gr.sppzglou.easy.composable

import androidx.compose.animation.SplineBasedFloatDecayAnimationSpec
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.generateDecayAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

enum class SheetValue {
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
fun Test(
    modifier: Modifier = Modifier.background(
        Color.Gray,
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ),
    BottomSheetContent: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val state = remember {
        AnchoredDraggableState(
            initialValue = SheetValue.Hidden,
            snapAnimationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            positionalThreshold = { totalDistance -> totalDistance * 0.5f },
            velocityThreshold = { 50.dpToPx.toFloat() },
            decayAnimationSpec = SplineBasedFloatDecayAnimationSpec(density).generateDecayAnimationSpec()
        )
    }
    val nestedScrollConnection = remember(state) {
        BottomSheetNestedScrollConnection(
            state = state,
            onFling = { velocity ->
                scope.launch { state.settle(velocity) }
            }
        )
    }

    var showHeight by rem(0)
    var sheetHeight by rem(0)
    var layoutHeight by rem(0)

    LaunchedEffect(layoutHeight) {
        println("layoutHeight: $layoutHeight")
    }
    Box(
        Modifier
            .onSizeChanged {
                layoutHeight = it.height
            }
            .fillMaxSize()
            .background(Color.Red)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height((2670 * 0.5).toDp.dp)
                .background(Color.Blue)
        )
        Button(modifier = Modifier.padding(top = 100.dp), onClick = {
            scope.launch {
                state.animateTo(SheetValue.HalfExpanded)
            }
        }) {
            Text("Click")
        }

        if (layoutHeight != 0) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    // --(3)--
                    .nestedScroll(nestedScrollConnection)
                    .offset {
                        val sheetOffsetY = state
                            .requireOffset()
                            .toInt()
                        showHeight = sheetHeight - sheetOffsetY
                        IntOffset(x = 0, y = sheetOffsetY)
                    }
                    .anchoredDraggable(state, Orientation.Vertical)
                    .onSizeChanged { sheetSize ->
                        sheetHeight = sheetSize.height

                        fun calcDragEndPoint(sheetValue: SheetValue): Float {
                            val dragPoint =
                                sheetHeight - (layoutHeight * sheetValue.draggableSpaceFraction)
                            return dragPoint.coerceAtLeast(0f) // Διασφάλιση ότι το αποτέλεσμα δεν είναι αρνητικό
                        }

                        val newAnchors = DraggableAnchors {
                            SheetValue.Hidden at calcDragEndPoint(SheetValue.Hidden) //2670
                            SheetValue.HalfExpanded at calcDragEndPoint(SheetValue.HalfExpanded) //1335
                            SheetValue.Expanded at calcDragEndPoint(SheetValue.Expanded)
                        }
                        state.updateAnchors(newAnchors, state.targetValue)
                    }
                    .then(modifier)
                    .heightIn(min = 50.dp)
                    .animateContentSize(),
                color = Color.Transparent
            ) {
                if (showHeight > 0) {
                    BottomSheetContent()
                }
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
internal fun BottomSheetNestedScrollConnection(
    state: AnchoredDraggableState<SheetValue>,
    onFling: (velocity: Float) -> Unit
): NestedScrollConnection =
    object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
            if (true) {
                val delta = available.offsetToFloat()
                if (delta < 0 && source == NestedScrollSource.UserInput) {
                    state.dispatchRawDelta(delta).toOffset()
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
        ): Offset = if (true) {
            if (source == NestedScrollSource.UserInput) {
                state.dispatchRawDelta(available.offsetToFloat()).toOffset()
            } else {
                Offset.Zero
            }
        } else super.onPostScroll(consumed, available, source)

        override suspend fun onPreFling(available: Velocity): Velocity = if (true) {
            val toFling = available.toFloat()
            val currentOffset = state.requireOffset()
            val minAnchor = state.anchors.minAnchor()

            if (toFling < 0 && currentOffset > minAnchor) {
                state.settle(toFling)
                onFling(toFling)
                available
            } else {
                Velocity.Zero
            }
        } else super.onPreFling(available)

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
            if (true) {
                onFling(available.toFloat())
                available
            } else super.onPostFling(consumed, available)

        private fun Float.toOffset(): Offset = Offset(
            x = 0f,
            y = this
        )

        private fun Velocity.toFloat() = y
        private fun Offset.offsetToFloat(): Float = y

    }