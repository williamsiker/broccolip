package com.example.amogusapp.camerax

import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.navigation.NavController
import com.example.amogusapp.BlinkScreen
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch

//UI FOR CAMERAX SCANNING
@androidx.annotation.OptIn(ExperimentalGetImage::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(navController: NavController, viewModel: CameraViewModel) {
    val context = LocalContext.current
    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(
                CameraController.IMAGE_CAPTURE or
                        CameraController.VIDEO_CAPTURE or
                        CameraController.IMAGE_ANALYSIS
            )
        }
    }
    fun takePhoto(onPhotoTaken: (Bitmap) -> Unit) {
        controller.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)
                    val bitmap = image.toBitmap()
                    val rotatedBitmap = viewModel.nativeRotateAndScale(bitmap, image.imageInfo.rotationDegrees.toFloat())
                    onPhotoTaken(rotatedBitmap)
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    Log.e("Camera", "Error capturing photo", exception)
                }
            }
        )
    }

    fun scanQRCode(imageProxy: ImageProxy, onQrCodeDetected: (String) -> Unit) {
        val image = InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        val scanner: BarcodeScanner = BarcodeScanning.getClient(options)

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    barcode.rawValue?.let {
                        onQrCodeDetected(it)
                    }
                }
                imageProxy.close()
            }
            .addOnFailureListener {
                Log.e("QR Scan", "Error scanning QR code", it)
                imageProxy.close()
            }
    }
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState()
    val bitmaps by viewModel.bitmaps.collectAsState()
    var qrCode by remember { mutableStateOf<String?>(null) }

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
                controller = controller,
                modifier = Modifier.fillMaxSize()
            )

            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.offset(16.dp, 16.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }

            IconButton(
                onClick = {
                    controller.cameraSelector =
                        if (controller.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
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

            qrCode?.let { code ->
                LaunchedEffect(code) {
                    Toast.makeText(context, "CONNECTED :)", Toast.LENGTH_SHORT).show()
                    navController.navigate(BlinkScreen(credentials = code))
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
                        takePhoto(onPhotoTaken = viewModel::onTakePhoto)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Take photo"
                    )
                }
            }

            LaunchedEffect(Unit) {
                controller.setImageAnalysisAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                    scanQRCode(imageProxy) { qrCodeValue ->
                        qrCode = qrCodeValue
                    }
                }
            }
        }
    }
}
