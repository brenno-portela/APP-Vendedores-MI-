package com.xateenergia.vendedoresminum.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.xateenergia.vendedoresminum.R
import com.xateenergia.vendedoresminum.presentation.theme.MinumColorTokens

/** Elementos recorrentes da identidade visual Minum. */
@Composable
fun MinumLogo(
    modifier: Modifier = Modifier,
    variant: MinumLogoVariant = MinumLogoVariant.OnLight
) {
    Image(
        painter = painterResource(id = R.drawable.minum_logo),
        contentDescription = "Minum",
        contentScale = ContentScale.Fit,
        colorFilter = when (variant) {
            MinumLogoVariant.OnLight -> null
            MinumLogoVariant.OnDark -> ColorFilter.tint(MinumColorTokens.Text.Inverse)
            MinumLogoVariant.Energy -> ColorFilter.tint(MinumColorTokens.Brand.Energy)
        },
        modifier = modifier
    )
}

enum class MinumLogoVariant { OnLight, OnDark, Energy }

/** Linha de apoio oficial: verde institucional seguido do verde claro. */
@Composable
fun MinumLine(
    modifier: Modifier = Modifier,
    primarySegmentColor: Color = MinumColorTokens.Brand.Primary,
    secondarySegmentColor: Color = MinumColorTokens.Brand.Light
) {
    Row(modifier = modifier) {
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(4.dp)
                .background(primarySegmentColor)
        )
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(4.dp)
                .background(secondarySegmentColor)
        )
    }
}

/** Mantem compatibilidade com as telas que ainda usam o nome anterior. */
@Composable
fun MinumAccentLine(modifier: Modifier = Modifier) = MinumLine(modifier)
