package com.madmaxlgndklr.pokedex.ui.navigation

import android.media.MediaPlayer
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.madmaxlgndklr.pokedex.PokedexApplication
import com.madmaxlgndklr.pokedex.R
import com.madmaxlgndklr.pokedex.ui.common.SystemStatusBar
import com.madmaxlgndklr.pokedex.ui.detail.DetailScreen
import com.madmaxlgndklr.pokedex.ui.detail.PokemonDetailViewModel
import com.madmaxlgndklr.pokedex.ui.list.FullListScreen
import com.madmaxlgndklr.pokedex.ui.list.FullListViewModel
import com.madmaxlgndklr.pokedex.ui.mycollection.MyCollectionScreen
import com.madmaxlgndklr.pokedex.ui.mycollection.MyCollectionViewModel
import com.madmaxlgndklr.pokedex.ui.compare.CompareScreen
import com.madmaxlgndklr.pokedex.ui.compare.CompareViewModel
import com.madmaxlgndklr.pokedex.ui.search.SearchScreen
import com.madmaxlgndklr.pokedex.ui.search.SearchViewModel
import com.madmaxlgndklr.pokedex.ui.settings.SettingsScreen
import com.madmaxlgndklr.pokedex.ui.settings.SettingsViewModel
import com.madmaxlgndklr.pokedex.ui.team.TeamScreen
import com.madmaxlgndklr.pokedex.ui.team.TeamViewModel

private object Routes {
    const val SEARCH = "search"
    const val FULL_LIST = "full_list"
    const val MY_COLLECTION = "my_collection"
    const val SETTINGS = "settings"
    const val TEAM = "team"
    const val COMPARE = "compare/{firstId}"
    const val DETAIL = "detail/{pokemonId}"
    fun detail(id: Int) = "detail/$id"
    fun compare(id: Int) = "compare/$id"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val repo = (context.applicationContext as PokedexApplication).repository
    val settingsRepo = (context.applicationContext as PokedexApplication).settingsRepository

    val teamVm: TeamViewModel = viewModel(factory = TeamViewModel.factory(repo, settingsRepo))

    val scope = rememberCoroutineScope()
    var statusBarVisible by remember { mutableStateOf(false) }
    var isDetailLoading by remember { mutableStateOf(false) }

    var pokedexState by remember { mutableStateOf(PokedexState.OPEN) }

    var isMuted by remember { mutableStateOf(false) }
    val currentIsMuted by rememberUpdatedState(isMuted)

    // null = not yet loaded from DataStore; avoids starting music before we know the preference
    val musicOnLaunch: Boolean? by settingsRepo.musicOnLaunch.collectAsState(initial = null)

    val currentMusicOnLaunch by rememberUpdatedState(musicOnLaunch ?: true)

    val mediaPlayer = remember {
        MediaPlayer.create(context, R.raw.title_screen).apply { isLooping = true }
    }

    // Start or stop music whenever the launch preference changes
    LaunchedEffect(musicOnLaunch) {
        val pref = musicOnLaunch ?: return@LaunchedEffect
        if (pref && !isMuted && !mediaPlayer.isPlaying) mediaPlayer.start()
        else if (!pref && mediaPlayer.isPlaying) mediaPlayer.pause()
    }

    // Music control tied to Pokédex state:
    // - Silence immediately on close/closing.
    // - Resume at the START of the opening animation (not after it finishes),
    //   but only if the user has music enabled and hasn't muted.
    LaunchedEffect(pokedexState) {
        when (pokedexState) {
            PokedexState.CLOSING, PokedexState.CLOSED -> {
                if (mediaPlayer.isPlaying) mediaPlayer.pause()
            }
            PokedexState.OPENING -> {
                if (currentMusicOnLaunch && !currentIsMuted) {
                    mediaPlayer.seekTo(0)
                    mediaPlayer.start()
                }
            }
            PokedexState.OPEN -> {
                if (currentMusicOnLaunch && !currentIsMuted && !mediaPlayer.isPlaying)
                    mediaPlayer.start()
            }
        }
    }

