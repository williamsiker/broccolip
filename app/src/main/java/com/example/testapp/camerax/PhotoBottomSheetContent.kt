package com.example.testapp.camerax

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException


@Composable
fun PhotoBottomSheetContent(
    imageUriList: List<Uri>,
    modifier: Modifier = Modifier
) {
    if(imageUriList.isEmpty()){
        Box(
            modifier = modifier
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("There are no photos yet")
        }
    }else {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalItemSpacing = 16.dp,
            contentPadding = PaddingValues(16.dp),
            modifier = modifier
        ) {
//            items(imageUriList) { uri ->
//                val scaledBitmap = loadBitmapFromUri(uri)
//                scaledBitmap?.let {
//                    Image(
//                        bitmap = it.asImageBitmap(),
//                        contentDescription = null,
//                        modifier = Modifier
//                            .clip(RoundedCornerShape(10.dp))
//                    )
//                }
//            }
        }
    }
}

@Composable
private fun loadBitmapFromUri(uri: Uri): Bitmap? {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uri) {
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    bitmap = BitmapFactory.decodeStream(inputStream)
                }
            } catch (e: IOException) {
                Log.e("loadBitmapFromUri", "Failed to load bitmap from URI: $uri", e)
            }
        }
    }

    return bitmap
}

private fun scaleBitmap(bitmap: Bitmap): Bitmap {
    val scaleFactor = 0.3f // Ejemplo: escalar a la mitad
    val scaledWidth = (bitmap.width * scaleFactor).toInt()
    val scaledHeight = (bitmap.height * scaleFactor).toInt()
    return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
}