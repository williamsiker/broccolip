package com.example.amogusapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class CameraHandler(context: Context) {
    private val controller = LifecycleCameraController(context).apply {
        setEnabledUseCases(
            CameraController.IMAGE_CAPTURE or
                    CameraController.VIDEO_CAPTURE or
                    CameraController.IMAGE_ANALYSIS
        )
    }

    private var socket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null
    private var isConnected: Boolean = false

    private fun getController(): LifecycleCameraController {
        return controller
    }

    private fun takePhoto(
        context: Context,
        onPhotoTaken: (Bitmap) -> Unit
    ) {
        controller.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)
                    val matrix = Matrix().apply {
                        postRotate(image.imageInfo.rotationDegrees.toFloat())
                        postScale(-1f, 1f)
                    }
                    val rotatedBitmap = Bitmap.createBitmap(
                        image.toBitmap(),
                        0,
                        0,
                        image.width,
                        image.height,
                        matrix,
                        true
                    )

                    onPhotoTaken(rotatedBitmap)
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    Log.e("Camera", "Error capturing photo", exception)
                }
            }
        )
    }

    //Function to analyze QR code
    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun scanQRCode(imageProxy: ImageProxy, onQrCodeDetected: (String) -> Unit) {
        val image = InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        val scanner : BarcodeScanner = BarcodeScanning.getClient(options)

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for(barcode in barcodes) {
                    barcode.rawValue?.let {
                        onQrCodeDetected(it)
                    }
                }
                imageProxy.close()
            }
            .addOnFailureListener {
                Log.e("QR Scan", "Error Scaning QR code", it)
                imageProxy.close()
            }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CameraScreen(navController: NavController) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val scaffoldState = rememberBottomSheetScaffoldState()
        val viewModel = viewModel<MainViewModel>()
        val bitmaps by viewModel.bitmaps.collectAsState()
        var qrCode by remember { mutableStateOf<String?>(null) }
        var isLoading by remember { mutableStateOf(false) }

        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetPeekHeight = 0.dp,
            sheetContent = {
                PhotoBottomSheetContent(
                    bitmaps = bitmaps,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                CameraPreview(
                    controller = getController(),
                    modifier = Modifier.fillMaxSize()
                )
                // Botón para retroceder
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.offset(16.dp, 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }

                // Botón para cambiar las cámaras
                IconButton(
                    onClick = {
                        getController().cameraSelector =
                            if (getController().cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            } else CameraSelector.DEFAULT_BACK_CAMERA
                    },
                    modifier = Modifier.offset(16.dp, 72.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch camera"
                    )
                }

                // QR functionality
                qrCode?.let {
                    Column(
                        modifier = Modifier.align(Alignment.TopCenter),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AlertDialog(
                            onDismissRequest = { qrCode = null },
                            title = { Text("QR Code Detected") },
                            text = { Text(it) },
                            confirmButton = {
                                Row {
                                    Button(onClick = { qrCode = null }) {
                                        Text("OK")
                                    }
                                    if (!isConnected) {
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Button(
                                            onClick = {
                                                isLoading = true
                                                connectToServer(it, scope) {
                                                    val encodedString = URLEncoder.encode(it, StandardCharsets.UTF_8.toString())
                                                    navController.navigate("send-fileScreen/$encodedString") {
                                                        popUpTo("cameraScreen") { inclusive = true }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                    isLoading = false
                                                }
                                            },
                                            enabled = !isLoading
                                        ) {
                                            if (isLoading) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(24.dp),
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            } else {
                                                Text("Conectar")
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                scaffoldState.bottomSheetState.expand()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Photo,
                            contentDescription = "Open gallery"
                        )
                    }

                    IconButton(
                        onClick = {
                            takePhoto(context, onPhotoTaken = viewModel::onTakePhoto)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Take photo"
                        )
                    }
                }

                LaunchedEffect(Unit) {
                    getController().setImageAnalysisAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                        scanQRCode(imageProxy) { qrCodeValue ->
                            qrCode = qrCodeValue
                        }
                    }
                }
            }
        }
    }

    private fun connectToServer(qrCodeValue: String, scope: CoroutineScope, onSuccess: () -> Unit) {
        val ipPortRegex = "ftp://(.*):(\\d+)#(\\S+)#(\\S+)".toRegex()
        val matchResult = ipPortRegex.find(qrCodeValue)

        if (matchResult != null) {
            val ip = matchResult.groupValues[1]
            val port = matchResult.groupValues[2].toInt()

            scope.launch {
                try {
                    if (!isConnected) {
                        withContext(Dispatchers.IO) {
                            socket = Socket(ip, port).apply {
                                reader = BufferedReader(InputStreamReader(getInputStream()))
                                writer = BufferedWriter(OutputStreamWriter(getOutputStream()))
                            }
                        }
                        isConnected = true
                        Log.d("Socket", "Connected to $ip:$port")
                        onSuccess() // Llama a la lambda onSuccess cuando la conexión es exitosa
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

/*
fun ImageProxy.toBitmap(): Bitmap {
    val planeProxy = this.planes[0]
    val buffer = planeProxy.buffer
    buffer.rewind()
    val bytes = ByteArray(buffer.capacity())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
}
*/
