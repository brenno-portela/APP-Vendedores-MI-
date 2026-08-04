package com.xateenergia.vendedoresminum.presentation.screens.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.xateenergia.vendedoresminum.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(700)
        onFinished()
    }

    // O wordmark e horizontal: esta caixa preserva a proporcao original e impede cortes laterais.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.minum_splash_logo),
            contentDescription = "Minum",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .aspectRatio(16f / 9f)
        )
    }
}
