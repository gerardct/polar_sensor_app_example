package mobappdev.example.sensorapplication.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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

    var dataPoints by remember { mutableStateOf(emptyList<Pair<Long, Float>>()) }
    var internalDataPoints by remember { mutableStateOf(emptyList<Pair<Long, Float>>()) }


    LaunchedEffect(Unit) {
        while (true) {
            val newDataPoint = when (val data = combinedPolarSensorData) {
                is CombinedPolarSensorData.AngleData -> {
                    Pair(System.currentTimeMillis(), data.angle1 ?: 0f) // Use angle1 or modify as needed
                }
                // Handle other types of data or default case if needed
                else -> Pair(System.currentTimeMillis(), 0f)
            }

            val newDataPointInternal = when (val internalData = combinedInternalSensorData) {
                is internalSensorData.InternalAngles -> {
                    Pair(System.currentTimeMillis(), internalData.intAngle1 ?: 0f) // Use angle1 or modify as needed
                }
                // Handle other types of internal data or default case if needed
                else -> Pair(System.currentTimeMillis(), 0f)
            }

            dataPoints = dataPoints + newDataPoint
            internalDataPoints = internalDataPoints + newDataPointInternal

            delay(5) // Wait for 0.005 second
        }
    }

    val angle = combinedPolarSensorData?.angle1 ?: 0f
    val time = combinedPolarSensorData?.time1 ?: 0L
    val internalAngle = combinedInternalSensorData?.intAngle1 ?: 0f
    val internalTime = combinedInternalSensorData?.timeInt1 ?: 0L

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Angle",
                    modifier = Modifier.padding(8.dp).align(Alignment.CenterHorizontally)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Time",
                    modifier = Modifier.padding(8.dp).align(Alignment.CenterHorizontally)
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$angle",
                    modifier = Modifier.padding(8.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$time",
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
        //LineChartWithTimeData(dataPoints = dataPoints, modifier = Modifier.fillMaxWidth().weight(1f))
        LineChartWithTimeData(
            dataPoints = dataPoints,
            internalDataPoints = internalDataPoints,
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
    }
}




@Composable
fun LineChartWithTimeData(dataPoints: List<Pair<Long, Float>>,  internalDataPoints: List<Pair<Long, Float>>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val maxY = 90f
        val minY = 0f
        val dataPointsCountPolar = dataPoints.size
        val dataPointsCountInternal = internalDataPoints.size

        val stepX = size.width / (dataPointsCountPolar - 1)
        val stepXint = size.width / (dataPointsCountInternal - 1)
        val stepY = size.height / (maxY - minY)


        val path = Path()
        if (dataPointsCountPolar > 0) {
            path.moveTo(
                ((dataPoints[0].first - dataPoints[0].first) / 10f) * stepX,
                size.height - ((dataPoints[0].second - minY) * stepY)
            )

            for (i in 1 until dataPointsCountPolar) {
                path.lineTo(
                    ((dataPoints[i].first - dataPoints[0].first) / 10f) * stepX,
                    size.height - ((dataPoints[i].second - minY) * stepY)
                )
            }
        }
        else if (dataPointsCountInternal > 0) {
            path.moveTo(
                ((dataPoints[0].first - dataPoints[0].first) / 10f) * stepXint,
                size.height - ((dataPoints[0].second - minY) * stepY)
            )
            for (i in 1 until dataPointsCountInternal) {
                path.lineTo(
                    ((dataPoints[i].first - dataPoints[0].first) / 10f) * stepXint,
                    size.height - ((dataPoints[i].second - minY) * stepY)
                )
            }
        }



        drawPath(path = path, color = Color.Blue, style = Stroke(width = 4.dp.toPx()))

    }
}





@Preview
@Composable
fun GraphScreenPreview() {
    val timeList = listOf(0L, 1L, 2L, 3L, 4L, 5L)
    val angleList1 = listOf(0f, 1f, 2f, 3f, 4f, 5f)
    val angleList2 = listOf(0f, 2f, 4f, 6f, 8f, 10f)
}

