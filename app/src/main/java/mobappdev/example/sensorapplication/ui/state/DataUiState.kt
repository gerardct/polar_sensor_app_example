package mobappdev.example.sensorapplication.ui.state

/**
 * File: DataUiState.kt
 * Purpose: Defines the UI state of the data screen.
 * Author: Jitse van Esch
 * Created: 2023-07-08
 * Last modified: 2023-07-11
 */


data class DataUiState(
    val currentHR: Int? = null,
    val hrList: List<Int> = emptyList(),
    val connected: Boolean = false,
    val measuring: Boolean = false

)
