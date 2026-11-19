package com.ourshelf.app.ui.navigation

import com.ourshelf.app.resources.Res
import com.ourshelf.app.resources.action_undo
import com.ourshelf.app.resources.completed_snackbar
import com.ourshelf.app.resources.detail_deleted
import com.ourshelf.app.resources.nav_lounge
import com.ourshelf.app.resources.nav_search
import com.ourshelf.app.resources.search_added_snackbar
import com.ourshelf.app.resources.search_open
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.ourshelf.app.ui.components.LocalNavAnimatedVisibilityScope
import com.ourshelf.app.ui.components.LocalSharedTransitionScope
import com.ourshelf.app.ui.motion.LocalMotionSettings
import com.ourshelf.app.ui.motion.MotionSettings
import com.ourshelf.app.ui.motion.rememberSystemAnimationsEnabled
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.koin.compose.koinInject
import com.ourshelf.app.ui.theme.OurshelfTheme
import com.ourshelf.app.ui.onboarding.OnboardingScreen
import com.ourshelf.app.ui.settings.SettingsSheet
import com.ourshelf.app.ui.shelf.ManageShelvesRoute as ManageShelvesScreenRoute
import com.ourshelf.app.ui.shelf.ShelfRoute as ShelfScreenRoute
import com.ourshelf.app.ui.detail.DetailRoute as DetailScreenRoute
import com.ourshelf.app.ui.lounge.LoungeRoute as LoungeScreenRoute
import com.ourshelf.app.ui.search.SearchRoute as SearchScreenRoute
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data object LoungeRoute

@Serializable
data object SearchRoute

/** [sharedKey]: posterin hangi raftan açıldığı; shared element geçişi bu anahtarla eşleşir. */
@Serializable
data class DetailRoute(val itemId: String, val sharedKey: String? = null)

@Serializable
data class ShelfRoute(val collectionId: String)

@Serializable
data object ManageShelvesRoute

private data class TopLevelDestination(val route: Any, val icon: ImageVector, val labelRes: StringResource)

private val topLevelDestinations = listOf(
    TopLevelDestination(LoungeRoute, Icons.Filled.Weekend, Res.string.nav_lounge),
    TopLevelDestination(SearchRoute, Icons.Filled.Search, Res.string.nav_search),
)

/** Android ve iOS'un ortak giriş noktası. Koin, platform tarafında başlatılmış olmalıdır. */
@Composable
fun OurshelfApp() {
    val imageLoader = koinInject<ImageLoader>()
    setSingletonImageLoaderFactory { imageLoader }
    OurshelfTheme {
        AppRoot()
    }
}

@Composable
private fun AppRoot(appViewModel: AppViewModel = koinViewModel()) {
    val preferences by appViewModel.preferences.collectAsStateWithLifecycle()
    val systemAnimations = rememberSystemAnimationsEnabled()

    val prefs = preferences
    if (prefs == null) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }
    val motion = MotionSettings(
        animationsEnabled = systemAnimations && prefs.motionEffectsEnabled,
        tiltEnabled = systemAnimations && prefs.motionEffectsEnabled,
        hapticsEnabled = prefs.hapticsEnabled,
    )
    var showSettings by rememberSaveable { mutableStateOf(false) }

    CompositionLocalProvider(LocalMotionSettings provides motion) {
        if (!prefs.onboardingCompleted) {
            OnboardingScreen(onFinish = appViewModel::completeOnboarding)
        } else {
            UnwindNavHost(appViewModel, onOpenSettings = { showSettings = true })
        }
        if (showSettings) {
            SettingsSheet(
                preferences = prefs,
                systemAnimationsEnabled = systemAnimations,
                onHapticsChange = appViewModel::setHapticsEnabled,
                onMotionChange = appViewModel::setMotionEffectsEnabled,
                onDismiss = { showSettings = false },
            )
        }
    }
}

