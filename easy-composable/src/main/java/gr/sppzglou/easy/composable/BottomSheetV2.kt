package gr.sppzglou.easy.composable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ModalBottomSheetDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

val LocalAppBottomSheet = staticCompositionLocalOf { BottomSheet() }

data class Sheet(
    val state: BottomSheetStateV3,
    val modifier: Modifier,
    val scrimColor: Color,
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
    state: BottomSheetStateV3,
    scrimColor: Color = ModalBottomSheetDefaults.scrimColor,
    sheetContent: @Composable () -> Unit
) {
    val screenSheets = LocalAppBottomSheet.current

    DisposableEffect(state) {
        screenSheets.addSheet(
            Sheet(
                state,
                modifier,
                scrimColor,
                sheetContent
            )
        )

        onDispose {
            screenSheets.removeSheet(
                Sheet(
                    state,
                    modifier,
                    scrimColor,
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

        Box(Modifier.fillMaxSize()) {
            content()

            sheet.sheets.forEachIndexed { i, sheet ->
                BottomSheet(
                    sheet.state,
                    sheet.modifier,
                    sheet.scrimColor,
                    sheet.content
                )
            }
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