package com.example.testapp.filecnt

import androidx.compose.ui.graphics.vector.ImageVector
import com.example.testapp.Screen
import com.example.testapp.room.Archivo
import kotlinx.serialization.Serializable
import java.io.File

//Model Itself
data class FileState(
    var selectedFiles: MutableList<String> = mutableListOf(),
    val curl: String = "ftp://usuario@12345",
    var allFiles: List<Archivo> = emptyList()
)

sealed class FileEvent {
    data class SelectFile(val fileName: String) : FileEvent()
    data object SendSelectedFiles : FileEvent()
    data class UpdateCurl(val curl: String) : FileEvent()
    data class InsertFileInDatabase(val archivo: Archivo) : FileEvent()
    data object GetAllFiles  : FileEvent()
    data class DeleteFiletypeID(val archivoId: Long) : FileEvent()
}

//Model For UI
data class NavigationItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val screen: Screen
)