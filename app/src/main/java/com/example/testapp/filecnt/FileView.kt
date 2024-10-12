package com.example.testapp.filecnt

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.AlignHorizontalLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Camera
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SettingsInputAntenna
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.testapp.BlinkScreen
import com.example.testapp.Camera
import com.example.testapp.Matrix
import com.example.testapp.R
import com.example.testapp.Settings
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@SuppressLint("ResourceAsColor")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Broccolink(
    navController: NavController,
    credentials: String,
    state: FileState,
    onEvent: (FileEvent) -> Unit
) {
    LaunchedEffect(credentials) {
        onEvent(FileEvent.UpdateCurl(credentials))
    }
    LaunchedEffect(Unit) {
        onEvent(FileEvent.GetAllFiles) // Llama al método GetAllFiles del ViewModel
    }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var isContextMenuVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var selectedItemIndex by rememberSaveable { mutableIntStateOf(0) }
    var expanded by remember { mutableStateOf(false) }
    var currentText by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var selectedThumbnail by remember { mutableStateOf<ByteArray?>(null) }

    val context = LocalContext.current
    val isConnected = credentials.isNotEmpty()
    val focusManager = LocalFocusManager.current
    //Navigation Drawer Items
    val navItems = listOf(
        NavigationItem(
            title = "Home",
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home,
            screen = BlinkScreen("amogus")
        ),
        NavigationItem(
            title = "Matrices",
            selectedIcon = ImageVector.vectorResource(id = R.drawable.matrix),
            unselectedIcon = ImageVector.vectorResource(id = R.drawable.matrix),
            screen = Matrix
        ),
        NavigationItem(
            title = "Settings",
            selectedIcon = Icons.Filled.Settings,
            unselectedIcon = Icons.Outlined.Settings,
            screen = Settings
        )
    )

    //Launcher for single file
    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri ->
            uriToFile(context, uri).let { _ ->
                onEvent(FileEvent.SelectFile(getFilePathFromUri(context, uri)))
            }
        }
    }
    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                navItems.forEachIndexed{index,item ->
                    NavigationDrawerItem(
                        label = { Text(item.title)},
                        selected = index == selectedItemIndex,
                        onClick = {
                            selectedItemIndex = index
                            navController.navigate(item.screen)
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        icon = {
                            Icon(imageVector = if(index==selectedItemIndex){ item.selectedIcon }else item.unselectedIcon
                                ,contentDescription = item.title
                            )
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        },
        drawerState = drawerState,
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Broccolink v2") },
                    modifier = Modifier.padding(16.dp),
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } }
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Outlined.AlignHorizontalLeft, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate(Camera) }) {
                            Icon(imageVector = Icons.Outlined.QrCodeScanner, contentDescription = "Scan QR code")
                        }
                        IconButton(onClick = { isContextMenuVisible = true }) {
                            Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = "More Options")
                        }
                        DropdownMenu(
                            expanded = isContextMenuVisible,
                            onDismissRequest = { isContextMenuVisible = false }
                        ) {
                            DropdownMenuItem(
                                onClick = { isContextMenuVisible = false },
                                text = { MenuItemContent("Search", Icons.Outlined.Search) }
                            )
                            DropdownMenuItem(
                                onClick = { isContextMenuVisible = false },
                                text = { MenuItemContent("Manual Connection", Icons.Outlined.SettingsInputAntenna) }
                            )
                            DropdownMenuItem(
                                onClick = { isContextMenuVisible = false },
                                text = { MenuItemContent("App Info", Icons.Outlined.Info) }
                            )
                            DropdownMenuItem(
                                onClick = { isContextMenuVisible = false },
                                text = { MenuItemContent("Fade Chat", Icons.Outlined.RemoveRedEye) }
                            )
                            DropdownMenuItem(
                                onClick = { isContextMenuVisible = false },
                                text = { MenuItemContent("Delete Chat", Icons.Outlined.Delete) }
                            )
                        }
                    }
                )
            },
            bottomBar = {
                BottomAppBar(
                    content = {
                        Row(
                            modifier = Modifier
                                .fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AnimatedVisibility(
                                visible = expanded,
                                enter = expandHorizontally(animationSpec = tween(durationMillis = 150)),
                                exit = shrinkHorizontally(animationSpec = tween(durationMillis = 150))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .verticalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { /*TODO*/ }, enabled = isConnected) {
                                        Icon(imageVector = Icons.Outlined.Camera, contentDescription = "Upload an image from Camera")
                                    }
                                    IconButton(onClick = { filePickerLauncher.launch("*/*") }, enabled = true) {
                                        Icon(imageVector = Icons.Outlined.AttachFile, contentDescription = "Attach single file")
                                    }
                                    IconButton(onClick = { /*TODO*/ }, enabled = isConnected) {
                                        Icon(imageVector = Icons.Outlined.FolderOpen, contentDescription = "Upload Multiple Files")
                                    }
                                }
                            }
                            TextField(
                                enabled = true,
                                value = currentText,
                                onValueChange = { currentText = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(4.dp),
                                placeholder = { Text("Type your message...") },
                                maxLines = Int.MAX_VALUE,
                                singleLine = false,
                                leadingIcon = {
                                    FloatingActionButton(
                                        onClick = { expanded = !expanded },
                                        shape = CircleShape,
                                        containerColor = Color.Black,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = if(expanded) Icons.Default.Remove else Icons.Default.Add,
                                            contentDescription = "Add",
                                            tint = Color.White
                                        )
                                    }
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { onEvent(FileEvent.SendSelectedFiles) },
                                        enabled = true
                                    ) {
                                        Icon(imageVector = Icons.Outlined.ArrowUpward, contentDescription = "Send")
                                    }
                                },
                                colors = TextFieldDefaults.colors(
                                    cursorColor = Color.White,
                                    focusedLeadingIconColor = Color.White,
                                ),
                                shape = RoundedCornerShape(topStart = 15.dp, topEnd = 15.dp)
                            )
                        }
                    },
                    modifier = Modifier.padding(16.dp)
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                focusManager.clearFocus() // Clear focus from any focused element
                            }
                        )
                    }
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    items(state.allFiles) { file ->
                        DeletableCard(
                            filename = file.filename.substringAfterLast("/"),
                            thumbnail = file.thumbnail,
                            onDelete = {
                                onEvent(FileEvent.DeleteFiletypeID(file.id)) // Puedes agregar lógica para eliminar también
                            },
                            onClick = {
                                // Maneja el clic en la tarjeta
                                selectedThumbnail = file.thumbnail
                                showDialog = true
                            }
                        )
                    }
                }
            }
            if (showDialog) {
                ThumbnailDialog(
                    thumbnail = selectedThumbnail,
                    onDismiss = { showDialog = false }
                )
            }
        }
    }
}

