package com.example.amogusapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.amogusapp.ui.theme.AmogusAppTheme

class MainActivity : ComponentActivity() {
    private var permissionsGranted by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasRequiredPermissions()) {
            ActivityCompat.requestPermissions(
                this, CAMERAX_PERMISSIONS, 0
            )
        } else {
            permissionsGranted = true
        }
        setContent {
            AmogusAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(permissionsGranted)
                }
            }
        }
    }

    fun hasRequiredPermissions(): Boolean {
        return CAMERAX_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                applicationContext,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this, CAMERAX_PERMISSIONS, 0
        )
    }

    companion object {
        private val CAMERAX_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
        )
    }

    @SuppressLint("UnrememberedMutableState")
    @Composable
    fun AppNavigation(permissionsGranted: Boolean) {
        val navController = rememberNavController()
        val context = LocalContext.current

        //Main screen for practicity
        NavHost(navController = navController, startDestination = "mainScreen") {
            composable("splashScreen") { SplashScreen(navController) }
            composable("mainScreen") { MainScreen(navController, this@MainActivity) }
            composable("cameraScreen") {
                val cameraHandler = remember { CameraHandler(context) }
                if (permissionsGranted) {
                    cameraHandler.CameraScreen(navController)
                } else {
                    MainScreen(navController, this@MainActivity)
                }
            }
            composable(
                "send-fileScreen/{cadena}",
                arguments = listOf(navArgument("cadena") { type = NavType.StringType })
            ) { backStackEntry ->
                val stringValue = backStackEntry.arguments?.getString("cadena")
                stringValue?.let {
                    SendFilesActivity().SendFilesScreen(navController, it)
                }
            }
            composable("conection/{cadena}",
                arguments = listOf(navArgument("cadena") {type = NavType.StringType})
            ) {backStackEntry ->
                val stringValue = backStackEntry.arguments?.getString("cadena")
                stringValue?.let {
                    addConnection(it)
                }
            }

        }
    }
}
