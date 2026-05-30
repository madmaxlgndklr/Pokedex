package com.madmaxlgndklr.pokedex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

// --- 1. DATA MODELS ---
data class PokemonResponse(val results: List<PokemonResult>)

data class PokemonResult(val name: String, val url: String) {
    // Extract the Pokémon's ID from the URL to get its image
    val imageUrl: String
        get() {
            val index = url.split("/".toRegex()).dropLast(1).last()
            return "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/$index.png"
        }
}

// --- 2. API SERVICE ---
interface PokeApi {
    // Fetch the original 151 Pokemon
    @GET("pokemon?limit=151")
    suspend fun getPokemon(): PokemonResponse
}

object RetrofitInstance {
    val api: PokeApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://madmaxlgndklrpokeapi.com/api/v2/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PokeApi::class.java)
    }
}

// --- 3. VIEWMODEL ---
class PokedexViewModel : ViewModel() {
    var pokemonList by mutableStateOf<List<PokemonResult>>(emptyList())
        private set

    init {
        fetchPokemon()
    }

    private fun fetchPokemon() {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.getPokemon()
                pokemonList = response.results
            } catch (e: Exception) {
                // In a production app, handle errors (like no internet) here
                e.printStackTrace()
            }
        }
    }
}

// --- 4. UI (JETPACK COMPOSE) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokedexApp(viewModel: PokedexViewModel = viewModel()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kanto Pokédex", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(contentPadding = paddingValues) {
            items(viewModel.pokemonList) { pokemon ->
                PokemonRow(pokemon)
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun PokemonRow(pokemon: PokemonResult) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = pokemon.imageUrl,
            contentDescription = "${pokemon.name} sprite",
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = pokemon.name.replaceFirstChar { it.uppercase() },
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// --- 5. MAIN ACTIVITY ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PokedexApp()
                }
            }
        }
    }
}