package mobappdev.example.sensorapplication.data

/**
 * File: InternalSensorControllerImpl.kt
 * Purpose: Implementation of the Internal Sensor Controller.
 * Author: Jitse van Esch
 * Created: 2023-09-21
 * Last modified: 2023-09-21
 */

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
//import com.polar.sdk.api.model.PolarAccelerometerData
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mobappdev.example.sensorapplication.domain.InternalSensorController
import kotlin.math.absoluteValue
import kotlin.math.atan2
import kotlin.math.sqrt

private const val LOG_TAG = "Internal Sensor Controller"

class InternalSensorControllerImpl(
    private val context: Context
   // private val updateInternalSensorData: (Triple<Float, Float, Float>?) -> Unit
): InternalSensorController, SensorEventListener {

    // Expose acceleration to the UI
    private val _currentLinAccUI = MutableStateFlow<Triple<Float, Float, Float>?>(null)
    override val currentLinAccUI: StateFlow<Triple<Float, Float, Float>?>
        get() = _currentLinAccUI.asStateFlow()

    private var _currentGyro: Triple<Float, Float, Float>? = null

    // Expose gyro to the UI on a certain interval
    private val _currentGyroUI = MutableStateFlow<Triple<Float, Float, Float>?>(null)
    override val currentGyroUI: StateFlow<Triple<Float, Float, Float>?>
        get() = _currentGyroUI.asStateFlow()

    private val _streamingGyro = MutableStateFlow(false)
    override val streamingGyro: StateFlow<Boolean>
        get() = _streamingGyro.asStateFlow()

    private val _streamingLinAcc = MutableStateFlow(false)
    override val streamingLinAcc: StateFlow<Boolean>
        get() = _streamingLinAcc.asStateFlow()

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroSensor: Sensor? by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }


    private val _measuring = MutableStateFlow(false)
    override val measuring: StateFlow<Boolean>
        get() = _measuring.asStateFlow()

    private val _internalConnected = MutableStateFlow(false)
    override val internalConnected: StateFlow<Boolean>
        get() = _internalConnected.asStateFlow()


    // Internal sensor Angles from algorithm 1
    private val _intAngleFromAlg1 = MutableStateFlow<Float?>(null)
    override val intAngleFromAlg1: StateFlow<Float?>
        get() = _intAngleFromAlg1.asStateFlow()

    // Internal sensor Angles from algorithm 2
    private val _intAngleFromAlg2 = MutableStateFlow<Float?>(null)
    override val intAngleFromAlg2: StateFlow<Float?>
        get() = _intAngleFromAlg2.asStateFlow()

    // angles to List
    private val _intAngleFromAlg1List = MutableStateFlow<List<Float>>(emptyList())
    override val intAngleFromAlg1List: StateFlow<List<Float>>
        get() = _intAngleFromAlg1List.asStateFlow()

    private val _intAngleFromAlg2List = MutableStateFlow<List<Float>>(emptyList())
    override val intAngleFromAlg2List: StateFlow<List<Float>>
        get() = _intAngleFromAlg2List.asStateFlow()

    // for the recording of the data: (timestamp)
    private val _timeIntalg1 = MutableStateFlow<Long>(0L)
    override val timeIntalg1: StateFlow<Long>
        get() = _timeIntalg1.asStateFlow()

    private val _timeIntalg2 = MutableStateFlow<Long>(0L)
    override val timeIntalg2: StateFlow<Long>
        get() = _timeIntalg2.asStateFlow()

    private val _timeIntalg1list = MutableStateFlow<List<Long>>(emptyList())
    override val timeIntalg1list: StateFlow<List<Long>>
        get() = _timeIntalg1list.asStateFlow()

    private val _timeIntalg2list = MutableStateFlow<List<Long>>(emptyList())
    override val timeIntalg2list: StateFlow<List<Long>>
        get() = _timeIntalg2list.asStateFlow()



    // for the recording of the data:
    private val timestampList = mutableListOf<Long>()
    override fun getTimestamps(): List<Long> {
        return timestampList.map { it / 1_000_000_000 }
    }





    // start streaming: IMU = gyro + linear acceleration
    override fun startImuStream() {
        _measuring.update { true } // Set measuring to true when starting the stream
        _internalConnected.update {true}
        startGyroStream() // Start gyroscope events

        if (_streamingGyro.value || _streamingLinAcc.value) {
            // IMU stream is already running, no need to start again
            return
        }

        // Start linear acceleration events
        val linAccSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        if (linAccSensor != null) {
            sensorManager.registerListener(this, linAccSensor, SensorManager.SENSOR_DELAY_UI)
            _streamingLinAcc.value = true
        }
    }


    override fun applyAngleOfElevation () {
        val intAcc = _currentLinAccUI.value
        val intGyro = _currentGyroUI.value

        //Log.d(LOG_TAG, "Linear Acc: $intAcc, Gyro: $intGyro")

        if (intAcc != null && intGyro != null) {
            // Algorithm 1: compute angle of elevation
            val intAngleFromAlg1 =
                computeAngleOfElevation(intAcc.first, intAcc.second, intAcc.third)

            // algorithm 2: apply complementary filter
            val intAngleFromAlg2 = applyComplementaryFilter(
                intAcc.first, intAcc.second, intAcc.third,
                intGyro.first, intGyro.second, intGyro.third
            )

            _intAngleFromAlg1.update { intAngleFromAlg1 }
            _intAngleFromAlg2.update { intAngleFromAlg2 }

            // update lists if needed
            _intAngleFromAlg1List.update { list -> list + intAngleFromAlg1 }
            _intAngleFromAlg2List.update { list -> list + intAngleFromAlg2 }

            // Update timestamps when updating angles
            val timestamp = System.currentTimeMillis()
            _timeIntalg1.update { timestamp  }
            _timeIntalg1list.update { timeIntalg1list -> timeIntalg1list + timestamp }

            _intAngleFromAlg1.update { intAngleFromAlg1 }

            // Update timestamps when updating angles
            _timeIntalg2.update { timestamp }
            _timeIntalg2list.update { timeIntalg2list -> timeIntalg2list + timestamp }

            _intAngleFromAlg2.update { intAngleFromAlg2 }

            // Introduce a delay before updating the angle display
            GlobalScope.launch(Dispatchers.Main) {
                delay(50000) // Adjust the delay duration as needed

                // Update the UI variable
                _currentGyroUI.update { _currentGyro }
            }

        }
    }



    // ALGORITHM 1: compute angle of elevation
    private var lastFilteredAngle: Float = 0.0f // Start with 0 degrees when parallel to the ground
    private val alpha: Float = 0.4f // filter factor

        private fun computeAngleOfElevation(ax: Float, ay: Float, az: Float): Float {

            //calculate the angle
            val angleRad = atan2(az.toDouble(), sqrt(ax * ax + ay * ay).toDouble()).toFloat()

            val angleDeg = Math.toDegrees(angleRad.toDouble()).toFloat()

            // EWMA Filter
            val filteredAngle = alpha * angleDeg + (1 - alpha) * lastFilteredAngle
            // Update the previous filtered angle for the next iteration
            lastFilteredAngle = filteredAngle

            return filteredAngle //.absoluteValue

        }


    // ALGORITHM 2: Complimentary filter combining linear acceleration and gyroscope
    private val alpha2: Float = 0.9f // filter factor

    private fun applyComplementaryFilter(
        ax: Float,
        ay: Float,
        az: Float,
        gyroX: Float,
        gyroY: Float,
        gyroZ: Float

    ): Float {
        // equation for complimentary filter:
        val filteredAccelerationX = alpha2 * ax + (1 - alpha2) * gyroX
        val filteredAccelerationY = alpha2 * ay + (1 - alpha2) * gyroY
        val filteredAccelerationZ = alpha2 * az + (1 - alpha2) * gyroZ

        // acceleration vector
        val magnitude = sqrt(
            filteredAccelerationX * filteredAccelerationX +
                    filteredAccelerationY * filteredAccelerationY +
                    filteredAccelerationZ * filteredAccelerationZ
        )

        // Calculate the elevation angle
        val angleRadians = atan2(filteredAccelerationX, magnitude)

        // Adjust the angle based on orientation (perpendicular: 90 degrees, parallel: 0 degrees)
        val adjustedAngle = Math.toDegrees(angleRadians.toDouble()).toFloat()

        // If sensor is parallel to the ground, return 0 degrees; if perpendicular, return 90 degrees
        return adjustedAngle.absoluteValue
    }



    override fun stopImuStream() {
        if (!_streamingGyro.value && !_streamingLinAcc.value) {
            // IMU stream is not running, nothing to stop
            return
        }
        // Stop  gyroscope events
        stopGyroStream()

        // Stop linear acceleration events
        if (_streamingLinAcc.value) {
            val linAccSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
            if (linAccSensor != null) {
                sensorManager.unregisterListener(this, linAccSensor)
                _streamingLinAcc.value = false

                // Set measuring to false when stopping the stream
                _internalConnected.update {false}
                _measuring.update {false}
            }
        }
    }




    @OptIn(DelicateCoroutinesApi::class)
    override fun startGyroStream() {
        if (gyroSensor == null) {
            Log.e(LOG_TAG, "Gyroscope sensor is not available on this device")
            return
        }
        if (_streamingGyro.value) {
            Log.e(LOG_TAG, "Gyroscope sensor is already streaming")
            return
        }


        // Register this class as a listener for gyroscope events
        sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_UI)



        // Start a coroutine to update the UI variable on a 2 Hz interval
        GlobalScope.launch(Dispatchers.Main) {
            _streamingGyro.value = true
            while (_streamingGyro.value) {
                // Update the UI variable
                _currentGyroUI.update { _currentGyro }
               // val scalingFactor = 0.5
                delay(500)
                //delay((1000 * scalingFactor).toLong())
            }
        }

    }

    override fun stopGyroStream() {
        if (_streamingGyro.value) {
            // Unregister the listener to stop receiving gyroscope events (automatically stops the coroutine as well
            sensorManager.unregisterListener(this, gyroSensor)
            _streamingGyro.value = false
        }
    }




    private fun handleInternalAccData(sensorEvent: SensorEvent) {
        //for (sample in _streamingLinAcc.sample)
        if (sensorEvent.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {
            val timestamp = sensorEvent.timestamp // Convert nanoseconds to seconds
            _timeIntalg1.update { timestamp }
            _timeIntalg1list.update { timeIntalg1list -> timeIntalg1list + timestamp }
            val accelerationTriple = Triple(
                sensorEvent.values[0],
                sensorEvent.values[1],
                sensorEvent.values[2]
            )
            _currentLinAccUI.update { accelerationTriple }
            applyAngleOfElevation() //  calculate angles after receiving accelerometer data

        }
    }

    private fun handleInternalGyroData(sensorEvent: SensorEvent) {
        if (sensorEvent.sensor.type == Sensor.TYPE_GYROSCOPE) {
            val timestamp = sensorEvent.timestamp  // Convert nanoseconds to seconds
            _timeIntalg2.update { timestamp }
            _timeIntalg2list.update { timeIntalg2list -> timeIntalg2list + timestamp }

            val gyroTriple = Triple(
                sensorEvent.values[0],
                sensorEvent.values[1],
                sensorEvent.values[2]
            )
            _currentGyroUI.update { gyroTriple }
            applyAngleOfElevation() // calculate angles after receiving gyroscope data
        }
    }

    override fun onSensorChanged(event: SensorEvent) {

        if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            // Extract gyro data (angular speed around X, Y, and Z axes)
            _currentGyro = Triple(event.values[0], event.values[1], event.values[2])

            // Handle gyro data
            handleInternalGyroData(event) //in here it updates the angle calculation
        }

        //if the acceleration changes
        if (event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {
            handleInternalAccData(event)
        }
        // Return both angles
        val angleFromAlg1 = _intAngleFromAlg1.value
        val angleFromAlg2 = _intAngleFromAlg2.value
        val timestampAlg1 = _timeIntalg1.value
        val timestampAlg2 = _timeIntalg2.value

        // Do something with the angles:  log them
        Log.d(LOG_TAG, "Angle from Alg1: $angleFromAlg1, Timestamp Alg1: $timestampAlg1")
        Log.d(LOG_TAG, "Angle from Alg2: $angleFromAlg2, Timestamp Alg2: $timestampAlg2")
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        // Not used in this example
    }
}