package com.madmaxlgndklr.pokedex.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PokedexRed
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P

@Composable
fun StatBar(label: String, value: Int, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = label.uppercase().take(7).padEnd(7),
            fontFamily = PressStart2P,
            fontSize = 7.sp,
            color = PokedexCream,
            modifier = Modifier.width(60.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = value.toString().padStart(3),
            fontFamily = PressStart2P,
            fontSize = 7.sp,
            color = PokedexCream,
            modifier = Modifier.width(28.dp)
        )
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .weight(1f)
                .height(10.dp)
                .background(PokedexCream.copy(alpha = 0.2f))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(value / 255f)
                    .height(10.dp)
                    .background(PokedexRed)
            )
        }
    }
}

@Preview
@Composable
private fun StatBarPreview() {
    StatBar("HP", 78)
}
