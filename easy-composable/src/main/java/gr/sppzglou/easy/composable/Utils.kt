package gr.sppzglou.easy.composable

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.math.RoundingMode
import java.text.DecimalFormat

val Number.toDp: Float
    get() = (this.toFloat() / Resources.getSystem().displayMetrics.density)

fun safeDp(value: Dp): Dp {
    return if (value >= 0.dp) value
    else 0.dp
}

val String?.isNullOrEmptyOrBlank: Boolean
    get() = isNullOrEmpty() || this.isBlank()


val String?.isNotNullOrEmptyOrBlank: Boolean
    get() = !this.isNullOrBlank() && this.isNotEmpty()

fun Context.hideKeyboard() =
    (this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?)
        ?.hideSoftInputFromWindow((this as Activity).currentFocus?.windowToken, 0)

val Number.dpToPx: Int
    get() = (this.toFloat() * Resources.getSystem().displayMetrics.density).toInt()

val Int.dpToPx: Int
    get() = this.dp.value.dpToPx

fun decimalFormat(num: Number): String {
    val df = DecimalFormat("#.##")
    df.roundingMode = RoundingMode.DOWN
    return df.format(num)
}

fun Any?.tStr(old: String = "", new: String = "", nil: String = "") =
    this?.toString()?.replace(old, new) ?: nil

fun Int?.toBool() = this == 1

fun Boolean.toInt() = if (this) 1 else 0