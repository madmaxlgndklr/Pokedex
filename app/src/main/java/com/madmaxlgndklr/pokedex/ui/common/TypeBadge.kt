package com.madmaxlgndklr.pokedex.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P
import com.madmaxlgndklr.pokedex.ui.theme.typeColor

@Composable
fun TypeBadge(type: String, modifier: Modifier = Modifier) {
    Text(
        text = type.uppercase(),
        fontFamily = PressStart2P,
        fontSize = 7.sp,
        color = PokedexCream,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(typeColor(type))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    )
}

@Preview
@Composable
private fun TypeBadgePreview() {
    TypeBadge("fire")
}
