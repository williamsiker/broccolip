package com.example.testapp.camerax

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.Executor

//ViewModel for CameraX
class CameraViewModel : ViewModel() {
    init {
        System.loadLibrary("camerax_api")
        System.loadLibrary("connections_api")
    }
    private external fun nativeRotateAndScale(bitmap: Bitmap, rotationDegrees: Float): Bitmap
    private external fun nativeConnectToServer(ip: String, port: Int): Boolean

    private val _state = MutableStateFlow(CameraState())
    val state = _state.asStateFlow()

    private var isFrontCamera = false
    private var cameraController: CameraController? = null

    private fun setCameraController(controller: CameraController) { cameraController = controller }

    override fun onCleared() {
        _state.value = CameraState()
        super.onCleared()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun onEvent(event: CameraEvent) {
        when (event) {
            is CameraEvent.TakePhoto -> {
                takePhoto(event.context)
            }
            is CameraEvent.SwitchCamera -> {
                switchCamera(event.isFront)
            }
            is CameraEvent.QrCodeScanned -> {
                scanQRCode(event.imageProxy) { qrCodeValue ->
                    _state.update {
                        it.copy(lastScannedQrCode = qrCodeValue)
                    }
                }
            }
            is CameraEvent.ControllerSet -> {
                setCameraController(event.controller)
            }

            is CameraEvent.HostSpotConnection -> {
                connectToHostSpot(event.context, event.ssid, event.password)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun takePhoto(context: Context) {
        val mainExecutor: Executor = ContextCompat.getMainExecutor(context)
        val cameraController = cameraController ?: return
        cameraController.takePicture(mainExecutor, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                viewModelScope.launch {
                    val bitmap = withContext(Dispatchers.IO) {
                        imageProxyToBitmap(imageProxy)
                    }
                    val rotatedBitmap = withContext(Dispatchers.IO) {
                        nativeRotateAndScale(bitmap, imageProxy.imageInfo.rotationDegrees.toFloat())
                    }
                    saveImageToGallery(context, rotatedBitmap)
                    imageProxy.close()
                    bitmap.recycle()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("TakePhoto", "Error capturing photo", exception)
            }
        })
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val buffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, this)
            inSampleSize = calculateInSampleSize(this, imageProxy.width, imageProxy.height)
            inJustDecodeBounds = false
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun saveImageToGallery(context: Context, bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "amogus_image_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Camera")
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let { imageUri ->
                resolver.openOutputStream(imageUri)?.use { outputStream ->
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)) {
                        throw IOException("Failed to save bitmap.")
                    }
                }
            }
            uri?.let { _state.update { state -> state.copy(imagesUris = state.imagesUris + it) } }
        }
    }

    private fun switchCamera(isFront: Boolean) {
        isFrontCamera = isFront
        cameraController?.cameraSelector = if (isFront) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun scanQRCode(imageProxy: ImageProxy, onQrCodeDetected: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val image = InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)
            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
            val scanner: BarcodeScanner = BarcodeScanning.getClient(options)

            scanner.process(image).addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    barcode.rawValue?.let { onQrCodeDetected(it) }
                }
                imageProxy.close()
            }.addOnFailureListener {
                Log.e("QR Scan", "Error scanning QR code", it)
                imageProxy.close()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun connectToHostSpot(context: Context, ssid: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val wifiNetworkSpecifier = WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(password)
                .build()

            val networkRequest = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED) // No requiere acceso a Internet
                .addCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED)
                .setNetworkSpecifier(wifiNetworkSpecifier)
                .build()

            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            withContext(Dispatchers.Main) {
                cm.requestNetwork(networkRequest, object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        super.onAvailable(network)
                        cm.bindProcessToNetwork(network)
                        Toast.makeText(context, "Connected to $ssid", Toast.LENGTH_SHORT).show()
                    }

                    override fun onUnavailable() {
                        super.onUnavailable()
                        Toast.makeText(context, "Network unavailable", Toast.LENGTH_SHORT).show()
                    }

                    override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                        super.onCapabilitiesChanged(network, capabilities)
                        // No verificar la capacidad de Internet
                        Toast.makeText(context, "Connected to $ssid", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }
    }

}