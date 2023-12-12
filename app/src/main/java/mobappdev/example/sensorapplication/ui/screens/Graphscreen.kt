package mobappdev.example.sensorapplication.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mobappdev.example.sensorapplication.ui.viewmodels.DataVM

@Composable
fun GraphScreen(vm: DataVM) {
    val state = vm.state.collectAsStateWithLifecycle().value

    // Assuming state contains timealg1list and angleFromAlg1list
   // val timeList = state.timealg1list
    val angleList = state.angleFromAlg1List

    // Your graph Composable goes here using timeList and angleList
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
       // LineChart(timeList, angleList)
    }
}

@Composable
fun LineChart(timeList: List<Long?>, angleList: List<Float>) {
    val maxValue = angleList.maxOrNull() ?: 0f

    Canvas(
        modifier = Modifier.fillMaxSize(),
        onDraw = {
            val chartWidth = size.width
            val chartHeight = size.height

            val xStep = if (timeList.isNotEmpty()) chartWidth / timeList.size else 0f
            val yStep = if (maxValue != 0f) chartHeight / maxValue else 0f

            // Draw horizontal axis
            drawLine(start = Offset(0f, chartHeight), end = Offset(chartWidth, chartHeight), color = Color.Black)

            // Draw vertical axis
            drawLine(start = Offset(0f, 0f), end = Offset(0f, chartHeight), color = Color.Black)

            // Draw line based on the data
            if (timeList.isNotEmpty() && angleList.isNotEmpty()) {
                val path = Path()
                path.moveTo(0f, chartHeight - angleList.first() * yStep)

                timeList.forEachIndexed { index, _ ->
                    val x = index * xStep
                    val y = chartHeight - angleList[index] * yStep
                    path.lineTo(x, y)
                }

                drawPath(path, color = Color.Blue, alpha = 0.8f)
            }
        }
    )
}

