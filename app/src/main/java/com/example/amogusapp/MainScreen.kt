package com.example.amogusapp

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.NotInterested
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Textsms
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets


data class Connections (
    val osImgvector : ImageVector,
    val os : String,
    val device : String,
    var isOnsession : Boolean,
)

data class HistorialItem(
    val contenido: Any,
    var active : MutableState<Boolean> = mutableStateOf(false)
)

data class TabItem(
    val title : String,
    val unselectedIcon : ImageVector,
    val selectedIcon : ImageVector
)

class MainUI : ComponentActivity() {
    init {
        try {
            System.loadLibrary("connections_api")
        } catch (e: UnsatisfiedLinkError) {
            Log.e("LibraryLoadError", "No se pudo cargar la biblioteca nativa 'connections_api'", e)
        }
    }

    private external fun fetchPageContent() : String
    private external fun uploadFileToFTP(ftpUrl: String, filePath: String): Boolean
    private external fun nativeTestConnection(ftpUrl : String?) : Boolean

    private var ftpHost = ""
    private var ftpPort = 0
    private var ftpUser = ""
    private var ftpPasswd = ""

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun MainScreen(navController: NavController, stringQr : String?) {
        val tabItems = listOf(
            TabItem("Home", Icons.Outlined.Home, Icons.Filled.Home),
            TabItem("Recibidos", Icons.Outlined.Email, Icons.Filled.Email),
            TabItem("ML Logs", Icons.Outlined.Computer, Icons.Filled.Computer)
        )
        var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
        val pagerState = rememberPagerState { tabItems.size }
        var isVisible by remember {mutableStateOf(true)}
        var offsetY by remember { mutableFloatStateOf(0f) }
        val animatedOffset by animateFloatAsState(
            targetValue = if(isVisible) 0f else 200f,
            label = "tabrowScrollable"
        )

        var isContextMenuVisible by remember { mutableStateOf(false) }

        LaunchedEffect(selectedTabIndex) {
            pagerState.animateScrollToPage(selectedTabIndex)
        }
        LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
            if(!pagerState.isScrollInProgress) {
                selectedTabIndex = pagerState.currentPage
            }
        }