@Composable
fun ThumbnailDialog(thumbnail: ByteArray?, onDismiss: () -> Unit) {
    val bitmap = thumbnail?.let {
        val originalBitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
        // Redimensionar el bitmap para que se ajuste al tamaño del diálogo
        val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, 800, 600, true) // Ajusta el tamaño según sea necesario
        scaledBitmap.asImageBitmap()
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(200.dp)
                .height(200.dp)
                .clickable(onClick = onDismiss) // Cierra el diálogo al hacer clic en cualquier parte del Box
        ) {
            bitmap?.let {
                Image(
                    bitmap = it,
                    contentDescription = "Full Size Thumbnail",
                    modifier = Modifier
                        .width(200.dp)
                        .height(200.dp)
                        .clickable(onClick = onDismiss) // Cierra el diálogo al hacer clic en la imagen
                )
            }
        }
    }

}

@Composable
fun MenuItemContent(text: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        Icon(imageVector = icon, contentDescription = text)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text)
    }
}

@Composable
fun DeletableCard(
    filename: String,
    thumbnail: ByteArray?,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() }, // Mantén clickable para la animación de elevación
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.Gray, RoundedCornerShape(8.dp))
            ) {
                thumbnail?.let {
                    val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
                    Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize())
                } ?: run {
                    Icon(imageVector = Icons.Filled.Image, contentDescription = "No Thumbnail", modifier = Modifier.fillMaxSize())
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = filename, modifier = Modifier.weight(1f))
            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Delete")
            }
        }
    }
}

@SuppressLint("Recycle")
fun getFilePathFromUri(context: Context, uri: Uri): String {
    var fileName = ""
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            fileName = if (displayNameIndex >= 0) {
                cursor.getString(displayNameIndex) ?: "unknown_file"
            } else {
                "unknown_file"
            }
        } else {
            fileName = "unknown_file"
        }
    }
    val file = File(context.cacheDir, fileName)
    return file.absolutePath
}

@SuppressLint("Recycle")
fun uriToFile(context: Context, uri: Uri): String {
    val fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: return "Error: Unable to open file descriptor"
    val mimeType = context.contentResolver.getType(uri)
    Log.d("UriToFile", "URI: $uri, MIME Type: $mimeType")

    val extension = mimeType?.split("/")?.lastOrNull()
    val fileName = getFilePathFromUri(context, uri)
    val finalFileName = if (extension != null) { "$fileName.$extension" } else { fileName }

    Log.d("UriToFile", "Generated file name: $finalFileName")

    val file = File(context.cacheDir, finalFileName)

    return try {
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val outputStream = FileOutputStream(file)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        file.absolutePath // Return the path of the file
    } catch (e: Exception) {
        Log.e("UriToFile", "Error copying file", e)
        "Error: ${e.message}"
    } finally {
        fileDescriptor.close()
    }
}



