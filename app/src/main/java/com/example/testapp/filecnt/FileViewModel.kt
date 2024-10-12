package com.example.testapp.filecnt

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.testapp.room.Archivo
import com.example.testapp.room.ArchivoDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URLEncoder

//Factory
class FileViewModelFactory(private val archivoDao: ArchivoDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FileViewModel(archivoDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
//ViewModel
class FileViewModel(private val archivoDao: ArchivoDao): ViewModel() {
    init {
        System.loadLibrary("connections_api")
    }

    private external fun fetchPageContent(rawRes: Int): String
    private external fun uploadFileToFTP(ftpUrl: String, filePath: String): Boolean
    private external fun nativeTestConnection(ftpUrl: String?): Boolean
    private external fun uploadChunkstoFTP(ftpUrls: Array<String>, files: Array<String>): Boolean

    private val _state = MutableStateFlow(FileState())
    val state  = _state.asStateFlow()

    override fun onCleared() {
        super.onCleared()
        _state.value = FileState()
    }

    fun onEvent(event: FileEvent) {
        when (event) {
            is FileEvent.SelectFile -> {
                val thumbnailBitmap = generateThumbnail(event.fileName)
                val thumbnailByteArray = thumbnailBitmap?.let { bitmapToByteArray(it) }

                val archivo = Archivo(
                    uri = event.fileName,
                    filename = event.fileName,
                    thumbnail = thumbnailByteArray
                )
                viewModelScope.launch {
                    archivoDao.insertArchivo(archivo)
                }
                selectFile(event.fileName)
            }
            is FileEvent.SendSelectedFiles -> {
                sendSelectedFiles()
            }
            is FileEvent.UpdateCurl -> {
                updateCurl(event.curl)
            }
            is FileEvent.InsertFileInDatabase -> {
                insertarArchivo(event.archivo)
            }
            is FileEvent.GetAllFiles -> {
                getAllFiles()
            }
            is FileEvent.DeleteFiletypeID -> {
                deleteFilebyID(event.archivoId)
            }
        }
    }

    private fun generateThumbnail(fileName: String): Bitmap? {
        // Lógica para generar una miniatura basada en el tipo de archivo
        // Ejemplo para imágenes:
        return BitmapFactory.decodeFile(fileName)?.let {
            Bitmap.createScaledBitmap(it, 100, 100, true) // Ajusta el tamaño de la miniatura según sea necesario
        }
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    private fun updateCurl(curl: String) {
        _state.update { it.copy(curl = curl) }
    }

    private fun selectFile(fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { state ->
                state.copy(
                    selectedFiles = state.selectedFiles.toMutableList().apply { add(fileName) }
                )
            }
        }
    }

    private fun getAllFiles() {
        viewModelScope.launch {
            archivoDao.getAllFiles().collect { files ->
                _state.update { it.copy(allFiles = files) }
            }
        }
    }

    private fun deleteFilebyID(archivoId: Long) {
        viewModelScope.launch {
            archivoDao.deleteArchivoById(archivoId)
        }
    }

    private fun sendSelectedFiles(){
        viewModelScope.launch(Dispatchers.IO) {
            val currentState = _state.value
            if (currentState.selectedFiles.isNotEmpty()) {
                connectAndUploadFile(currentState)
            }
        }
    }

    private fun connectAndUploadFile(fileModel: FileState) {
        fileModel.selectedFiles.forEach { file ->
            val encodedFileName = URLEncoder.encode(file.substringAfterLast("/"), "UTF-8").replace("+", "%20")
            val ftpUrl = "${fileModel.curl}$encodedFileName"
            val connected = uploadFileToFTP(ftpUrl, file)
            if (connected) {
                Log.d("URL, ", "URL $ftpUrl es correcto")
                Log.d("FTP connection", "Archivo $file enviado correctamente")
            } else {
                Log.d("FTP connection", "Error al enviar archivo $file")
            }
        }
    }

    private fun insertarArchivo(archivo: Archivo) {
        viewModelScope.launch {
            archivoDao.insertArchivo(archivo)
        }
    }
}
