package com.madmaxlgndklr.pokedex.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.madmaxlgndklr.pokedex.PokedexApplication
import com.madmaxlgndklr.pokedex.ui.detail.DetailScreen
import com.madmaxlgndklr.pokedex.ui.detail.PokemonDetailViewModel
import com.madmaxlgndklr.pokedex.ui.list.FullListScreen
import com.madmaxlgndklr.pokedex.ui.list.FullListViewModel
import com.madmaxlgndklr.pokedex.ui.mycollection.MyCollectionScreen
import com.madmaxlgndklr.pokedex.ui.mycollection.MyCollectionViewModel
import com.madmaxlgndklr.pokedex.ui.search.SearchScreen
import com.madmaxlgndklr.pokedex.ui.search.SearchViewModel

private object Routes {
    const val SEARCH = "search"
    const val FULL_LIST = "full_list"
    const val MY_COLLECTION = "my_collection"
    const val DETAIL = "detail/{pokemonId}"
    fun detail(id: Int) = "detail/$id"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val repo = (LocalContext.current.applicationContext as PokedexApplication).repository

    NavHost(
        navController = navController,
        startDestination = Routes.SEARCH,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(Routes.SEARCH) {
            val vm: SearchViewModel = viewModel(factory = SearchViewModel.factory(repo))
            SearchScreen(
                viewModel = vm,
                onPokemonClick = { id -> navController.navigate(Routes.detail(id)) },
                onNavigateFullList = { navController.navigate(Routes.FULL_LIST) },
                onNavigateMyCollection = { navController.navigate(Routes.MY_COLLECTION) }
            )
        }
        composable(Routes.FULL_LIST) {
            val vm: FullListViewModel = viewModel(factory = FullListViewModel.factory(repo))
            FullListScreen(
                viewModel = vm,
                onPokemonClick = { id -> navController.navigate(Routes.detail(id)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.MY_COLLECTION) {
            val vm: MyCollectionViewModel = viewModel(factory = MyCollectionViewModel.factory(repo))
            MyCollectionScreen(
                viewModel = vm,
                onPokemonClick = { id -> navController.navigate(Routes.detail(id)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("pokemonId") { type = NavType.IntType })
        ) { backStackEntry ->
            val pokemonId = backStackEntry.arguments?.getInt("pokemonId") ?: return@composable
            val vm: PokemonDetailViewModel = viewModel(
                factory = PokemonDetailViewModel.factory(repo, pokemonId)
            )
            DetailScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onEvolutionClick = { id -> navController.navigate(Routes.detail(id)) }
            )
        }
    }
}
