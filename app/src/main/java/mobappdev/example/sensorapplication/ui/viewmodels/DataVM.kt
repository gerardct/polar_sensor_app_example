package mobappdev.example.sensorapplication.ui.viewmodels

/**
 * File: DataVM.kt
 * Purpose: Defines the view model of the data screen.
 *          Uses Dagger-Hilt to inject a controller model
 * Author: Jitse van Esch
 * Created: 2023-07-08
 * Last modified: 2023-07-11
 */

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import mobappdev.example.sensorapplication.domain.InternalSensorController
import mobappdev.example.sensorapplication.domain.PolarController
import javax.inject.Inject

import kotlinx.coroutines.delay
import java.io.File
import java.io.FileWriter
import kotlinx.coroutines.launch
import java.io.IOException



@HiltViewModel
class DataVM @Inject constructor(
    private val polarController: PolarController,
    private val internalSensorController: InternalSensorController
): ViewModel() {

    private val gyroDataFlow = internalSensorController.currentGyroUI
    private val linAccDataFlow = internalSensorController.currentLinAccUI
    // private val angle 1
    // private val angle 2

    private val hrDataFlow = polarController.currentHR
    private val accDataFlow = polarController.currentAcceleration
    private val gyroDataFlowPolar = polarController.currentGyro
    private val ang1dataFlowPol = polarController.angleFromAlg1
    private val ang2dataFlowPol = polarController.angleFromAlg2





// Separant Polar i internal

    val combinedPolarDataFlow = combine(
        polarController.angleFromAlg1,
        polarController.angleFromAlg2,
        polarController.timealg1,
        polarController.timealg2
    ) { angle1, angle2, time1, time2 ->
        CombinedPolarSensorData.AngleData(angle1, angle2,time1,time2)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // internal data flow
    val combinedInternalDataFlow = combine(
        internalSensorController.intAngleFromAlg1,
        internalSensorController.intAngleFromAlg2,
    ) { intAngle1, intAngle2 ->
        internalSensorData.InternalAngles(intAngle1 ?: 0.0f, intAngle2 ?: 0.0f)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

//


    val combinedDataFlow = combine(
        gyroDataFlow,
        linAccDataFlow,
        hrDataFlow,
        accDataFlow,
        ang1dataFlowPol
    ) { gyro, linAcc, hr, acc, ang ->
        when {
            hr != null -> CombinedSensorData.HrData(hr)
            gyro != null -> CombinedSensorData.GyroData(gyro)
            acc != null -> CombinedSensorData.AccelerometerData(acc,ang)
            linAcc != null -> CombinedSensorData.InternalSensorData(linAcc)

            else -> null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _state = MutableStateFlow(DataUiState())

    val state = combine( // només agafa 5 variables
        polarController.angleFromAlg1list,
        polarController.angleFromAlg2list,
        polarController.timealg1list,
        polarController.connected,
        _state
    ) { angleFromAlg1List, angleFromAlg2List, time1list, connected,
        state->
        state.copy(
            angleFromAlg1List = angleFromAlg1List,
            angleFromAlg2List = angleFromAlg2List,
            timePolList = time1list,
            connected = connected
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    private val _internalState = MutableStateFlow(DataUiState())

    // todo: he intentat separar en un altre state: angles resultants son 0.
    val internalState = combine (
        internalSensorController.intAngleFromAlg1List,
        internalSensorController.intAngleFromAlg2List,
        internalSensorController.measuring,
        _internalState
    ) { intAngleFromAlg1List, intAngleFromAlg2List, measuring, state ->
        state.copy(
            intAngleFromAlg1List = intAngleFromAlg1List,
            intAngleFromAlg2List = intAngleFromAlg2List,
            measuring = measuring
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _internalState.value)




    private var streamType: StreamType? = null


    private val _deviceId = MutableStateFlow("")
    val deviceId: StateFlow<String>
        get() = _deviceId.asStateFlow()


    fun chooseSensor(deviceId: String) {
        _deviceId.update { deviceId }
    }

    fun connectToSensor() {
        polarController.connectToDevice(_deviceId.value)
    }

    fun disconnectFromSensor() {
        stopDataStream()
        polarController.disconnectFromDevice(_deviceId.value)
    }

    fun startHr() {
        polarController.startHrStreaming(_deviceId.value)
        streamType = StreamType.FOREIGN_HR
        _state.update { it.copy(measuring = true) }
    }
    fun startPolar(){
        polarController.startCombinedStreaming(_deviceId.value)
        streamType = StreamType.FOREIGN
        _state.update { it.copy(measuring = true) }
    }

    fun startGyro() {
        internalSensorController.startGyroStream()
        streamType = StreamType.LOCAL_GYRO

        _state.update { it.copy(measuring = true) }
    }


    fun stopDataStream() {
        when (streamType) {
            StreamType.LOCAL_GYRO -> internalSensorController.stopGyroStream()
            StreamType.LOCAL_ACC -> internalSensorController.stopImuStream()
            StreamType.FOREIGN_HR -> polarController.stopHrStreaming()
            StreamType.FOREIGN -> polarController.stopCombinedStreaming()

            else -> {} // Do nothing
        }
        _state.update { it.copy(measuring = false) }
    }


    fun startImuStream() {
        internalSensorController.startImuStream()
        streamType = StreamType.LOCAL_ACC
        _state.update { it.copy(measuring = true) }

    }

    fun stopImuStream() {
        when (streamType) {
            StreamType.LOCAL_ACC -> internalSensorController.stopImuStream()
            else -> {} // Do nothing
        }
        _state.update { it.copy(measuring = false) }
    }




    // this is for the recording of data:
    private var recordingStartedTimestamp: Long = 0
    private val recordingDuration = 30 * 1000L // duration: 30 seconds

    // todo: implement start Recroding function after pressing some button
    fun startRecording() {
        // start recording logic
        recordingStartedTimestamp = System.currentTimeMillis()
        _state.update { it.copy(measuring = true) }

        // Delay for the specified recording duration
        viewModelScope.launch {
            delay(recordingDuration)
            stopRecording()
        }
        // save measurements to a local database (different view to display the results)

    }


    fun stopRecording() {
        // stop recording logic
        recordingStartedTimestamp = 0
        _state.update { it.copy(measuring = false) }

        // Export data after stopping recording
        saveCSVToFile()
    }


    // function to save the CSV data to the file
    private fun saveCSVToFile() {
        val csvFile = createCsvFile()

        try {
            FileWriter(csvFile).use { writer ->
                // Write data to the CSV file
                val polarAlg1List = polarController.angleFromAlg1list.value
                val polarAlg2List = polarController.angleFromAlg2list.value
                val internalAlg1List = internalSensorController.intAngleFromAlg1List.value
                val internalAlg2List = internalSensorController.intAngleFromAlg2List.value
                val timeAlg1List = polarController.timealg1list.value
                val timeAlg2List = polarController.timealg2list.value
                val timeIntAlg1List = internalSensorController.timeIntalg1list.value
                val timeIntAlg2List = internalSensorController.timeIntalg2list.value

                // Write header to the CSV file
                writer.append("Timestamp, Polar Alg1, Polar Alg2, Internal Alg1, Internal Alg2, Time Alg1, Time Alg2, Time Int Alg1, Time Int Alg2\n")

                for (i in timeIntAlg1List.indices){
                    val timestamp = timeIntAlg1List[i]
                    val polarAlg1 = polarAlg1List.getOrNull(i) ?: 0.0
                    val polarAlg2 = polarAlg2List.getOrNull(i) ?: 0.0
                    val internalAlg1 = internalAlg1List.getOrNull(i) ?: 0.0
                    val internalAlg2 = internalAlg2List.getOrNull(i) ?: 0.0
                    val timeAlg1 = timeAlg1List.getOrNull(i) ?: 0L
                    val timeAlg2 = timeAlg2List.getOrNull(i) ?: 0L
                    val timeIntAlg1 = timeIntAlg1List[i] ?: 0L
                    val timeIntAlg2 = timeIntAlg2List[i] ?: 0L

                    writer.append("$timestamp, $polarAlg1, $polarAlg2, $internalAlg1, $internalAlg2, $timeAlg1, $timeAlg2, $timeIntAlg1, $timeIntAlg2\n")
                }
            }
            // File saved successfully
        } catch (e: IOException) {
            e.printStackTrace()
            // show error message if it didn't work
        }
    }

    private fun createCsvFile():
            File {
        val directory = File("/Users/claudiasegales/Desktop")  // directory to save the file
        if (!directory.exists()) {
            directory.mkdirs()
        }

        val fileName = "Lab1_3_sensor_data.csv"
        return File(directory, fileName)
    }


}

data class DataUiState(
   // val hrList: List<Int> = emptyList(),
    // val accelerationList: List<Triple<Float, Float, Float>?> = emptyList(), // Define the type of data in the list
    val angleFromAlg1List: List<Float> = emptyList(),
    val angleFromAlg2List: List<Float> = emptyList(),
    val timePolList: List<Long?> = emptyList(),
    val intAngleFromAlg1List: List<Float> = emptyList(),
    val intAngleFromAlg2List: List<Float> = emptyList(),
    val timeIntAlg1List: List<Long?> = emptyList(),
    val timeIntAlg2List: List<Long?> = emptyList(),
    val connected: Boolean = false,
    val measuring: Boolean = false
)




enum class StreamType {
    LOCAL_GYRO, LOCAL_ACC, FOREIGN_HR, FOREIGN_ACC, FOREIGN
}

sealed class CombinedSensorData {
    data class GyroData(val gyro: Triple<Float, Float, Float>?) : CombinedSensorData()
    data class HrData(val hr: Int?) : CombinedSensorData()
    data class AccelerometerData(val acc: Triple<Float, Float, Float>?,val ang: Float?) : CombinedSensorData()
    data class InternalSensorData(val linAcc: Triple<Float, Float, Float>?) : CombinedSensorData()//, val angle1: Float?, val angle2: Float?) : CombinedSensorData()

}
sealed class CombinedPolarSensorData {
    data class AngleData(val angle1: Float?, val angle2: Float?,val Time1: Long?,val Time2: Long?) : CombinedPolarSensorData()
}

sealed class internalSensorData {
    data class InternalAngles(val intAngle1: Float?, val intAngle2: Float?) : internalSensorData()
}




