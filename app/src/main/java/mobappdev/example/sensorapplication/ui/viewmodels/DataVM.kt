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

import android.os.Environment
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch


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
    ) { angle1, angle2 ->
        CombinedPolarSensorData.AngleData(angle1, angle2)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // internal data flow
    val combinedInternalDataFlow = combine(
        internalSensorController.intAngleFromAlg1,
        internalSensorController.intAngleFromAlg2,
    ) { intAngle1, intAngle2 ->
        internalSensorData.internalAngles(intAngle1 ?: 0.0f, intAngle2 ?: 0.0f)
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

    val state = combine(
        polarController.angleFromAlg1list,
        polarController.angleFromAlg2list,
        polarController.connected,
        internalSensorController.intAngleFromAlg1List,
        //internalSensorController.intAngleFromAlg2,

        _state
    ) { angleFromAlg1List, angleFromAlg2List, connected, intAngleFromAlg1List,
         state->
        state.copy(
            angleFromAlg1List = angleFromAlg1List,
            angleFromAlg2List = angleFromAlg2List,
            intAngleFromAlg1List = intAngleFromAlg1List,
           // intAngleFromAlg2List = intAngleFromAlg2List,

            connected = connected
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)



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
        createCSVString()
    }

    //this is only example. replace it with a list from the resulting angle
    private val elevationDataList = mutableListOf<Pair<Long, Float>>() // Example elevation data list

    fun createCSVString(): String {
        // logic to export and get the data to CSV format
        //create a CSV string representing the data and then save it to a file
        val csvStringBuilder = StringBuilder() //build CVS string

        // add header row:
        csvStringBuilder.append("Timestamp, Elevation\n")

        elevationDataList.forEach { (timestamp, elevation) ->
            csvStringBuilder.append("$timestamp, $elevation\n")
        }

        return csvStringBuilder.toString()
    }

    private fun saveCSVToFile(csvString: String) {
        try {
            val root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val fileName = "sensor_data_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"
            val file = File(root, fileName)

            FileWriter(file).use { writer ->
                writer.write(csvString)
            }

            // TODO: Notify the user that the export was successful, and provide the file path

        } catch (e: Exception) {
            e.printStackTrace()
            // TODO: notify the user of export failure?
        }
    }
    fun exportDataToCSV() {
        val csvString = createCSVString()
        saveCSVToFile(csvString)
    }


}

data class DataUiState(
    val hrList: List<Int> = emptyList(),
    val accelerationList: List<Triple<Float, Float, Float>?> = emptyList(), // Define the type of data in the list
    val angleFromAlg1List: List<Float> = emptyList(),
    val angleFromAlg2List: List<Float> = emptyList(),
    val intAngleFromAlg1List: List<Float> = emptyList(),
    val intAngleFromAlg2List: List<Float?> = emptyList(),
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
    data class AngleData(val angle1: Float?, val angle2: Float?) : CombinedPolarSensorData()
}

sealed class internalSensorData {
    data class internalAngles(val intAngle1: Float?, val intAngle2: Float?) : internalSensorData()
}



