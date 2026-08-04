package com.xateenergia.vendedoresminum.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Fundamentos visuais compartilhados por todas as telas do aplicativo Minum. */
object MinumColorTokens {
    object Brand {
        val PrimaryDark = Color(0xFF00463A)
        val Primary = Color(0xFF009279)
        val Energy = Color(0xFF00D2AE)
        val Light = Color(0xFFA4E0CE)
        val Blue = Color(0xFF5889FB)
        val Yellow = Color(0xFFFDF083)
    }

    object Surface {
        val Default = Color(0xFFF2F4FA)
        val Subtle = Color(0xFFE7F2EE)
        val Elevated = Color.White
        val Inverse = Brand.PrimaryDark
    }

    object Text {
        val Primary = Color(0xFF12342F)
        val Secondary = Color(0xFF526761)
        val Muted = Color(0xFF71857F)
        val Inverse = Color.White
    }

    object Border {
        val Default = Color(0xFFD9E5E1)
        val Strong = Color(0xFF9DB5AD)
    }

    object Feedback {
        val Success = Brand.Primary
        val Warning = Color(0xFFA67800)
        val Error = Color(0xFFB9382F)
        val Info = Brand.Blue
    }
}

object MinumSpacing {
    val Xs = 4.dp
    val Sm = 8.dp
    val Md = 12.dp
    val Lg = 16.dp
    val Xl = 24.dp
    val Xxl = 32.dp
}

object MinumRadii {
    val Small = 6.dp
    val Medium = 8.dp
}
