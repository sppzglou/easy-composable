package gr.sppzglou.easycomposable

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gr.sppzglou.easy.composable.BottomSheet
import gr.sppzglou.easy.composable.InitBottomSheet
import gr.sppzglou.easy.composable.SpacerV
import gr.sppzglou.easy.composable.rememberBottomSheetState
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val scope = rememberCoroutineScope()
            InitBottomSheet {
                val sheet =
                    rememberBottomSheetState(ModalBottomSheetValue.Hidden, false, false)


                Button(onClick = {
                    scope.launch {
                        sheet.show()
                    }
                }) {
                    Text("Click me!")
                }

                BottomSheet(sheet, defaultStyle = false) {
                    Column(
                        Modifier
                            .navigationBarsPadding()
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 50.dp)
                            .shadow(5.dp, RoundedCornerShape(10.dp))
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Gray)
                            .padding(10.dp),
                        Arrangement.Center, Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Please wait...",
                            color = Color.Black,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        SpacerV(50.dp)
                        CircularProgressIndicator()
                        Button(onClick = {
                            scope.launch {
                                sheet.hide()
                            }
                        }) {
                            Text("Click me!")
                        }
                        SpacerV(50.dp)
                    }
                }
            }
        }
    }
}