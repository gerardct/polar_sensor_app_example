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
import kotlin.math.sqrt

private const val LOG_TAG = "Internal Sensor Controller"

class InternalSensorControllerImpl(
    context: Context
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


    // this is to differentiate angle from algorithms 1 and 2
    private val _currentAngle1 = MutableStateFlow<Float?>(null)
    override val currentAngle1: StateFlow<Float?>
        get() = _currentAngle1.asStateFlow()

    private val _currentAngle2 = MutableStateFlow<Float?>(null)
    override val currentAngle2: StateFlow<Float?>
        get() = _currentAngle2.asStateFlow()



    // Callback to update internal sensor data
    private var internalSensorDataCallback: ((Triple<Float, Float, Float>?) -> Unit)? = null

    // Setter function for the callback
    override fun setInternalSensorDataCallback(callback: (Triple<Float, Float, Float>?) -> Unit) {
        internalSensorDataCallback = callback
    }


    // ALGORITHM 1: compute angle of elevation
    private var lastFilteredAngle: Float = 0.0f
    private val alpha: Float = 0.9f // for the filter --> Change?

    private fun computeAngleOfElevation (ax: Float, ay: Float, az: Float): Float {
        //calculate the angle
        val angle = Math.atan2(az.toDouble(), sqrt(ax * ax + ay * ay).toDouble()).toFloat()

        // EWMA Filter
        val filteredAngle = alpha * angle + (1 - alpha) * lastFilteredAngle
        // Update the previous filtered angle for the next iteration
        lastFilteredAngle = filteredAngle

        val filteredAngleInDegrees = Math.toDegrees(filteredAngle.toDouble()).toFloat()
        return filteredAngleInDegrees

    }
    private fun updateCurrentAngle1(value: Float?) {
        _currentAngle1.value = value
    }

    // ALGORITHM 2: Complimentary filter combining linear acceleration and gyroscope
    private val alpha2: Float = 0.98f // filter factor

    private fun applyComplementaryFilter(ax: Float, ay: Float, az: Float, gyro: Triple<Float, Float, Float>): Triple<Float, Float, Float> {
        // equation for complimentary filter:
        val filteredAccelerationX = alpha2 * ax + (1 - alpha2) * gyro.first
        val filteredAccelerationY = alpha2 * ay + (1 - alpha2) * gyro.second
        val filteredAccelerationZ = alpha2 * az + (1 - alpha2) * gyro.third

        return Triple(filteredAccelerationX, filteredAccelerationY, filteredAccelerationZ)
    }
    private fun updateCurrentAngle2(value: Float?) {
        _currentAngle2.value = value
    }


    // Function to update measuring state
    private fun updateMeasuringState(value: Boolean) {
        _measuring.value = value
    }



    override fun startImuStream() {
        // Todo: implement
        if (_streamingGyro.value || _streamingLinAcc.value) {
            // IMU stream is already running, no need to start again
            return
        }
        // Start gyroscope events
        startGyroStream()

        // Start linear acceleration events
        val linAccSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        if (linAccSensor != null) {
            sensorManager.registerListener(this, linAccSensor, SensorManager.SENSOR_DELAY_UI)
            _streamingLinAcc.value = true

            // Set measuring to true when starting the stream
            updateMeasuringState(true)
        }
    }

    override fun stopImuStream() {
        // Todo: implement
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
                updateMeasuringState(false)
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
                delay(500)
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

    override fun onSensorChanged(event: SensorEvent) {
        // linear acceleration data:
        val ax = event.values[0]
        val ay = event.values[1]
        val az = event.values[2]

        if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            // Extract gyro data (angular speed around X, Y, and Z axes (for algorithm 2)
            _currentGyro = Triple(event.values[0], event.values[1], event.values[2])

            // Apply complementary filter for gyroscope (for algorithm 2)
            val angle2 = applyComplementaryFilter(ax, ay, az, _currentGyro ?: Triple(0.0f, 0.0f, 0.0f))

            // Update UI or perform further processing with the filtered gyro data
            _currentLinAccUI.value = angle2

            // Update internal sensor data stream (with callback)
            internalSensorDataCallback?.invoke(angle2)
            //updateCurrentAngle1(angle2)
        }

        //if the acceleration changes
        if (event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {

            // compute angle of elevation (for algorithm 1)
            val angle1 = computeAngleOfElevation(ax, ay, az)
            // Update UI or perform further processing with the angle1
            updateCurrentAngle1(angle1)

            // Update internal sensor data stream
            //internalSensorDataCallback?.invoke(angle1)
            // internalSensorDataCallback?.invoke(Triple(ax, ay, az))
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        // Not used in this example
    }
}