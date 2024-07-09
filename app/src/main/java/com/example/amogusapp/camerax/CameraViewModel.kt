package com.example.amogusapp.camerax

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CameraViewModel() : ViewModel() {
    init {
        System.loadLibrary("camerax_api")
        System.loadLibrary("connections_api")
    }

    external fun nativeRotateAndScale(bitmap: Bitmap, rotationDegrees: Float): Bitmap
    private external fun nativeConnectToServer(ip: String, port: Int): Boolean

    private val _bitmaps = MutableStateFlow<List<Bitmap>>(emptyList())
    val bitmaps = _bitmaps.asStateFlow()
    fun onTakePhoto(bitmap: Bitmap) {
        _bitmaps.value += bitmap
    }

    private var isConnected: Boolean = false
    fun connectToServer(qrCodeValue: String, onSuccess: () -> Unit) {
        val ipPortRegex = "ftp://(\\S+):(\\S+)@(.*):(\\d+)".toRegex()
        val matchResult = ipPortRegex.find(qrCodeValue)

        if (matchResult != null) {
            val ip = matchResult.groupValues[3]
            val port = matchResult.groupValues[4].toInt()

            viewModelScope.launch {
                try {
                    if (!isConnected) {
                        val connected = withContext(Dispatchers.IO) {
                            nativeConnectToServer(ip, port)
                        }

                        if (connected) {
                            isConnected = true
                            Log.d("Socket", "Connected to $ip:$port")
                            onSuccess()
                        } else {
                            Log.e("Socket", "Error connecting to $ip:$port")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Socket", "Error connecting", e)
                }
            }
        } else {
            Log.e("Socket", "Invalid QR code value format: $qrCodeValue")
        }
    }
}