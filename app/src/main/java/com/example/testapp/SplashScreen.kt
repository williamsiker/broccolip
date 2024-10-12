package com.example.testapp

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    val alpha = remember {
        Animatable(0f)
    }
    val alphaOut = remember { Animatable(1f) }
    var isExiting by remember { mutableStateOf(false) }
    LaunchedEffect(key1 = true) {
        alpha.animateTo(1f, animationSpec = tween(1500))
        delay(2000)

        isExiting = true
        alphaOut.animateTo(0f, animationSpec = tween(500))
        navController.navigate(BlinkScreen("")) {
            popUpTo<SplashScreen> {
                inclusive = true
            }
        }
    }
    Splash(alpha = if (isExiting) alphaOut else alpha)
}

@Composable
fun Splash(alpha: Animatable<Float, AnimationVector1D>) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.splash),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha.value)
        )
    }
}

@Preview
@Composable
fun SplashPreview() {
    val alpha = remember { Animatable(1f) }
    Splash(alpha)
}