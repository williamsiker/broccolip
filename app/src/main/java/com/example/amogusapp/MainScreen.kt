package com.example.amogusapp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.DeviceUnknown
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.LaptopMac
import androidx.compose.material.icons.outlined.LaptopWindows
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

data class Connections (
    val osImgvector : ImageVector,
    val os : String,
    val device : String,
    var isOnsession : Boolean,
)

fun matchImgVector(str : String) : ImageVector {
    return when(str) {
        "linux" -> Icons.Outlined.Coffee
        "mac" -> Icons.Outlined.LaptopMac
        "windows" -> Icons.Outlined.LaptopWindows
        "android" -> Icons.Outlined.Android
        else -> Icons.Outlined.DeviceUnknown
    }
}

@Composable
fun addConnection(deviceData : String) : Connections {
    //connection://linux,deviceName,1
    val regularExpression = "connection://(\\S+):(\\S+):(\\d+)".toRegex()
    val matchResult = regularExpression.find(deviceData)

    return if (matchResult != null) {
        // Extract the matched values
        val os = matchResult.groupValues[1]
        val deviceName = matchResult.groupValues[2]
        val status = matchResult.groupValues[3].toInt()

        // Get the corresponding ImageVector for the operating system
        val osImg = matchImgVector(os)

        Connections(
            osImgvector = osImg,
            os = os,
            device = deviceName,
            isOnsession = status == 1
        )
    } else {
        Connections(Icons.Outlined.Error, "no", "no", false)
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    val navController = rememberNavController()
    MainScreen(navController, MainActivity())
}

@Composable
fun MainScreen(navController: NavController, mainActivity: MainActivity) {
    var isContextMenuVisible by remember { mutableStateOf(false) }
    val conections = remember { mutableStateListOf<Connections>() }

    Column(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .wrapContentSize(Alignment.TopEnd),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = {
                    if (mainActivity.hasRequiredPermissions()) {
                        navController.navigate("cameraScreen")
                    } else {
                        mainActivity.requestPermissions()
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.QrCodeScanner,
                    contentDescription = "Qr Connection"
                )
            }

            IconButton(
                onClick = {
                    isContextMenuVisible = true
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Thunderstorm,
                    contentDescription = "Manual Connections"
                )
            }

            DropdownMenu(
                expanded = isContextMenuVisible,
                onDismissRequest = { isContextMenuVisible = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Amogus") },
                    onClick = { isContextMenuVisible = false }
                )
                DropdownMenuItem(
                    text = { Text("Bmogus") },
                    onClick = { isContextMenuVisible = false }
                )
            }

        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Conecciones")
        }
        Row (
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Start
        ) {
            conections.forEach { conection ->
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedCard(
                        elevation = CardDefaults.cardElevation(4.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        border = BorderStroke(1.dp, Color.Gray)
                    ) {
                        Text(conection.device)
                    }
                }
            }
        }
    }
}