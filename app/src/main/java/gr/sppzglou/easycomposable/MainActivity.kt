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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import gr.sppzglou.easy.composable.BottomSheet
import gr.sppzglou.easy.composable.BottomSheetStateV3
import gr.sppzglou.easy.composable.BottomSheetValueV3
import gr.sppzglou.easy.composable.rememberBottomSheetState
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state: BottomSheetStateV3 =
                rememberBottomSheetState(BottomSheetValueV3.Hidden, skipHalfExpanded = true)
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
                        Text("Show Bottom Sheet")
                    }
                }

                BottomSheet(state) {
//                    LazyColumn {
//                        items(100) {
//                            Text("$it", Modifier.fillMaxWidth())
//                        }
//                    }
                    Column {
                        (0..10).forEach {
                            Text("$it", Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}