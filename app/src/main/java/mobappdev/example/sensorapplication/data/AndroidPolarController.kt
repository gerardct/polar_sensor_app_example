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
import android.util.Log
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.errors.PolarInvalidArgument
import com.polar.sdk.api.model.PolarAccelerometerData
import com.polar.sdk.api.model.PolarDeviceInfo
import com.polar.sdk.api.model.PolarGyroData
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
) : PolarController {

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

    private val _currentGyro = MutableStateFlow<Triple<Float, Float, Float>?>(null)
    override val currentGyro: StateFlow<Triple<Float, Float, Float>?>
        get() = _currentGyro.asStateFlow()

    private val _gyroList = MutableStateFlow<List<Triple<Float, Float, Float>?>>(emptyList())
    override val gyroList: StateFlow<List<Triple<Float, Float, Float>?>>
        get() = _gyroList.asStateFlow()

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
    // Method to start streaming the accelerometer data
    override fun startAccStreaming(deviceId: String) {
        if (accDisposable?.isDisposed == false) {
            Log.d(TAG, "Already streaming")
            return
        }

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
                { handleAccData(it) },
                { handleError(it) },
                { handleComplete() }
            )
    }

    // Handle accelerometer data
    private fun handleAccData(accData: PolarAccelerometerData) {
        for (sample in accData.samples) {
            val accelerationTriple = Triple(sample.x.toFloat(), sample.y.toFloat(), sample.z.toFloat())
            _currentAcceleration.update { accelerationTriple }

            val angle = computeAngleOfElevation(sample.x.toFloat(), sample.y.toFloat(), sample.z.toFloat())
            _currentAngleOfElevation.update { angle }
        }
    }

    // Handle errors in the stream
    private fun handleError(error: Throwable) {
        Log.e(TAG, "Acceleration stream failed.\nReason: $error")
    }

    // Handle completion of the stream
    private fun handleComplete() {
        Log.d(TAG, "Acceleration stream complete")
    }

    // Method to stop streaming the accelerometer data
    override fun stopAccStreaming() {
        _measuring.update { false }
        accDisposable?.dispose()
        _currentAcceleration.update { null }
    }


    // ALGORITHM 1: compute angle of elevation
    private var lastFilteredAngle: Float = 0.0f
    private val alpha: Float = 0.4f // for the filter

    fun computeAngleOfElevation(ax: Float, ay: Float, az: Float): Float {
        val projectedMagnitude = sqrt(ax * ax + ay * ay)

        if (projectedMagnitude < 0.000001f) {
            return 0.0f
        }

        val elevationAngle = atan2(az.toDouble(), projectedMagnitude.toDouble()).toFloat()

        val filteredAngle = alpha * elevationAngle + (1 - alpha) * lastFilteredAngle
        lastFilteredAngle = filteredAngle

        val elevationAngleInDegrees = Math.toDegrees(filteredAngle.toDouble()).toFloat()

        return elevationAngleInDegrees
    }

    // ALGORITHM 2: Complimentary filter combining linear acceleration and gyroscope
    private val alpha2: Float = 0.9f // filter factor

    private fun applyComplementaryFilter(ax: Float, ay: Float, az: Float, gyro: Triple<Float, Float, Float>): Triple<Float, Float, Float> {
        // equation for complimentary filter:
        val filteredAccelerationX = alpha2 * ax + (1 - alpha2) * gyro.first
        val filteredAccelerationY = alpha2 * ay + (1 - alpha2) * gyro.second
        val filteredAccelerationZ = alpha2 * az + (1 - alpha2) * gyro.third

        return Triple(filteredAccelerationX, filteredAccelerationY, filteredAccelerationZ)
    }

    private var gyroDisposable: Disposable? = null
    override fun startGyroStreaming(deviceId: String) {
        if (gyroDisposable?.isDisposed == false) {
            Log.d(TAG, "Already streaming")
            return
        }

        _measuring.update { true }

        // Configure gyro sensor settings as needed
        val gyroSensorSettings = mapOf(
            PolarSensorSetting.SettingType.CHANNELS to 3, // Number of gyro channels
            PolarSensorSetting.SettingType.RANGE to 2000, // Gyro range (e.g., ±2000 degrees/second)
            PolarSensorSetting.SettingType.RESOLUTION to 16, // Resolution (bits)
            PolarSensorSetting.SettingType.SAMPLE_RATE to 100 // Sample rate (e.g., 100 Hz)
        )

        val polarGyroSettings = PolarSensorSetting(gyroSensorSettings)

        gyroDisposable = api.startGyroStreaming(deviceId, polarGyroSettings)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { gyroData: PolarGyroData ->
                        // Handle gyro data
                        handleGyroData(gyroData)
                    },
                    { error: Throwable ->
                        // Handle errors in gyro stream
                        Log.e(TAG, "Gyro stream failed.\nReason $error")
                    },
                    {
                        // Handle completion of gyro stream
                        Log.d(TAG, "Gyro stream complete")
                    }
                )
    }

    override fun stopGyroStreaming() {
        _measuring.update { false }
        gyroDisposable?.dispose()
        // Perform any additional cleanup if necessary
    }

    private fun handleGyroData(gyroData: PolarGyroData) {
        for (sample in gyroData.samples) {
            val gyroTriple = Triple(sample.x, sample.y, sample.z)
            _currentGyro.update { gyroTriple }

            // Optionally, add the gyro data to a list
            _gyroList.update { gyroList ->
                gyroList + gyroTriple
            }

            // Calculate angle of elevation using linear acceleration only
            val angleUsingLinearAcc = computeAngleOfElevation(
                _currentAcceleration.value?.first ?: 0f,
                _currentAcceleration.value?.second ?: 0f,
                _currentAcceleration.value?.third ?: 0f
            )

            // Calculate angle of elevation using linear acceleration and gyroscope data
            val complementaryAngle = applyComplementaryFilter(
                _currentAcceleration.value?.first ?: 0f,
                _currentAcceleration.value?.second ?: 0f,
                _currentAcceleration.value?.third ?: 0f,
                gyroTriple
            )

            // Update UI or perform other operations with angles
            // For example, update UI text views with angle values
            // angleUsingLinearAccTextView.text = "Angle using Linear Acc: $angleUsingLinearAcc"
            // complementaryAngleTextView.text = "Complementary Angle: $complementaryAngle"

            // Optionally, pass the angles to other functions or components as needed
            // addComplementaryFunction(angleUsingLinearAcc, complementaryAngle)
        }
    }
}