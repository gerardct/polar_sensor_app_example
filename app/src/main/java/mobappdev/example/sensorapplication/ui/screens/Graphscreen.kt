package mobappdev.example.sensorapplication.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import mobappdev.example.sensorapplication.ui.viewmodels.CombinedPolarSensorData
import mobappdev.example.sensorapplication.ui.viewmodels.DataVM
import mobappdev.example.sensorapplication.ui.viewmodels.internalSensorData


@Composable
fun GraphScreen(vm: DataVM, navController: NavController) {
    val combinedPolarSensorDataFlow = vm.combinedPolarDataFlow
    val combinedInternalDataFlow = vm.combinedInternalDataFlow
    val combinedPolarSensorData by combinedPolarSensorDataFlow.collectAsState()
    val combinedInternalSensorData by combinedInternalDataFlow.collectAsState()
    val state = vm.state.collectAsStateWithLifecycle().value

    var dataPoints by remember { mutableStateOf(emptyList<Pair<Long, Float>>()) }
    var dataPoints2 by remember { mutableStateOf(emptyList<Pair<Long, Float>>()) }

    var internalDataPoints by remember { mutableStateOf(emptyList<Pair<Long, Float>>()) }
    var internalDataPoints2 by remember { mutableStateOf(emptyList<Pair<Long, Float>>()) }


    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < 20000) {
            val newDataPoint: Pair<Long, Float> = if (state.connected && state.measuring) {
                when (val data = combinedPolarSensorData) {
                    is CombinedPolarSensorData.AngleData -> {
                        Pair(
                            System.currentTimeMillis(),
                            data.angle1 ?: 0f
                        ) // Use angle1 or modify as needed
                    }

                    else -> Pair(0L, 0f)
                }
            } else {
                Pair(0L, 0f)
            }
            val newDataPoint2: Pair<Long, Float> = if (state.connected && state.measuring) {
                when (val data = combinedPolarSensorData) {
                    is CombinedPolarSensorData.AngleData -> {
                        Pair(
                            System.currentTimeMillis(),
                            data.angle2 ?: 0f
                        ) // Use angle1 or modify as needed
                    }

                    else -> Pair(0L, 0f)
                }
            } else {
                Pair(0L, 0f)
            }

            val newDataPointInternal: Pair<Long, Float> = if (!state.connected && state.measuring) {
                when (val internalData = combinedInternalSensorData) {
                    is internalSensorData.InternalAngles -> {
                        Pair(System.currentTimeMillis(), internalData.intAngle1 ?: 0f)
                        // Use intAngle1 or modify as needed
                    }

                    else -> Pair(0L, 0f)
                }
            } else {
                Pair(0L, 0f)
            }
            val newDataPointInternal2: Pair<Long, Float> =
                if (!state.connected && state.measuring) {
                    when (val internalData = combinedInternalSensorData) {
                        is internalSensorData.InternalAngles -> {
                            Pair(System.currentTimeMillis(), internalData.intAngle2 ?: 0f)
                            // Use intAngle1 or modify as needed
                        }

                        else -> Pair(0L, 0f)
                    }
                } else {
                    Pair(0L, 0f)
                }

            dataPoints = dataPoints + newDataPoint
            dataPoints2 = dataPoints2 + newDataPoint2

            internalDataPoints = internalDataPoints + newDataPointInternal
            internalDataPoints2 = internalDataPoints2 + newDataPointInternal2

            delay(5) // Wait for 0.005 second
        }
    }

    val angle = combinedPolarSensorData?.angle1 ?: 0f
    val angle2 = combinedPolarSensorData?.angle2 ?: 0f
    val time = combinedPolarSensorData?.time1 ?: 0L
    val internalAngle = combinedInternalSensorData?.intAngle1 ?: 0f
    val internalAngle2 = combinedInternalSensorData?.intAngle2 ?: 0f
    val internalTime = combinedInternalSensorData?.timeInt1 ?: 0L

    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = {
                navController.popBackStack()
                vm.stopRecording() // Navigate back to the previous screen
            },
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(text = "Back to Main Screen")
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Angle 1",
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Angle 2",
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }
            // Remove the Column for time text
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (state.connected) "$angle" else "$internalAngle",
                    modifier = Modifier.padding(8.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (state.connected) "$angle2" else "$internalAngle2",
                    modifier = Modifier.padding(8.dp)
                )
            }
            // Remove the Column for time text
        }
        Column(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.weight(1f)) {
                LineChartWithTimeData(
                    dataPoints = if (state.connected) dataPoints else internalDataPoints,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight() // Chart fills the available height evenly
                    // Adjust scaleX for wider display
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                LineChartWithTimeData(
                    dataPoints = if (state.connected) dataPoints2 else internalDataPoints2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight() // Chart fills the available height evenly
                    // Chart fills the entire width
                )
            }
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        vm.stopDataStream()
                        vm.stopRecording()
                    },
                    enabled = state.measuring,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier
                        .height(60.dp)
                        .width(140.dp)
                ) {
                    Text(text = "STOP", fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    vm.saveCSVToFile()
                },
                enabled = !state.measuring,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                ),
                modifier = Modifier
                    .height(40.dp)
                    .width(100.dp)
            ) {
                Text(text = "Export ", fontSize = 14.sp)
            }


        }
    }
}



    @Composable
    fun LineChartWithTimeData(dataPoints: List<Pair<Long, Float>>, modifier: Modifier = Modifier) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val maxY = 90f
            val minY = 0f
            val dataPointsCount = dataPoints.size

            val stepX = size.width / (dataPointsCount - 1)
            val stepY = size.height / (maxY - minY)

            val path = Path()
            if (dataPointsCount > 0) {
                path.moveTo(
                    ((dataPoints[0].first - dataPoints[0].first) / 20) * stepX,
                    size.height - ((dataPoints[0].second - minY) * stepY)
                )

                for (i in 1 until dataPointsCount) {
                    path.lineTo(
                        ((dataPoints[i].first - dataPoints[0].first) / 20) * stepX,
                        size.height - ((dataPoints[i].second - minY) * stepY)
                    )
                }
            }

            drawIntoCanvas { canvas ->
                val paint = Paint().asFrameworkPaint().apply {
                    color = Color.Black.toArgb()
                    textAlign = android.graphics.Paint.Align.LEFT
                    textSize = 16.sp.toPx()
                }

                canvas.nativeCanvas.drawText("0", 0f, size.height, paint)
                canvas.nativeCanvas.drawText("90", 0f, 16.dp.toPx(), paint)
            }

            drawPath(path = path, color = Color.Blue, style = Stroke(width = 4.dp.toPx()))
        }
    }





