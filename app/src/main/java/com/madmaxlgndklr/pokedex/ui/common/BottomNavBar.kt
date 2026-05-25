package com.madmaxlgndklr.pokedex.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madmaxlgndklr.pokedex.R
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P

enum class NavDestination { SEARCH, FULL_LIST, MY_COLLECTION }

@Composable
fun BottomNavBar(
    current: NavDestination,
    onNavigateSearch: () -> Unit,
    onNavigateFullList: () -> Unit,
    onNavigateMyCollection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavIcon(
            label = "SEARCH",
            isActive = current == NavDestination.SEARCH,
            onClick = onNavigateSearch
        )
        NavIcon(
            label = "FULL DEX",
            isActive = current == NavDestination.FULL_LIST,
            onClick = onNavigateFullList
        )
        NavIcon(
            label = "MY DEX",
            isActive = current == NavDestination.MY_COLLECTION,
            onClick = onNavigateMyCollection
        )
    }
}

@Composable
private fun NavIcon(label: String, isActive: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .alpha(if (isActive) 1f else 0.40f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = !isActive,
                onClick = onClick
            )
    ) {
        Image(
            painter = painterResource(R.drawable.pdex_icon),
            contentDescription = label,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontFamily = PressStart2P,
            fontSize = 6.sp,
            color = PokedexCream,
            textAlign = TextAlign.Center
        )
    }
}
