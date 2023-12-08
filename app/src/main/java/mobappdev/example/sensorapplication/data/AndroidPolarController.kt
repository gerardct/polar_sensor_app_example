package mobappdev.example.sensorapplication.data

/**
 * File: AndroidPolarController.kt
 * Purpose: Implementation of the PolarController Interface.
 *          Communicates with the polar API
 * Author: Jitse van Esch
 * Created: 2023-07-08
 * Last modified: 2023-07-11
 */

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.util.Log
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.errors.PolarInvalidArgument
import com.polar.sdk.api.model.PolarAccelerometerData
import com.polar.sdk.api.model.PolarDeviceInfo
import com.polar.sdk.api.model.PolarHrData
import com.polar.sdk.api.model.PolarSensorSetting
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import mobappdev.example.sensorapplication.domain.PolarController
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.sqrt

class AndroidPolarController(
    private val context: Context,
) : PolarController, SensorEventListener {

    private val api: PolarBleApi by lazy {
        // Notice all features are enabled
        PolarBleApiDefaultImpl.defaultImplementation(
            context = context,
            setOf(
                PolarBleApi.PolarBleSdkFeature.FEATURE_HR,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_SDK_MODE,
                PolarBleApi.PolarBleSdkFeature.FEATURE_BATTERY_INFO,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_H10_EXERCISE_RECORDING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_OFFLINE_RECORDING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_DEVICE_TIME_SETUP,
                PolarBleApi.PolarBleSdkFeature.FEATURE_DEVICE_INFO,
            )
        )
    }

    private var hrDisposable: Disposable? = null
    private var accDisposable: Disposable? = null
    private val TAG = "AndroidPolarController"

    private val _currentHR = MutableStateFlow<Int?>(null)
    override val currentHR: StateFlow<Int?>
        get() = _currentHR.asStateFlow()

    private val _hrList = MutableStateFlow<List<Int>>(emptyList())
    override val hrList: StateFlow<List<Int>>
        get() = _hrList.asStateFlow()

    private val _connected = MutableStateFlow(false)
    override val connected: StateFlow<Boolean>
        get() = _connected.asStateFlow()

    private val _measuring = MutableStateFlow(false)
    override val measuring: StateFlow<Boolean>
        get() = _measuring.asStateFlow()

    private val _currentAcceleration = MutableStateFlow<Triple<Float, Float, Float>?>(null)
    override val currentAcceleration: StateFlow<Triple<Float, Float, Float>?>
        get() = _currentAcceleration.asStateFlow()

    private val _accelerationList = MutableStateFlow<List<Triple<Float, Float, Float>?>>(emptyList())
    override val accelerationList: StateFlow<List<Triple<Float, Float, Float>?>>
        get() = _accelerationList.asStateFlow()

    private val _currentAngleOfElevation = MutableStateFlow<Float?>(null)
    override val currentAngleOfElevation: StateFlow<Float?>
        get() = _currentAngleOfElevation.asStateFlow()

    init {
        api.setPolarFilter(false) //if true, only Polar devices are looked for

        val enableSdkLogs = false
        if(enableSdkLogs) {
            api.setApiLogger { s: String -> Log.d("Polar API Logger", s) }
        }

        api.setApiCallback(object: PolarBleApiCallback() {
            override fun batteryLevelReceived(identifier: String, level: Int) {
                Log.d(TAG, "BATTERY LEVEL: $level")
            }

            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "CONNECTED: ${polarDeviceInfo.deviceId}")
                _connected.update { true }
            }

            override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "CONNECTING: ${polarDeviceInfo.deviceId}")
            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "DISCONNECTED: ${polarDeviceInfo.deviceId}")
                _connected.update { false }
            }

            override fun disInformationReceived(identifier: String, uuid: UUID, value: String) {
                Log.d(TAG, "DIS INFO uuid: $uuid value: $value")
            }
        })
    }

    override fun connectToDevice(deviceId: String) {
        try {
            api.connectToDevice(deviceId)
        } catch (polarInvalidArgument: PolarInvalidArgument) {
            Log.e(TAG, "Failed to connect to $deviceId.\n Reason $polarInvalidArgument")
        }
    }

    override fun disconnectFromDevice(deviceId: String) {
        try {
            api.disconnectFromDevice(deviceId)
        } catch (polarInvalidArgument: PolarInvalidArgument) {
            Log.e(TAG, "Failed to disconnect from $deviceId.\n Reason $polarInvalidArgument")
        }
    }

    override fun startHrStreaming(deviceId: String) {
        val isDisposed = hrDisposable?.isDisposed ?: true
        if(isDisposed) {
            _measuring.update { true }
            hrDisposable = api.startHrStreaming(deviceId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { hrData: PolarHrData ->
                        for (sample in hrData.samples) {
                            _currentHR.update { sample.hr }
                            _hrList.update { hrList ->
                                hrList + sample.hr
                            }
                        }
                    },
                    { error: Throwable ->
                        Log.e(TAG, "Hr stream failed.\nReason $error")
                    },
                    { Log.d(TAG, "Hr stream complete")}
                )
        } else {
            Log.d(TAG, "Already streaming")
        }

    }

    override fun stopHrStreaming() {
        _measuring.update { false }
        hrDisposable?.dispose()
        _currentHR.update { null }
    }
    override fun startAccStreaming(deviceId: String) {
        val isDisposed = accDisposable?.isDisposed ?: true
        if (isDisposed) {
            _measuring.update { true }

            val sensorSettings = mapOf(
                PolarSensorSetting.SettingType.CHANNELS to 3,
                PolarSensorSetting.SettingType.RANGE to 8,
                PolarSensorSetting.SettingType.RESOLUTION to 16,
                PolarSensorSetting.SettingType.SAMPLE_RATE to 52
            )

            val polarSensorSettings = PolarSensorSetting(sensorSettings)

            accDisposable = api.startAccStreaming(deviceId, polarSensorSettings)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { accData: PolarAccelerometerData ->
                        for (sample in accData.samples) {
                            val accelerationTriple = Triple(sample.x.toFloat(), sample.y.toFloat(), sample.z.toFloat())
                            _currentAcceleration.update { accelerationTriple }
                            _accelerationList.update { accelerationList ->
                                accelerationList + accelerationTriple
                            }
                        }
                    },
                    { error: Throwable ->
                        Log.e(TAG, "Acceleration stream failed.\nReason: $error")
                    },
                    { Log.d(TAG, "Acceleration stream complete") }
                )
        } else {
            Log.d(TAG, "Already streaming")
        }
    }

    override fun stopAccStreaming() {
        _measuring.update { false }
        accDisposable?.dispose()
        _currentAcceleration.update { null }
    }

    // ALGORITHM 1: compute angle of elevation
    private var lastFilteredAngle: Float = 0.0f
    private val alpha: Float = 0.2f // for the filter

    private fun computeAngleOfElevation(ax: Float, ay: Float, az: Float): Float {
        // Calculate the angle
        val angle = atan2(az.toDouble(), sqrt(ax * ax + ay * ay).toDouble()).toFloat()

        // EWMA Filter
        val filteredAngle = alpha * angle + (1 - alpha) * lastFilteredAngle
        // Update the previous filtered angle for the next iteration
        lastFilteredAngle = filteredAngle
        return filteredAngle
    }

    override fun onSensorChanged(event: SensorEvent) {
        // If the acceleration changes
        if (event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {
            // Linear acceleration data
            val ax = event.values[0]
            val ay = event.values[1]
            val az = event.values[2]

            // Compute angle of elevation
            val angle = computeAngleOfElevation(ax, ay, az)

            // Update UI or perform further processing with the angle
            _currentAcceleration.value = Triple(ax, ay, az)

            // Update angle of elevation
            _currentAngleOfElevation.value = angle
        }
    }
    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        // Not used in this example
    }
}