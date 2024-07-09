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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.amogusapp.camerax.CameraScreen
import com.example.amogusapp.camerax.CameraViewModel
import com.example.amogusapp.ui.theme.AmogusAppTheme
import kotlinx.serialization.Serializable

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

    private fun hasRequiredPermissions(): Boolean {
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

        //New NavHost Controller Thank you android devs :)))
        NavHost(navController = navController, startDestination = MainScreen) {
            composable<SplashScreen> { SplashScreen(navController) }
            composable<MainScreen> {
                Broccolink(navController, null)
            }
            composable<Camera> {
                val viewModel: CameraViewModel = viewModel()
                if (permissionsGranted) {
                    CameraScreen(navController,viewModel)
                } else {
                    Broccolink(navController, null)
                }
            }
            composable<BlinkScreen> {
                val args = it.toRoute<BlinkScreen>()
                Broccolink(navController, args.credentials)
            }
        }
    }
}

@Serializable
object SplashScreen

@Serializable
object MainScreen

@Serializable
object Camera

//Second version with composable as arguments
@Serializable
data class BlinkScreen(
    val credentials: String?
)
