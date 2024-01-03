package gr.sppzglou.easy.composable

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow

private inline fun SharedPreferences.edit(operation: (SharedPreferences.Editor) -> Unit) {
    val editor = edit()
    operation(editor)
    editor.apply()
}

operator fun SharedPreferences.set(key: String, value: Any?) {
    when (value) {
        is String? -> edit { it.putString(key, value) }
        is Int -> edit { it.putInt(key, value) }
        is Boolean -> edit { it.putBoolean(key, value) }
        is Float -> edit { it.putFloat(key, value) }
        is Long -> edit { it.putLong(key, value) }
        else -> throw UnsupportedOperationException("Access PreferencesHelper to implement this kind of operation")
    }
}

inline operator fun <reified T : Any> SharedPreferences.get(
    key: String,
    defaultValue: T
): T {
    return when (T::class) {
        String::class -> getString(key, defaultValue as String) as T
        Int::class -> getInt(key, defaultValue as Int) as T
        Boolean::class -> getBoolean(key, defaultValue as Boolean) as T
        Float::class -> getFloat(key, defaultValue as Float) as T
        Long::class -> getLong(key, defaultValue as Long) as T
        else -> throw UnsupportedOperationException("Access PreferencesHelper to implement this kind of operation")
    }
}

inline fun <reified T : Any> SharedPreferences.getFlowOf(key: String, defaultValue: T) =
    callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, k ->
            if (key == k) {
                trySend(this@getFlowOf[key, defaultValue])
            }
        }
        registerOnSharedPreferenceChangeListener(listener)
        if (contains(key)) {
            send(this@getFlowOf[key, defaultValue]) // if you want to emit an initial pre-existing value
        }
        awaitClose { unregisterOnSharedPreferenceChangeListener(listener) }
    }.buffer(Channel.UNLIMITED)


@Composable
inline fun <reified T : Any> SharedPreferences.collectFlowAsState(key: String, defaultValue: T) =
    getFlowOf(key, defaultValue).collectAsState(defaultValue)
