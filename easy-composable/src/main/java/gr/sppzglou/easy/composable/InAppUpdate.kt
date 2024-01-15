package gr.sppzglou.easy.composable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.ktx.AppUpdateResult
import com.google.android.play.core.ktx.requestUpdateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

const val APP_UPDATE_REQUEST_CODE = 86500

@Composable
fun rememberInAppUpdateState(onError: (Throwable) -> Unit): InAppUpdateState {
    return rememberMutableInAppUpdateState(onError)
}


internal val LocalAppUpdateManager = staticCompositionLocalOf<AppUpdateManager?> {
    null
}

interface InAppUpdateState {
    val result: AppUpdateResult
}

@Composable
internal fun rememberMutableInAppUpdateState(onError: (Throwable) -> Unit): InAppUpdateState {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appUpdateManager = LocalAppUpdateManager.current ?: AppUpdateManagerFactory.create(context)

    val inAppUpdateState = remember {
        MutableInAppUpdateState(appUpdateManager, scope, onError)
    }

    return inAppUpdateState
}

internal class MutableInAppUpdateState(
    private val appUpdateManager: AppUpdateManager,
    scope: CoroutineScope,
    onError: (Throwable) -> Unit
) : InAppUpdateState {

    private var _appUpdateResult by mutableStateOf<AppUpdateResult>(AppUpdateResult.NotAvailable)

    override val result: AppUpdateResult
        get() = _appUpdateResult

    init {
        scope.launch {
            appUpdateManager.requestUpdateFlow()
            appUpdateManager.requestUpdateFlow().catch { e ->
                onError(e)
            }.collect {
                _appUpdateResult = it
            }
        }
    }
}

@Composable
fun FragmentActivity.RequireLatestVersion(
    isUpdateEnabled: Boolean,
    updateType: Int = 1, // 1:FlexibleUpdate, 2:ImmediateUpdate
    inError: (String) -> Unit,
    progressView: @Composable () -> Unit,
    content: @Composable () -> Unit
) {

    if (isUpdateEnabled) {
        val scope = rememberCoroutineScope()
        val inAppUpdateState = rememberInAppUpdateState {
            scope.launch {
                inError(it.message ?: it.localizedMessage)
            }
        }

        when (val state = inAppUpdateState.result) {
            is AppUpdateResult.NotAvailable -> content()
            is AppUpdateResult.Available -> {
                LaunchedEffect(updateType) {
                    when (updateType) {
                        1 -> state.startFlexibleUpdate(
                            this@RequireLatestVersion,
                            APP_UPDATE_REQUEST_CODE
                        )

                        2 -> state.startImmediateUpdate(
                            this@RequireLatestVersion,
                            APP_UPDATE_REQUEST_CODE
                        )
                    }
                }
                if (updateType != 2) {
                    content()
                }
            }

            is AppUpdateResult.InProgress -> {
                progressView()
            }

            is AppUpdateResult.Downloaded -> {
                Launch {
                    scope.launch {
                        state.completeUpdate()
                        this@RequireLatestVersion.recreate()
                    }
                }
            }
        }
    } else {
        content()
    }
}