/** Her hedefe, paylaşılan öğe geçişleri için kendi AnimatedVisibilityScope'unu sağlar. */
@Composable
private fun AnimatedContentScope.WithVisibilityScope(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this, content = content)
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun UnwindNavHost(appViewModel: AppViewModel, onOpenSettings: () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val showBottomBar = destination != null &&
        topLevelDestinations.any { destination.hasRoute(it.route::class) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    /** Metinler Compose kaynaklarından askıya alan çağrıyla okunur (Android Context gerekmez). */
    fun showSnackbar(
        message: StringResource,
        messageArg: String? = null,
        actionLabel: StringResource,
        onAction: () -> Unit,
    ) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val text = if (messageArg != null) getString(message, messageArg) else getString(message)
            val result = snackbarHostState.showSnackbar(
                message = text,
                actionLabel = getString(actionLabel),
                withDismissAction = true,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) onAction()
        }
    }

    val openItem: (String, String?) -> Unit = { id, key -> navController.navigate(DetailRoute(id, key)) }
    val openShelf: (String) -> Unit = { navController.navigate(ShelfRoute(it)) }

    Scaffold(
        // İç ekranlar kendi status bar boşluklarını yönetir.
        contentWindowInsets = WindowInsets(0),
        snackbarHost = {
            SnackbarHost(
                snackbarHostState,
                modifier = if (showBottomBar) Modifier else Modifier.windowInsetsPadding(WindowInsets.navigationBars),
            )
        },
        bottomBar = {
            if (showBottomBar) BottomBar(navController, destination)
        },
    ) { padding ->
        SharedTransitionLayout(Modifier.padding(padding).consumeWindowInsets(padding)) {
            CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                NavHost(navController = navController, startDestination = LoungeRoute) {
                    composable<LoungeRoute> {
                        WithVisibilityScope {
                            LoungeScreenRoute(
                                onOpenItem = openItem,
                                onOpenSearch = {
                                    navController.navigate(SearchRoute) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                    }
                                },
                                onOpenShelf = openShelf,
                                onManageShelves = { navController.navigate(ManageShelvesRoute) },
                                onOpenSettings = onOpenSettings,
                            )
                        }
                    }
                    composable<SearchRoute> {
                        WithVisibilityScope {
                            SearchScreenRoute(
                                onOpenItem = openItem,
                                onItemAdded = { id, title ->
                                    showSnackbar(
                                        message = Res.string.search_added_snackbar,
                                        messageArg = title,
                                        actionLabel = Res.string.search_open,
                                        onAction = { navController.navigate(DetailRoute(id)) },
                                    )
                                },
                            )
                        }
                    }
                    composable<DetailRoute> { entry ->
                        WithVisibilityScope {
                            DetailScreenRoute(
                                sharedKey = entry.toRoute<DetailRoute>().sharedKey,
                                onBack = { navController.popBackStack() },
                                onDeleted = { id, _ ->
                                    navController.popBackStack()
                                    showSnackbar(
                                        message = Res.string.detail_deleted,
                                        actionLabel = Res.string.action_undo,
                                        onAction = { appViewModel.restoreItem(id) },
                                    )
                                },
                                onCompleted = { undo ->
                                    showSnackbar(
                                        message = Res.string.completed_snackbar,
                                        actionLabel = Res.string.action_undo,
                                        onAction = undo,
                                    )
                                },
                            )
                        }
                    }
                    composable<ShelfRoute> {
                        WithVisibilityScope {
                            ShelfScreenRoute(onBack = { navController.popBackStack() }, onOpenItem = openItem)
                        }
                    }
                    composable<ManageShelvesRoute> {
                        WithVisibilityScope {
                            ManageShelvesScreenRoute(onBack = { navController.popBackStack() }, onOpenShelf = openShelf)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomBar(navController: NavHostController, destination: androidx.navigation.NavDestination?) {
    NavigationBar {
        topLevelDestinations.forEach { dest ->
            val selected = destination?.hasRoute(dest.route::class) == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(dest.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(dest.icon, contentDescription = null) },
                label = { Text(stringResource(dest.labelRes)) },
            )
        }
    }
}
