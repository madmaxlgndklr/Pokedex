package com.madmaxlgndklr.pokedex.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.madmaxlgndklr.pokedex.PokedexApplication
import com.madmaxlgndklr.pokedex.ui.detail.DetailScreen
import com.madmaxlgndklr.pokedex.ui.detail.PokemonDetailViewModel
import com.madmaxlgndklr.pokedex.ui.list.ListScreen
import com.madmaxlgndklr.pokedex.ui.list.PokemonListViewModel
import com.madmaxlgndklr.pokedex.ui.mycollection.MyCollectionScreen
import com.madmaxlgndklr.pokedex.ui.mycollection.MyCollectionViewModel
import com.madmaxlgndklr.pokedex.ui.search.SearchScreen
import com.madmaxlgndklr.pokedex.ui.search.SearchViewModel
import com.madmaxlgndklr.pokedex.ui.theme.PokedexCream
import com.madmaxlgndklr.pokedex.ui.theme.PokedexDarkRed
import com.madmaxlgndklr.pokedex.ui.theme.PressStart2P
import kotlinx.coroutines.launch

private object Routes {
    const val LIST = "list"
    const val SEARCH = "search"
    const val MY_COLLECTION = "my_collection"
    const val DETAIL = "detail/{pokemonId}"
    fun detail(id: Int) = "detail/$id"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val repo = (LocalContext.current.applicationContext as PokedexApplication).repository

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(PokedexDarkRed)
                    .padding(24.dp)
            ) {
                Spacer(Modifier.height(48.dp))
                DrawerItem("● POKÉDEX") {
                    navController.navigate(Routes.LIST) { launchSingleTop = true }
                    scope.launch { drawerState.close() }
                }
                Spacer(Modifier.height(24.dp))
                DrawerItem("◌ SEARCH") {
                    navController.navigate(Routes.SEARCH) { launchSingleTop = true }
                    scope.launch { drawerState.close() }
                }
                Spacer(Modifier.height(24.dp))
                DrawerItem("◌ MY POKÉDEX") {
                    navController.navigate(Routes.MY_COLLECTION) { launchSingleTop = true }
                    scope.launch { drawerState.close() }
                }
            }
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = Routes.LIST,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Routes.LIST) {
                val vm: PokemonListViewModel = viewModel(factory = PokemonListViewModel.factory(repo))
                ListScreen(vm) { id -> navController.navigate(Routes.detail(id)) }
            }
            composable(Routes.SEARCH) {
                val vm: SearchViewModel = viewModel(factory = SearchViewModel.factory(repo))
                SearchScreen(vm) { id -> navController.navigate(Routes.detail(id)) }
            }
            composable(Routes.MY_COLLECTION) {
                val vm: MyCollectionViewModel = viewModel(factory = MyCollectionViewModel.factory(repo))
                MyCollectionScreen(vm) { id -> navController.navigate(Routes.detail(id)) }
            }
            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument("pokemonId") { type = NavType.IntType })
            ) { backStackEntry ->
                val pokemonId = backStackEntry.arguments!!.getInt("pokemonId")
                val vm: PokemonDetailViewModel = viewModel(
                    key = "detail_$pokemonId",
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
}

@Composable
private fun DrawerItem(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        fontFamily = PressStart2P,
        fontSize = 10.sp,
        color = PokedexCream,
        modifier = Modifier.clickable(onClick = onClick)
    )
}
