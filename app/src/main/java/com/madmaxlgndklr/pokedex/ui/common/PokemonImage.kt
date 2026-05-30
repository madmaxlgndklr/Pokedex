package com.madmaxlgndklr.pokedex.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.madmaxlgndklr.pokedex.data.remote.RetrofitClient

@Composable
fun PokemonImage(
    id: Int,
    name: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String = name
) {
    val mode = LocalSpriteMode.current
    var errored by remember(id, mode) { mutableStateOf(false) }
    val url = if (errored) RetrofitClient.spriteUrl(id)
              else RetrofitClient.spriteUrl(id, name, mode)

    AsyncImage(
        model = url,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
        onError = { errored = true }
    )
}
