package com.madmaxlgndklr.pokedex.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madmaxlgndklr.pokedex.model.EvolutionStage
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P

@Composable
fun EvolutionChain(
    stages: List<EvolutionStage>,
    onPokemonClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.horizontalScroll(rememberScrollState())
    ) {
        stages.forEachIndexed { index, stage ->
            if (index > 0) {
                Text("->", fontFamily = PressStart2P, fontSize = 12.sp, color = PokedexCream)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                stage.members.forEach { node ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable(onClickLabel = node.name) { onPokemonClick(node.id) }
                    ) {
                        PokemonImage(
                            id = node.id,
                            name = node.name,
                            contentDescription = "${node.name} sprite",
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = node.name.replaceFirstChar { it.uppercase() },
                            fontFamily = PressStart2P,
                            fontSize = 6.sp,
                            color = PokedexCream,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(64.dp)
                        )
                    }
                }
            }
        }
    }
}
