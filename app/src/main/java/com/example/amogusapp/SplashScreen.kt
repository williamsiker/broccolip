package com.example.amogusapp

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    val alpha = remember {
        Animatable(0f)
    }
    LaunchedEffect(key1 = true) {
        alpha.animateTo(1f, animationSpec = tween(1500))
        delay(2000)
        navController.navigate("mainScreen") {
            popUpTo("splashScreen") { inclusive = true } // Limpiar la pila de navegación
        }
    }
    Splash(alpha)
}

@Composable
fun Splash(alpha: Animatable<Float, AnimationVector1D>) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFDB58)), contentAlignment = Alignment.Center
    ) {
        // Fondo de la pantalla de splash
        Image(
            painter = painterResource(id = R.drawable.bbk_removebg_preview),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha.value)
        )

        // Contenido de la pantalla de splash
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "IP: 192.168.1.100\n" +
                        "L: 37.7749\n" +
                        "DNS:  森林里住着熊妈妈\n" +
                        "MAC: 1A:2B:3C:4D:5E:6F\n" +
                        "SN: ABC123XYZ456\n" +
                        "AC: 789DEF456ABC\n",
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Blue,
                textAlign = TextAlign.Center, // Alinear el texto al centro
                modifier = Modifier
                    .padding(top = 550.dp)
                    .alpha(alpha.value)
            )
        }

    }
}

@Preview(showBackground = true)
@Composable
fun SplashPreview() {
    val alpha = remember { Animatable(1f) }
    Splash(alpha)
}
