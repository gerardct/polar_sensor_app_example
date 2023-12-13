package mobappdev.example.sensorapplication.ui

/**
 * File: MainActivity.kt
 * Purpose: Defines the main activity of the application.
 * Author: Jitse van Esch
 * Created: 2023-07-08
 * Last modified: 2023-09-21
 */

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import mobappdev.example.sensorapplication.ui.screens.BluetoothDataScreen
import mobappdev.example.sensorapplication.ui.screens.GraphScreen
import mobappdev.example.sensorapplication.ui.theme.SensorapplicationTheme
import mobappdev.example.sensorapplication.ui.viewmodels.DataVM

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // Todo: Change for your own deviceID
    private var deviceId = "B36A7E29" // B37EA42F  B36A7E29  C07A5921

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT), 31)
            } else {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 30)
            }
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 29)
        }

        setContent {
            SensorapplicationTheme {
                val dataVM = hiltViewModel<DataVM>()

                // Use hardcoded deviceID
                dataVM.chooseSensor(deviceId)

                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = "BluetoothDataScreen"
                    ) {
                        composable("BluetoothDataScreen") {

                            BluetoothDataScreen(vm = dataVM, navController = navController)
                        }
                        composable("Graphscreen") {
                            // Instantiate the viewmodel
                            GraphScreen(vm = dataVM, navController = navController)
                        }
                    }
                }
            }
        }
    }
}
