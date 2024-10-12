package com.example.testapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.room.Room
import com.example.testapp.camerax.CameraScreen
import com.example.testapp.camerax.CameraViewModel
import com.example.testapp.filecnt.Broccolink
import com.example.testapp.filecnt.FileViewModel
import com.example.testapp.filecnt.FileViewModelFactory
import com.example.testapp.lalgebra.BasicOperations
import com.example.testapp.lalgebra.MatrixLayout
import com.example.testapp.lalgebra.MatrixViewModel
import com.example.testapp.room.ChatDatabase
import com.example.testapp.ui.theme.AmogusAppTheme
import kotlinx.serialization.Serializable

class MainActivity : ComponentActivity() {
    private var permissionsGranted by mutableStateOf(false)
    private lateinit var database: ChatDatabase
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = Room.databaseBuilder(
            applicationContext,
            ChatDatabase::class.java, "chat_database"
        ).fallbackToDestructiveMigration().build()

        if (hasRequiredPermissions()) {
            permissionsGranted = true
            setContent {
                AmogusAppTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavigation()
                    }
                }
            }
        } else {
            permissionsGranted = false
            requestPermissions()
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

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this, CAMERAX_PERMISSIONS, 0
        )
    }

    companion object {
        private val CAMERAX_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE
        )
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @Composable
    fun AppNavigation() {
        val navController = rememberNavController()
        val fileViewModel: FileViewModel = viewModel(
            factory = FileViewModelFactory(database.archivoDao())
        )
        val fileState by fileViewModel.state.collectAsStateWithLifecycle()

        val matrixViewModel : MatrixViewModel = viewModel()
        val matrixState by matrixViewModel.state.collectAsStateWithLifecycle()
        NavHost(navController = navController, startDestination = MainScreen) {
            composable<SplashScreen> { SplashScreen(navController) }
            composable<MainScreen> {
                Broccolink(
                    navController, "", fileState,
                    fileViewModel::onEvent
                )
            }
            composable<Camera> {
                val cameraViewModel: CameraViewModel = viewModel()
                val cameraState by cameraViewModel.state.collectAsStateWithLifecycle()
                if (permissionsGranted) {
                    CameraScreen(navController, cameraState, cameraViewModel::onEvent)
                } else {
                    Broccolink(navController, "", fileState,
                        fileViewModel::onEvent
                    )
                }
            }
            composable<BlinkScreen> {
                val args = it.toRoute<BlinkScreen>()
                Broccolink(
                    navController, args.credentials, fileState,
                    fileViewModel::onEvent
                )
            }
            composable<Matrix> {
                MatrixLayout(navController, matrixState, matrixViewModel::onEvent)
            }
        }
    }
}

interface Screen
@Serializable
object SplashScreen : Screen

@Serializable
object MainScreen : Screen

@Serializable
object Camera : Screen

@Serializable
object Matrix : Screen

@Serializable
object Settings : Screen

@Serializable
data class BlinkScreen(val credentials: String) : Screen

/*
@Serializable
object SplashScreen

@Serializable
object MainScreen

@Serializable
object Camera

@Serializable
object Matrix

@Serializable
data class BlinkScreen(
    val credentials: String
)
*/

