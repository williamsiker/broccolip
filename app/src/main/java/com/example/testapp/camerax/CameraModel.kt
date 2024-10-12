package com.example.testapp.camerax

import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController

//Model For CameraX
data class CameraState(
    val imagesUris: List<Uri> = emptyList(),
    val lastScannedQrCode: String = "",
    val ssid: String = "",
    val password: String = ""
)

sealed class CameraEvent {
    data class TakePhoto(val context: Context) : CameraEvent()
    data class SwitchCamera(val isFront: Boolean) : CameraEvent()
    data class QrCodeScanned(val imageProxy: ImageProxy) : CameraEvent()
    data class ControllerSet(val controller: CameraController) : CameraEvent()
    data class HostSpotConnection(val context: Context, val ssid: String, val password: String) : CameraEvent()
}