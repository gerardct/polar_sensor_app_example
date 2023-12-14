package mobappdev.example.sensorapplication.domain

/**
 * File: PolarController.kt
 * Purpose: Defines the blueprint for the polar controller model
 * Author: Jitse van Esch
 * Created: 2023-07-08
 * Last modified: 2023-07-11
 */


import kotlinx.coroutines.flow.StateFlow

interface PolarController {
    val currentHR: StateFlow<Int?>
    val hrList: StateFlow<List<Int>>

    val currentAcceleration: StateFlow<Triple<Float, Float, Float>?>
    val accelerationList: StateFlow<List<Triple<Float, Float, Float>?>>

    val currentGyro: StateFlow<Triple<Float, Float, Float>?>
    val gyroList: StateFlow<List<Triple<Float, Float, Float>?>>

    val angleFromAlg1: StateFlow<Float?>
    val angleFromAlg2: StateFlow<Float?>
    val angleFromAlg1list:StateFlow<List<Float>>
    val angleFromAlg2list:StateFlow<List<Float>>

    val timealg1: StateFlow<Float?>
    val timealg2: StateFlow<Float?>
    val timealg1list: StateFlow<List<Float?>>
    val timealg2list: StateFlow<List<Float?>>

    val connected: StateFlow<Boolean>
    val measuring: StateFlow<Boolean>

    fun connectToDevice(deviceId: String)
    fun disconnectFromDevice(deviceId: String)

    fun startCombinedStreaming(deviceId: String)
    fun stopCombinedStreaming()
    fun calculateAndApplyAngles()



    fun startHrStreaming(deviceId: String)
    fun stopHrStreaming()
}