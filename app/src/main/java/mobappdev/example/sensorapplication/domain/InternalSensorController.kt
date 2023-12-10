package mobappdev.example.sensorapplication.domain

/**
 * File: InternalSensorController.kt
 * Purpose: Defines the blueprint for the Internal Sensor Controller.
 * Author: Jitse van Esch
 * Created: 2023-09-21
 * Last modified: 2023-09-21
 */

import kotlinx.coroutines.flow.StateFlow

interface InternalSensorController {
    val currentLinAccUI: StateFlow<Triple<Float, Float, Float>?>
    val currentGyroUI: StateFlow<Triple<Float, Float, Float>?>
    val streamingGyro: StateFlow<Boolean>
    val streamingLinAcc: StateFlow<Boolean>

    //val connected: StateFlow<Boolean>
    val measuring: StateFlow<Boolean>

    // to display angle from algorithm 1 and 2:
    val currentAngle1: StateFlow<Float?>
    val currentAngle2: StateFlow<Float?>

    fun startImuStream()
    fun stopImuStream()

    fun startGyroStream()
    fun stopGyroStream()

    // function for the callback:
    fun setInternalSensorDataCallback(callback: (Triple<Float, Float, Float>?) -> Unit)

}