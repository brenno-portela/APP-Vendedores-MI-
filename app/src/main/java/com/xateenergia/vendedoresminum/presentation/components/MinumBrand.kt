package com.xateenergia.vendedoresminum.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.xateenergia.vendedoresminum.R

/** Elementos recorrentes da identidade visual Minum. */
@Composable
fun MinumLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.minum_logo),
        contentDescription = "Minum",
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

/** Linha de apoio oficial: verde Minum seguido do verde menta. */
@Composable
fun MinumAccentLine(modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        Box(
            modifier = Modifier
                .width(34.dp)
                .height(5.dp)
                .background(MaterialTheme.colorScheme.secondary)
        )
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(5.dp)
                .background(MaterialTheme.colorScheme.primaryContainer)
        )
    }
}
