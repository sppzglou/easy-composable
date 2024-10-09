package gr.sppzglou.easy.composable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ModalBottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val LocalAppBottomSheet = staticCompositionLocalOf { BottomSheet() }

data class Sheet(
    val state: BottomSheetStateV2,
    val skipHalfExpanded: Boolean,
    val modifier: Modifier,
    val scrimColor: Color,
    val defaultStyle: Boolean,
    val isCancellable: Boolean,
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
    modifier: Modifier = Modifier,
    state: BottomSheetStateV2,
    skipHalfExpanded: Boolean = false,
    scrimColor: Color = ModalBottomSheetDefaults.scrimColor,
    defaultStyle: Boolean = true,
    isCancellable: Boolean = true,
    sheetContent: @Composable () -> Unit
) {
    val screenSheets = LocalAppBottomSheet.current

    DisposableEffect(state) {
        screenSheets.addSheet(
            Sheet(
                state,
                skipHalfExpanded,
                modifier,
                scrimColor,
                defaultStyle,
                isCancellable,
                sheetContent
            )
        )

        onDispose {
            screenSheets.removeSheet(
                Sheet(
                    state,
                    skipHalfExpanded,
                    modifier,
                    scrimColor,
                    defaultStyle,
                    isCancellable,
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
        MainBottomSheet(sheet, content)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainBottomSheet(
    sheet: BottomSheet,
    content: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        content()

        sheet.sheets.forEachIndexed { i, sheet ->
            val isVisible by remember(sheet.state.isVisible) {
                mutableStateOf(sheet.state.isVisible)
            }
            val sheetState =
                androidx.compose.material3.rememberModalBottomSheetState(sheet.skipHalfExpanded,
                    confirmValueChange = {
                        sheet.isCancellable
                    })

            if (isVisible) {
                ModalBottomSheet(
                    onDismissRequest = {
                        sheet.state.hide()
                    },
                    sheetState = sheetState,
                    scrimColor = sheet.scrimColor,
                    containerColor = Color.Transparent,
                    contentWindowInsets = { WindowInsets.ime },
                    shape = RoundedCornerShape(15.dp, 15.dp, 0.dp, 0.dp),
                    properties = ModalBottomSheetProperties(shouldDismissOnBackPress = sheet.isCancellable)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(15.dp, 15.dp))
                            .then(sheet.modifier)
                    ) {
                        sheet.content()
                    }
                }
            }
//            ModalBottomSheetLayout(
//                {
//                    Box(
//                        Modifier
//                            .fillMaxWidth()
//                            .clip(RoundedCornerShape(15.dp, 15.dp))
//                            .then(it.modifier)
//                            .applyIf(it.defaultStyle) {
//                                it
//                                    .background(defaultBg)
//                                    .navigationBarsPadding()
//                                    .padding(10.dp)
//                            }
//                    ) {
//                        var size by rem(DpSize.Zero)
//
//                        if (size == DpSize.Zero || it.state.targetValue != ModalBottomSheetValue.Hidden || it.state.isVisible) {
//                            Box(Modifier.onSizeChanged {
//                                size = DpSize(it.width.toDp.dp, it.height.toDp.dp)
//                            }) {
//                                it.content()
//                            }
//
//                            onBack {
//                                if (it.closeOnBack) it.state.hide()
//                            }
//                        } else {
//                            Box(Modifier.size(size))
//                        }
//                    }
//                },
//                Modifier.zIndex(1f),
//                sheetElevation = 0.dp,
//                sheetState = it.state,
//                scrimColor = it.scrimColor,
//                sheetBackgroundColor = Color.Transparent,
//                sheetGesturesEnabled = it.closeOnBack
//            ) {}
        }
    }
}


@Composable
fun rememberBottomSheetState(
    initialValue: Boolean = false
): BottomSheetStateV2 {
    return rememberSaveable(
        saver = BottomSheetStateV2.Saver
    ) {
        BottomSheetStateV2(initialValue)
    }
}

@Stable
class BottomSheetStateV2(
    initialValue: Boolean
) {
    var isVisible by mutableStateOf(initialValue)

    fun show() {
        isVisible = true
    }

    fun hide() {
        isVisible = false
    }

    companion object {
        val Saver: Saver<BottomSheetStateV2, Boolean> = Saver(
            save = { it.isVisible },
            restore = { BottomSheetStateV2(it) }
        )
    }

}

fun Modifier.applyIf(bool: Boolean, block: Modifier.(Modifier) -> Modifier): Modifier {
    return if (bool) this.then(block(this))
    else this
}