    val activity = context as? ComponentActivity
    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME ->
                    if (currentMusicOnLaunch && !currentIsMuted && !mediaPlayer.isPlaying)
                        mediaPlayer.start()
                Lifecycle.Event.ON_PAUSE ->
                    if (mediaPlayer.isPlaying) mediaPlayer.pause()
                else -> {}
            }
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose {
            activity?.lifecycle?.removeObserver(observer)
            mediaPlayer.release()
        }
    }

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Routes.SEARCH,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Routes.SEARCH) {
                val vm: SearchViewModel = viewModel(factory = SearchViewModel.factory(repo, settingsRepo))
                SearchScreen(
                    viewModel = vm,
                    onPokemonClick = { id -> navController.navigate(Routes.detail(id)) },
                    onBack = { navController.popBackStack() },
                    onNavigateFullList = { navController.navigate(Routes.FULL_LIST) },
                    onNavigateMyCollection = { navController.navigate(Routes.MY_COLLECTION) },
                    onNavigateTeam = { navController.navigate(Routes.TEAM) },
                    onNavigateSettings = { navController.navigate(Routes.SETTINGS) },
                    onSyncNow = { navController.navigate(Routes.SETTINGS) },
                    onAnimationStarted = { statusBarVisible = false },
                    onAnimationEnded = {
                        navController.navigate(Routes.FULL_LIST) {
                            popUpTo(Routes.SEARCH) { inclusive = true }
                        }
                        scope.launch {
                            delay(500)
                            statusBarVisible = true
                        }
                    }
                )
            }
            composable(Routes.FULL_LIST) {
                val vm: FullListViewModel = viewModel(factory = FullListViewModel.factory(repo))
                FullListScreen(
                    viewModel = vm,
                    onPokemonClick = { id -> navController.navigate(Routes.detail(id)) },
                    onBack = { navController.popBackStack() },
                    onNavigateMyCollection = {
                        navController.navigate(Routes.MY_COLLECTION) {
                            popUpTo(Routes.FULL_LIST) { inclusive = true }
                        }
                    },
                    onNavigateTeam = { navController.navigate(Routes.TEAM) },
                    onNavigateSettings = { navController.navigate(Routes.SETTINGS) },
                    onClosePokedex = { pokedexState = PokedexState.CLOSING }
                )
            }
            composable(Routes.MY_COLLECTION) {
                val vm: MyCollectionViewModel = viewModel(factory = MyCollectionViewModel.factory(repo))
                MyCollectionScreen(
                    viewModel = vm,
                    onPokemonClick = { id -> navController.navigate(Routes.detail(id)) },
                    onBack = { navController.popBackStack() },
                    onNavigateFullList = {
                        navController.navigate(Routes.FULL_LIST) {
                            popUpTo(Routes.MY_COLLECTION) { inclusive = true }
                        }
                    },
                    onNavigateTeam = { navController.navigate(Routes.TEAM) },
                    onNavigateSettings = { navController.navigate(Routes.SETTINGS) },
                    onClosePokedex = { pokedexState = PokedexState.CLOSING }
                )
            }
            composable(Routes.TEAM) {
                TeamScreen(
                    viewModel = teamVm,
                    onBack = { navController.popBackStack() },
                    onPokemonClick = { id -> navController.navigate(Routes.detail(id)) },
                    onNavigateFullList = { navController.navigate(Routes.FULL_LIST) { popUpTo(Routes.TEAM) { inclusive = true } } },
                    onNavigateMyCollection = { navController.navigate(Routes.MY_COLLECTION) { popUpTo(Routes.TEAM) { inclusive = true } } },
                    onNavigateSettings = { navController.navigate(Routes.SETTINGS) },
                    onClosePokedex = { pokedexState = PokedexState.CLOSING }
                )
            }
            composable(
                route = Routes.COMPARE,
                arguments = listOf(navArgument("firstId") { type = NavType.IntType })
            ) { backStackEntry ->
                val firstId = backStackEntry.arguments?.getInt("firstId") ?: return@composable
                val vm: CompareViewModel = viewModel(
                    factory = CompareViewModel.factory(repo, firstId)
                )
                CompareScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.SETTINGS) {
                val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(settingsRepo, repo))
                SettingsScreen(
                    viewModel = vm,
                    isMuted = isMuted,
                    onToggleMute = {
                        isMuted = !isMuted
                        if (isMuted) mediaPlayer.pause()
                        else if (currentMusicOnLaunch && !mediaPlayer.isPlaying) mediaPlayer.start()
                    },
                    onBack = { navController.popBackStack() },
                    onNavigateFullList = {
                        navController.navigate(Routes.FULL_LIST) {
                            popUpTo(Routes.SETTINGS) { inclusive = true }
                        }
                    },
                    onNavigateMyCollection = {
                        navController.navigate(Routes.MY_COLLECTION) {
                            popUpTo(Routes.SETTINGS) { inclusive = true }
                        }
                    }
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
                val teamIds by teamVm.teamIds.collectAsState()
                DetailScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onNavigatePrev = {
                        if (pokemonId > 1) {
                            navController.navigate(Routes.detail(pokemonId - 1)) {
                                popUpTo(Routes.detail(pokemonId)) { inclusive = true }
                            }
                        }
                    },
                    onNavigateNext = {
                        if (pokemonId < 1025) {
                            navController.navigate(Routes.detail(pokemonId + 1)) {
                                popUpTo(Routes.detail(pokemonId)) { inclusive = true }
                            }
                        }
                    },
                    onLoadingChanged = { isDetailLoading = it },
                    onEvolutionClick = { id -> navController.navigate(Routes.detail(id)) },
                    isOnTeam = pokemonId in teamIds,
                    onToggleTeam = {
                        if (pokemonId in teamIds) teamVm.removeFromTeam(pokemonId)
                        else teamVm.addToTeam(pokemonId)
                    },
                    onCompare = { id -> navController.navigate(Routes.compare(id)) }
                )
            }
        }

        if (statusBarVisible && !isDetailLoading) {
            SystemStatusBar(
                isMuted = isMuted,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Full-screen overlay for close/open animations — sits on top of all nav content
        if (pokedexState != PokedexState.OPEN) {
            PokedexAnimOverlay(
                state = pokedexState,
                onClosingComplete = { pokedexState = PokedexState.CLOSED },
                onOpeningComplete = { pokedexState = PokedexState.OPEN },
                onPowerButtonTap  = { pokedexState = PokedexState.OPENING }
            )
        }
    }
}
