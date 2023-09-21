package mobappdev.example.sensorapplication.ui.viewmodels

/**
 * File: DataVM.kt
 * Purpose: Defines the viewmodel of the data screen.
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import mobappdev.example.sensorapplication.domain.PolarController
import mobappdev.example.sensorapplication.ui.state.DataUiState
import javax.inject.Inject

@HiltViewModel
class DataVM @Inject constructor(
    private val polarController: PolarController
): ViewModel() {

    private val _state = MutableStateFlow(DataUiState())
    val state = combine(
        polarController.currentHR,
        polarController.hrList,
        polarController.connected,
        polarController.measuring,
        _state
    ) { currentHr, hrList, connected, measuring, state ->
        state.copy(
            currentHR = currentHr,
            hrList = hrList,
            connected = connected,
            measuring = measuring
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    private var mDeviceId: String = ""

    fun chooseSensor(deviceId: String) {
        mDeviceId = deviceId
    }

    fun connectToSensor() {
        polarController.connectToDevice(mDeviceId)
    }

    fun disconnectFromSensor() {
        polarController.disconnectFromDevice(mDeviceId)
    }

    fun acquireHr() {
        polarController.startHrStreaming(mDeviceId)
    }

    fun stopHr() {
        polarController.stopHrStreaming()
    }
}