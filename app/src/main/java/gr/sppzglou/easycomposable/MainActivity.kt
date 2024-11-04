package gr.sppzglou.easycomposable

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import gr.sppzglou.easy.composable.BottomSheet
import gr.sppzglou.easy.composable.BottomSheetStateV4
import gr.sppzglou.easy.composable.BottomSheetValues
import gr.sppzglou.easy.composable.Launch
import gr.sppzglou.easy.composable.SpacerV
import gr.sppzglou.easy.composable.rem
import gr.sppzglou.easy.composable.rememberBottomSheetState
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state: BottomSheetStateV4 =
                rememberBottomSheetState(BottomSheetValues.Hidden, skipHalfExpanded = false)
            val scope = rememberCoroutineScope()

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
                    var list by rem(0..0)
//                    LazyColumn {
//                        items(100) {
//                            Text("$it", Modifier.fillMaxWidth())
//                        }
//                    }

                    Launch {
                        //delay(2000)
                        list = 0..20
                    }

                    Column {
                        //Text("SKATA!!", Modifier.fillMaxWidth(), fontSize = 30.sp, color = Color.White)
                        SpacerV(190.dp)
                        list.forEach {
                            Text("$it", Modifier.fillMaxWidth())
                        }
//                        LazyColumn {
//                            items(100) {
//                                Text("$it", Modifier.fillMaxWidth())
//                            }
//                        }
                    }
                }
            }
        }
    }
}