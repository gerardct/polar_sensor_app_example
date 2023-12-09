package mobappdev.example.sensorapplication.domain

/**
 * File: PolarController.kt
 * Purpose: Defines the blueprint for the polar controller model
 * Author: Jitse van Esch
 * Created: 2023-07-08
 * Last modified: 2023-07-11
 */


import com.polar.sdk.api.model.PolarAccelerometerData
import kotlinx.coroutines.flow.StateFlow

interface PolarController {
    val currentHR: StateFlow<Int?>
    val hrList: StateFlow<List<Int>>

    val currentAcceleration: StateFlow<Triple<Float, Float, Float>?>
    val accelerationList: StateFlow<List<Triple<Float, Float, Float>?>>
    val currentAngleOfElevation: StateFlow<Float?>

    val currentGyro: StateFlow<Triple<Float, Float, Float>?>
    val gyroList: StateFlow<List<Triple<Float, Float, Float>?>>

    val connected: StateFlow<Boolean>
    val measuring: StateFlow<Boolean>

    fun connectToDevice(deviceId: String)
    fun disconnectFromDevice(deviceId: String)

    fun startHrStreaming(deviceId: String)
    fun stopHrStreaming()

    fun startAccStreaming(deviceId: String)
    fun stopAccStreaming()
    fun startGyroStreaming(deviceId: String)
    fun stopGyroStreaming()
}