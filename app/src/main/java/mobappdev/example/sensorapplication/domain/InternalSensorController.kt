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

    val measuring: StateFlow<Boolean>
    val internalConnected: StateFlow<Boolean>


    // to display angle from algorithm 1 and 2:
    val intAngleFromAlg1: StateFlow<Float?>
    val intAngleFromAlg2: StateFlow<Float?>
    val intAngleFromAlg1List: StateFlow<List<Float>>
    val intAngleFromAlg2List: StateFlow<List<Float>>

    fun startImuStream()
    fun stopImuStream()

    fun startGyroStream()
    fun stopGyroStream()

    fun applyAngleOfElevation()



}