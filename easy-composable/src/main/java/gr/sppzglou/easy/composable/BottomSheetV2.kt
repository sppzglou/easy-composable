package gr.sppzglou.easy.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetDefaults
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

val LocalAppBottomSheet = staticCompositionLocalOf { BottomSheet() }

data class Sheet(
    val state: ModalBottomSheetState,
    val modifier: Modifier,
    val scrimColor: Color,
    val defaultStyle: Boolean,
    val closeOnBack: Boolean,
    val content: (@Composable () -> Unit)
) {
    override fun equals(other: Any?): Boolean {
        return other is Sheet && other.state == this.state
    }

    override fun hashCode(): Int {
        return state.hashCode()
    }
}

data class BottomSheet(
    var sheets: MutableList<Sheet> = mutableStateListOf()
) {
    fun addSheet(sheets: Sheet) {
        this.sheets.add(sheets)
    }

    fun removeSheet(sheets: Sheet) {
        this.sheets.removeAt(this.sheets.indexOf(sheets))
    }
}

@Composable
fun BottomSheet(
    state: ModalBottomSheetState,
    modifier: Modifier = Modifier,
    scrimColor: Color = ModalBottomSheetDefaults.scrimColor,
    defaultStyle: Boolean = true,
    closeOnBack: Boolean = true,
    sheetContent: @Composable () -> Unit
) {
    val screenSheets = LocalAppBottomSheet.current

    DisposableEffect(state) {
        screenSheets.addSheet(
            Sheet(
                state,
                modifier,
                scrimColor,
                defaultStyle,
                closeOnBack,
                sheetContent
            )
        )

        onDispose {
            screenSheets.removeSheet(
                Sheet(
                    state,
                    modifier,
                    scrimColor,
                    defaultStyle,
                    closeOnBack,
                    sheetContent
                )
            )
        }
    }
}

@Composable
fun InitBottomSheet(content: @Composable () -> Unit) {
    val sheet by rem(BottomSheet())

    CompositionLocalProvider(
        LocalAppBottomSheet provides sheet
    ) {
        MainBottomSheet(sheet, content) {
            BackPressHandler {
                it()
            }
        }
    }
}

@Composable
private fun MainBottomSheet(
    sheet: BottomSheet,
    content: @Composable () -> Unit,
    onBack: @Composable (suspend () -> Unit) -> Unit
) {
    val defaultBg = MaterialTheme.colors.surface
    Box(Modifier.fillMaxSize()) {
        content()

        sheet.sheets.forEachIndexed { i, it ->
            ModalBottomSheetLayout(
                {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(15.dp, 15.dp))
                            .applyIf(it.defaultStyle) {
                                it
                                    .background(defaultBg)
                                    .navigationBarsPadding()
                                    .padding(10.dp)
                            }
                            .then(it.modifier)
                    ) {
                        var size by rem(DpSize.Zero)

                        if (size == DpSize.Zero || it.state.targetValue != ModalBottomSheetValue.Hidden || it.state.isVisible) {
                            Box(Modifier.onSizeChanged {
                                size = DpSize(it.width.toDp.dp, it.height.toDp.dp)
                            }) {
                                it.content()
                            }

                            onBack {
                                if (it.closeOnBack) it.state.hide()
                            }
                        } else {
                            Box(Modifier.size(size))
                        }
                    }
                },
                Modifier.zIndex(1f),
                sheetElevation = 0.dp,
                sheetState = it.state,
                scrimColor = it.scrimColor,
                sheetBackgroundColor = Color.Transparent,
                sheetGesturesEnabled = it.closeOnBack
            ) {}
        }
    }
}

@Composable
fun rememberBottomSheetState(
    initialValue: ModalBottomSheetValue,
    skipHalfExpanded: Boolean = false,
    isCancellable: Boolean = true,
): ModalBottomSheetState {
    var confirmValueChange by rememberSaveable { mutableStateOf(isCancellable) }
    val state =
        rememberModalBottomSheetState(
            initialValue,
            confirmValueChange = {
                confirmValueChange
            },
            skipHalfExpanded = skipHalfExpanded
        )

    if (!isCancellable) {
        LaunchedEffect(state.currentValue) {
            confirmValueChange = state.currentValue == ModalBottomSheetValue.Hidden
        }
    }
    return state
}

fun Modifier.applyIf(bool: Boolean, block: Modifier.(Modifier) -> Modifier): Modifier {
    return if (bool) this.then(block(this))
    else this
}