        Scaffold(
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .wrapContentSize(Alignment.TopEnd),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = {
                        navController.navigate("cameraScreen")
                    }) {
                        Icon(imageVector = Icons.Outlined.QrCodeScanner, contentDescription = "Qr Connection")
                    }
                    IconButton(onClick = { isContextMenuVisible = true }) {
                        Icon(imageVector = Icons.Outlined.Thunderstorm, contentDescription = "Manual Connections")
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
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        modifier = Modifier.offset(y = animatedOffset.dp)
                    ) {
                        tabItems.forEachIndexed { index, item ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = {
                                    selectedTabIndex = index
                                },
                                //text = { Text(item.title) },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTabIndex == index) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.title,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                isVisible = offsetY < -100f
                                offsetY = 0f
                            },
                            onVerticalDrag = { _, dragAmount ->
                                offsetY += dragAmount
                            }
                        )
                    }
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> HomeContent(
                            stringQr = stringQr
                        )
                        1 -> Recieveds()
                        2 -> ML_Logs()
                    }
                }
            }
        }
    }

    @Composable
    private fun HomeContent(
        stringQr: String?
    ) {
        var message by remember { mutableStateOf("") }
        val fileList = remember { mutableStateListOf<HistorialItem>() }
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        var isLoading by remember { mutableStateOf(false) }
        val filePickerLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let {
                val file = uriToFile(context, uri)
                Log.d("file", uri.toString())
                file?.let {
                    if (stringQr != null) {
                        connectAndUploadFile(it, scope, stringQr)
                    }
                    fileList.add(HistorialItem(contenido = file))
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(fileList) { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                            .clickable {
                                item.active.value = !item.active.value
                                Log.d(
                                    "SwitchClick",
                                    "\"Item ${item.contenido} seleccionado: ${item.active.value}\""
                                )
                            }
                    ) {
                        val imageModifier = Modifier
                            .size(48.dp)
                            .padding(end = 8.dp)
                        val image = when (item.contenido) {
                            is String -> Icons.Outlined.Textsms
                            is File -> Icons.Outlined.UploadFile
                            else -> Icons.Outlined.NotInterested
                        }
                        Icon(
                            imageVector = image,
                            contentDescription = "File icon",
                            modifier = imageModifier
                        )

                        Text(
                            text = when (val contenido = item.contenido) {
                                is String -> contenido
                                is File -> contenido.name
                                else -> "Desconocido"
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )

                        RadioButton(
                            selected = item.active.value,
                            onClick = {
                                item.active.value = !item.active.value
                                Log.e("SwitchClick", "Item ${item.contenido} seleccionado: ${item.active.value}")
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = nativeTestConnection(stringQr),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = message,
                        onValueChange = { message = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .heightIn(min = 56.dp, max = 200.dp)
                            .verticalScroll(rememberScrollState()),
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start),
                        label = { Text("Ingrese Texto Aquí") },
                        singleLine = false,
                        maxLines = 10,
                    )

                    IconButton(
                        onClick = {
                            filePickerLauncher.launch("*/*")
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AttachFile,
                            contentDescription = "Open Folder",
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            filePickerLauncher.launch("*/*")
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = "Open Folder",
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            isLoading = true
                            scope.launch {
                                try {
                                    if (message.isNotEmpty()) {
                                        sendMessage(message, this, URLEncoder.encode(stringQr, StandardCharsets.UTF_8.toString()))
                                        fileList.add(HistorialItem(contenido = message))
                                        message = ""
                                    } else {
                                        fileList.filter { it.active.value }.forEach { item ->
                                            when (val contenido = item.contenido) {
                                                is String -> sendMessage(contenido, this, URLEncoder.encode(
                                                    stringQr, StandardCharsets.UTF_8.toString()))
                                                is File -> connectAndUploadFile(contenido, this, URLEncoder.encode(
                                                    stringQr, StandardCharsets.UTF_8.toString()))
                                            }
                                            item.active.value = false
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("HomeContent", "Error sending message or file", e)
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = "Enviar Todo",
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }


    @Composable
    fun Recieveds() {
        var isStarted = false
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            //Imagen
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Computer,
                    contentDescription = "Start FTP Service",
                    modifier = Modifier.fillMaxSize()
                )
            }

            //Botton Start Service
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = {
                        if(!isStarted) {
                            isStarted = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(Color.Blue)
                ) {
                    if(!isStarted){
                        Text(text = "Start Service")
                    }else{
                        Text(text = "End Service")
                    }
                }
            }

            //Fila Credenciales
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "pageContent")
            }

            //Fila Logs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextField(
                    value = "logText",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    label = {Text("Logs")}
                )
            }
        }
    }

    @Composable
    fun ML_Logs() {
        val pageContent = fetchPageContent()
        Text(pageContent)
    }



    @SuppressLint("Range")
    fun getFileNameFromUri(context: Context, uri: Uri): String? {
        var fileName: String? = null
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            fileName = if (it.moveToFirst()) {
                val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex >= 0) {
                    cursor.getString(displayNameIndex)
                } else {
                    null
                }
            } else {
                null
            }
        }
        return fileName
    }


    @SuppressLint("Recycle")
    fun uriToFile(context: Context, uri: Uri): File? {
        val fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
        val mimeType = context.contentResolver.getType(uri)
        Log.d("UriToFile", "URI: $uri, MIME Type: $mimeType")

        val extension = mimeType?.split("/")?.lastOrNull()
        val fileName = getFileNameFromUri(context, uri) ?: "tempFile" // Obtiene el nombre de archivo

        // Añade la extensión si se pudo determinar
        val finalFileName = if (extension != null) {
            fileName
        } else {
            fileName // Si no hay extensión, usa el nombre base
        }

        Log.d("UriToFile", "Generated file name: $finalFileName")

        val file = File(context.cacheDir, finalFileName)

        try {
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            return file
        } catch (e: Exception) {
            Log.e("UriToFile", "Error copying file", e)
        } finally {
            fileDescriptor.close()
        }
        return null
    }

    private fun connectAndUploadFile(
        file: File,
        scope: CoroutineScope,
        stringQr : String
    ) {
        val regularExpression = "ftp://(\\S+):(\\S+)@(.*):(\\d+)".toRegex()
        val matchResult = regularExpression.find(stringQr)

        if (matchResult != null) {
            ftpUser = matchResult.groupValues[1]
            ftpPasswd = matchResult.groupValues[2]
            ftpHost = matchResult.groupValues[3]
            ftpPort = matchResult.groupValues[4].toInt()

            scope.launch(Dispatchers.IO) {
                val encodedFileName = URLEncoder.encode(file.name, "UTF-8").replace("+", "%20")
                val string = "$stringQr/$encodedFileName"
                val connected = uploadFileToFTP(string, file.absolutePath)
                if (connected) {
                    Log.d("FTP connection", "Successfully uploaded file")
                } else {
                    Log.d("FTP connection", "Failed to upload file")
                }
            }
        }else{
            Log.d("Cadenas", "Cadena no valida")
        }



        /*
        //Antigua Funcion KOTLIN CONNECT-UPLOAD FILE
        scope.launch(Dispatchers.IO) {
            val ftpClient = FTPClient()
            try {
                ftpClient.connect(ftpHost, ftpPort)
                ftpClient.login(ftpUser, ftpPasswd)
                ftpClient.enterLocalPassiveMode()
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE)

                if (!FTPReply.isPositiveCompletion(ftpClient.replyCode)) {
                    Log.e("FTP", "Failed to connect to FTP server")
                    return@launch
                }

                Log.d("FTP", "Connected to FTP server")

                val inputStream = FileInputStream(file)
                val remoteFileName = file.name

                val done = ftpClient.storeFile(remoteFileName, inputStream)
                inputStream.close()

                if (done) {
                    Log.d("FTP", "File uploaded successfully")
                } else {
                    Log.e("FTP", "Failed to upload file")
                }

                ftpClient.logout()
            } catch (ex: IOException) {
                Log.e("FTP", "Error: ${ex.message}", ex)
            } finally {
                if (ftpClient.isConnected) {
                    try {
                        ftpClient.disconnect()
                    } catch (ex: IOException) {
                        Log.e("FTP", "Error disconnecting: ${ex.message}", ex)
                    }
                }
            }
        }*/
    }

    private fun sendMessage(message: String, scope: CoroutineScope, stringQr: String) {
        val regularExpression = "ftp://(\\S+):(\\S+)@(.*):(\\d+)".toRegex()
        val matchResult = regularExpression.find(stringQr)

        if (matchResult != null) {
            val ftpHost = matchResult.groupValues[1]
            val ftpPort = matchResult.groupValues[2].toInt()
            val ftpUser = matchResult.groupValues[3]
            val ftpPasswd = matchResult.groupValues[4]

            scope.launch {
                var ftpClient: FTPClient? = null
                try {
                    ftpClient = FTPClient()
                    ftpClient.connect(ftpHost, ftpPort)
                    val loginSuccess = ftpClient.login(ftpUser, ftpPasswd)

                    if (!loginSuccess) {
                        Log.d("FTP", "Failed to login to FTP server")
                        return@launch
                    }

                    ftpClient.enterLocalPassiveMode()
                    ftpClient.setFileType(FTP.BINARY_FILE_TYPE)

                    if (!FTPReply.isPositiveCompletion(ftpClient.replyCode)) {
                        Log.d("FTP", "Failed to connect to FTP server")
                        return@launch
                    }

                    // Send message as a file with a temporary name
                    val temporaryFileName = "temp_message.txt"
                    val inputStream = message.byteInputStream()
                    val done = ftpClient.storeFile(temporaryFileName, inputStream)
                    inputStream.close()

                    if (done) {
                        Log.d("FTP", "Message uploaded successfully")
                    } else {
                        Log.d("FTP", "Failed to upload message")
                    }

                    ftpClient.logout()
                } catch (ex: IOException) {
                    Log.d("FTP", "Error: ${ex.message}", ex)
                } finally {
                    ftpClient?.disconnect()
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    val navController = rememberNavController()
    MainUI().MainScreen(navController, "amogus")
}

