package com.example.amogusapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

@Preview(showBackground=true)
@Composable
fun MainScreenPreview() {
    val navController = rememberNavController()
    MainScreen(navController, MainActivity())
}

@Composable
fun MainScreen(navController: NavController, mainActivity: MainActivity) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                if (mainActivity.hasRequiredPermissions()) {
                    navController.navigate("cameraScreen")
                } else {
                    mainActivity.requestPermissions()
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Blue
            ),
            modifier = Modifier.padding(8.dp)
        ) {
            Text("Conectarse con QR", color = Color.White)
        }
    }
}
