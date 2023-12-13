package mobappdev.example.sensorapplication.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import mobappdev.example.sensorapplication.ui.viewmodels.DataVM


@Composable
fun GraphScreen(vm: DataVM, navController: NavController) {

    }

@Preview
@Composable
fun GraphScreenPreview() {
    val timeList = listOf(0L, 1L, 2L, 3L, 4L, 5L)
    val angleList1 = listOf(0f, 1f, 2f, 3f, 4f, 5f)
    val angleList2 = listOf(0f, 2f, 4f, 6f, 8f, 10f)

}

