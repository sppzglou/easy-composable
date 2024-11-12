package gr.sppzglou.easycomposable

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ModalBottomSheetDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import gr.sppzglou.easy.composable.BottomSheet
import gr.sppzglou.easy.composable.BottomSheetStateV3
import gr.sppzglou.easy.composable.CameraView
import gr.sppzglou.easy.composable.InitBottomSheet
import gr.sppzglou.easy.composable.Launch
import gr.sppzglou.easy.composable.SheetValues
import gr.sppzglou.easy.composable.applyIf
import gr.sppzglou.easy.composable.rememberBottomSheetState
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            /*val state: BottomSheetStateV3 =
                rememberBottomSheetState(
                    SheetValues.Hidden,
                    skipHalfExpanded = false
                )

            Box(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Button(onClick = {
                        scope.launch {
                            state.show()
                        }
                    }) {
                        Text("${state.isVisible} ${state.isVisibleReal}")
                    }
                }

                BottomSheet(state) {
                    var list by rem(0..50)
//                    LazyColumn {
//                        items(100) {
//                            Text("$it", Modifier.fillMaxWidth())
//                        }
//                    }

                    Launch {
//                        delay(2000)
//                        list = 0..20
//                        delay(5000)
//                        list = 0..0
                    }

                    Column {
//                        Text(
//                            "SKATA!!",
//                            Modifier.fillMaxWidth(),
//                            fontSize = 30.sp,
//                            color = Color.White
//                        )
//                        SpacerV(190.dp)
                        list.forEach {
                            Text("$it", Modifier.fillMaxWidth())
                        }
                        LazyColumn {
                            items(100) {
                                Text("$it", Modifier.fillMaxWidth())
                            }
                            item {
                                Spacer(Modifier.navigationBarsPadding())
                            }
                        }
                    }
                }
            }*/

            InitBottomSheet {
                val scope = rememberCoroutineScope()

                val permissionsLauncher =
                    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}


                Launch {
                    val list = mutableListOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO
                    )
                    permissionsLauncher.launch(list.toTypedArray())
                }

                val camera = cameraView("TEST") { file, error ->
                    file?.let {

                    }
                }

                Button(onClick = {
                    scope.launch {
                        camera.show()
                    }
                }, modifier = Modifier.padding(50.dp)) {
                    Text("click")
                }
            }

        }
    }
}

@Composable
fun cameraView(
    label: String,
    handler: (f: File?, error: Exception?) -> Unit
): BottomSheetStateV3 {
    val scope = rememberCoroutineScope()
    val state =
        rememberBottomSheetState(SheetValues.Hidden, skipHalfExpanded = true, isCancellable = false)

    val perfix by remember(label) {
        mutableStateOf("${label}_%s")
    }

    BottomSheetApp(state, Modifier.zIndex(1f)) {
        Box(Modifier.statusBarsPadding()) {
            CameraView(
                {
                    CircleBtn("back") { state.hide() }
                },
                { torch, block ->
                    CircleBtn(
                        if (torch) "on"
                        else "off"
                    ) {
                        block()
                    }
                },
                {
                    Box(
                        Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.Red)
                            .clickable {
                                scope.launch {
                                    it()
                                }
                            })
                },
                { CircleBtn("ok") { it() } },
                { CircleBtn("cancel") { it() } },
                { state.hide() },
                { CircularProgressIndicator() },
                5.dp, 20.dp, Color.Green, Color.Red, null,
                perfix,
                true,
                false,
                handler
            )
        }
    }

    return state
}

@Composable
fun BottomSheetApp(
    state: BottomSheetStateV3,
    modifier: Modifier = Modifier,
    scrimColor: Color = ModalBottomSheetDefaults.scrimColor,
    defaultStyle: Boolean = true,
    sheetContent: @Composable () -> Unit
) {
    BottomSheet(
        modifier,
        state,
        scrimColor
    ) {
        Box(
            modifier
                .fillMaxWidth()
                .applyIf(defaultStyle) {
                    it
                        .clip(RoundedCornerShape(15.dp, 15.dp))
                        .background(Color.Gray)
                }) {
            sheetContent()
        }
    }
}

@Composable
fun CircleBtn(
    txt: String,
    listener: suspend () -> Unit
) {
    val scope = rememberCoroutineScope()
    Button(onClick = {
        scope.launch {
            listener()
        }
    }) {
        Text(txt)
    }
}