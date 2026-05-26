package com.madmaxlgndklr.pokedex.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madmaxlgndklr.pokedex.ui.theme.CaughtGold
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PokedexDark
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P

@Composable
fun RegionFilterDialog(
    selectedGens: Set<Generation>,
    onToggle: (Generation) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PokedexDark,
        title = {
            Text(
                text = "FILTER BY REGION",
                fontFamily = PressStart2P,
                fontSize = 8.sp,
                color = CaughtGold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                HorizontalDivider(color = PokedexCream.copy(alpha = 0.15f))
                Generation.entries.forEach { gen ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onToggle(gen) }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = gen in selectedGens,
                            onCheckedChange = { onToggle(gen) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = CaughtGold,
                                uncheckedColor = PokedexCream.copy(alpha = 0.4f),
                                checkmarkColor = PokedexDark
                            )
                        )
                        Text(
                            text = gen.label,
                            fontFamily = PressStart2P,
                            fontSize = 7.sp,
                            color = if (gen in selectedGens) CaughtGold else PokedexCream
                        )
                    }
                }
            }
        },
        confirmButton = {
            Text(
                text = "DONE",
                fontFamily = PressStart2P,
                fontSize = 7.sp,
                color = CaughtGold,
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
                text = "CLEAR",
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
