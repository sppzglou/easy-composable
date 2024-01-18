package gr.sppzglou.easy.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SnackBar(hasError: MutableState<Boolean>, errorMessage: MutableState<String>, color: Color) {
    val haptic = LocalHapticFeedback.current
    var scope = rememberCoroutineScope()
    if (hasError.value)
        scope = rememberCoroutineScope()

    val alpha by valueAnimation(if (hasError.value) 1f else 0f) {
        if (it == 1f) {
            scope.launch {
                delay(3000)
                hasError.value = false
            }
        }
    }

    if (alpha > 0) {
        Launch {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        Column(
            Modifier
                .alpha(alpha)
                .zIndex(100f)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            color,
                            color,
                            color,
                            Color.Transparent
                        )
                    )
                )
                .Tap {
                    scope.cancel()
                    hasError.value = false
                }
                .padding(horizontal = 30.dp, vertical = 20.dp)
                .padding(bottom = 20.dp)
        ) {
            Text(
                errorMessage.value,
                Modifier.statusBarsPadding(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}