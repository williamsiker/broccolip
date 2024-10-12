package com.example.testapp.camerax

import android.graphics.Color
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import androidx.camera.core.CameraSelector
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
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
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.testapp.BlinkScreen


fun parseString(input: String): Triple<String, String, String>? {
    val regex = """([^}]+),([^}]+),([^}]+)""".toRegex()
    val matchResult = regex.find(input)

    return if (matchResult != null && matchResult.groupValues.size == 4) {
        val (_, ssid, password, url) = matchResult.groupValues
        Triple(ssid.trim(), password.trim(), url.trim())
    } else {
        null
    }
}

@Composable
fun CameraScreen(
    navController: NavController,
    state: CameraState,
    onEvent: (CameraEvent) -> Unit
) {
    val context = LocalContext.current

    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS or
            CameraController.IMAGE_CAPTURE or CameraController.VIDEO_CAPTURE)
        }
    }
    val lyfecycleOwner = LocalLifecycleOwner.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButtonPosition = FabPosition.Center,
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.offset(16.dp, 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        ,
                    factory = { context ->
                        PreviewView(context).apply {
                            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                            setBackgroundColor(Color.BLACK)
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            scaleType = PreviewView.ScaleType.FILL_START
                        }.also { previewView ->
                            previewView.controller = controller
                            controller.bindToLifecycle(lyfecycleOwner)
                        }
                    }
                )
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                IconButton(
                    onClick = {
                        val isFront = controller.cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA
                        onEvent(CameraEvent.SwitchCamera(!isFront))
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch camera"
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    IconButton(
                        onClick = {}
                    ) {
                        Icon(
                            imageVector = Icons.Default.Photo,
                            contentDescription = "Open gallery"
                        )
                    }

                    IconButton(
                        onClick = {
                            onEvent(CameraEvent.TakePhoto(context))
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Take photo"
                        )
                    }
                }
            }
        }
    )

    val result = parseString(state.lastScannedQrCode)
    result?.let {
        val (ssid, password, url) = result
        LaunchedEffect(Unit) {
            onEvent(CameraEvent.HostSpotConnection(context,ssid, password))
        }
        LaunchedEffect(Unit) {
            //Toast.makeText(context, "CONNECTING :)", Toast.LENGTH_SHORT).show()
            navController.navigate(BlinkScreen(credentials = url))
        }
    }
    LaunchedEffect(controller) {
        onEvent(CameraEvent.ControllerSet(controller))
    }

    LaunchedEffect(Unit) {
        controller.setImageAnalysisAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
            onEvent(CameraEvent.QrCodeScanned(imageProxy))
        }
    }
}
