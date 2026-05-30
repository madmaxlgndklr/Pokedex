package com.madmaxlgndklr.pokedex.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madmaxlgndklr.pokedex.ui.theme.CaughtGold
import com.madmaxlgndklr.pokedex.ui.theme.GlowBlue
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PokedexDark
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P
import com.madmaxlgndklr.pokedex.ui.theme.typeColor

val ALL_POKEMON_TYPES = listOf(
    "normal", "fire", "water", "electric", "grass", "ice", "fighting", "poison",
    "ground", "flying", "psychic", "bug", "rock", "ghost", "dragon", "dark", "steel", "fairy"
)

@Composable
fun TypeFilterDialog(
    selectedTypes: Set<String>,
    onToggle: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PokedexDark,
        title = {
            Text(
                "FILTER BY TYPE",
                fontFamily = PressStart2P,
                fontSize = 8.sp,
                color = CaughtGold
            )
        },
        text = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ALL_POKEMON_TYPES.forEach { type ->
                    val selected = type in selectedTypes
                    Box(
                        modifier = Modifier
                            .background(
                                typeColor(type).copy(alpha = if (selected) 1f else 0.35f),
                                RoundedCornerShape(4.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onToggle(type) }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = type.uppercase(),
                            fontFamily = PressStart2P,
                            fontSize = 6.sp,
                            color = PokedexCream.copy(alpha = if (selected) 1f else 0.55f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Text(
                "DONE",
                fontFamily = PressStart2P,
                fontSize = 7.sp,
                color = GlowBlue,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onDismiss() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        },
        dismissButton = {
            Text(
                "CLEAR",
                fontFamily = PressStart2P,
                fontSize = 7.sp,
                color = PokedexCream.copy(alpha = 0.6f),
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onClear() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    )
}
