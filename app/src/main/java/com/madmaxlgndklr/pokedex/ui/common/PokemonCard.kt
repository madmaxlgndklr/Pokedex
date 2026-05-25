package com.madmaxlgndklr.pokedex.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PokedexDarkRed
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P

@Composable
fun PokemonCard(
    id: Int,
    name: String,
    types: List<String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spriteUrl = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/$id.png"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(PokedexDarkRed)
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        AsyncImage(
            model = spriteUrl,
            contentDescription = "$name sprite",
            modifier = Modifier.size(80.dp)
        )
        Text(
            text = "#${id.toString().padStart(3, '0')}",
            fontFamily = PressStart2P,
            fontSize = 7.sp,
            color = PokedexCream.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = name.replaceFirstChar { it.uppercase() },
            fontFamily = PressStart2P,
            fontSize = 7.sp,
            color = PokedexCream,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            types.forEach { type -> TypeBadge(type) }
        }
    }
}
