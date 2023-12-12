package mobappdev.example.sensorapplication.ui.screens

/**
 * File: BluetoothDataScreen.kt
 * Purpose: Defines the UI of the data screen.
 * Author: Jitse van Esch
 * Created: 2023-07-08
 * Last modified: 2023-07-11
 */

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mobappdev.example.sensorapplication.ui.viewmodels.CombinedSensorData
import mobappdev.example.sensorapplication.ui.viewmodels.CombinedPolarSensorData
import mobappdev.example.sensorapplication.ui.viewmodels.DataVM
import mobappdev.example.sensorapplication.ui.viewmodels.internalSensorData
import androidx.compose.runtime.*


@Composable
fun BluetoothDataScreen(
    vm: DataVM
) {
    val state = vm.state.collectAsStateWithLifecycle().value
    val deviceId = vm.deviceId.collectAsStateWithLifecycle().value

    var polarConnected by remember { mutableStateOf<Boolean>(false) }
    var internalConnected by remember { mutableStateOf<Boolean>(false) }

    // Extract the values from the state objects
    //val angle1: Float? = angle1State.value
    //val angle2: Float? = angle2State.value

    val value: String = when {
        state.connected && state.measuring -> {
            // Connected case
            // Your existing logic based on CombinedSensorData
            when (val combinedPolarSensorData = vm.combinedPolarDataFlow.collectAsState().value) {
                is CombinedPolarSensorData.AngleData -> {
                    val angle1pol = combinedPolarSensorData.angle1
                    val angle2pol = combinedPolarSensorData.angle2
                    if (angle1pol == null || angle2pol == null) {
                        "-"
                    } else {
                        String.format("%.1f,%.1f", angle1pol, angle2pol)
                    }
                }
                else -> "-"
            }
        }

        state.connected && state.measuring -> {
            // Display internal sensor data when measuring
            when (val internalSensorData = vm.combinedInternalDataFlow.collectAsState().value) {
                is internalSensorData.internalAngles -> {
                    val intAngle1 = internalSensorData.intAngle1
                    val intAngle2 = internalSensorData.intAngle2
                    if (intAngle1 == null || intAngle2 == null) {
                        "-"
                    } else {
                        String.format(
                            "Internal Angle 1: %.1f\nInternal Angle 2: %.1f",
                            intAngle1,
                            intAngle2
                        )
                    }
                }

                else -> "-"
            }
        }

    else -> {
            // Not connected case
            // Define the string when not connected
            when (val combinedSensorData = vm.combinedDataFlow.collectAsState().value) {
                is CombinedSensorData.GyroData -> {
                    val triple = combinedSensorData.gyro
                    if (triple == null) {
                        "-"
                    } else {
                        String.format("%.1f, %.1f, %.1f", triple.first, triple.second, triple.third)
                    }
                }
                is CombinedSensorData.HrData -> combinedSensorData.hr.toString()
                is CombinedSensorData.AccelerometerData -> {
                    val accData = combinedSensorData.acc
                    if (accData == null) {
                        "-"
                    } else {
                        val accString = String.format("%.1f, %.1f, %.1f", accData.first, accData.second, accData.third)
                        val angleString = combinedSensorData.ang?.toString()
                        "$accString\nAngle: $angleString"
                    }
                }
                else -> "-"
            }
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = CenterHorizontally
    ) {
        Text(text = if (state.connected) "Polar sense connected" else "Polar sense disconnected")
        Box(
            contentAlignment = Center,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = if(state.measuring) value else "-",
                fontSize = if (value.length < 3) 128.sp else 40.sp,
                color = Color.Black,
            )
        }
        Text(text = "Select sensor:")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxWidth()
        ){
            Button(
                onClick = {vm.connectToSensor()
                    polarConnected = true
                    internalConnected = false
                    },
                enabled = !polarConnected,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = Color.Gray
                )
            ) {
                Text(text = "Polar sense")
            }
            Button(
                onClick = {vm.disconnectFromSensor()
                    internalConnected = true
                    polarConnected = false
                    },
                enabled = !internalConnected,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = Color.Gray
                )
            ) {
                Text(text = "Internal")
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxWidth()
        ){ Button(
                onClick = vm::startPolar,
                enabled = (state.connected && !state.measuring),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = Color.Gray
                )
            ) {
                Text(text = "Start Polar sensor Stream")
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxWidth()
        ){
            Button(
                onClick = vm::stopDataStream,
                enabled = (state.measuring),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = Color.Gray
                )
            ) {
                Text(text = "Stop\nstream")
            }
        }


        // new row for starting /stopping the internal sensor
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxWidth()
        ){
            Button(
                onClick = {
                    vm.startImuStream()
                },
                enabled = (!state.measuring),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = Color.Gray
                )
            ) {
                Text(text = "Start\nInternal Stream")
            }

        }

    }
}

