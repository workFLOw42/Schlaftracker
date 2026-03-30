package de.schlafgut.app

import android.os.Bundle
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import de.schlafgut.app.ui.RootViewModel
import de.schlafgut.app.ui.allentries.AllEntriesScreen
import de.schlafgut.app.ui.dashboard.DashboardScreen
import de.schlafgut.app.ui.logger.SleepLoggerScreen
import de.schlafgut.app.ui.navigation.Screen
import de.schlafgut.app.ui.navigation.navigationItems
import de.schlafgut.app.ui.onboarding.OnboardingScreen
import de.schlafgut.app.ui.settings.SettingsScreen
import de.schlafgut.app.ui.statistics.StatisticsScreen
import de.schlafgut.app.ui.theme.SchlafGutTheme
import de.schlafgut.app.ui.theme.TextSecondary
import de.schlafgut.app.util.BiometricHelper
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SchlafGutTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SchlafGutRoot()
                }
            }
        }
    }
}

@Composable
fun SchlafGutRoot(
    viewModel: RootViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current as FragmentActivity

    LaunchedEffect(uiState.isLocked) {
        if (uiState.isLocked) {
            BiometricHelper.authenticate(
                activity = activity,
                onSuccess = { viewModel.onAuthSuccess() },
                onError = { viewModel.onAuthError(it) }
            )
        }
    }

    when {
        uiState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Text("\uD83C\uDF19", style = MaterialTheme.typography.headlineLarge)
            }
        }
        uiState.isLocked -> {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.authError ?: "Bitte entsperre die App",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )
            }
        }
        uiState.needsOnboarding -> {
            OnboardingScreen(
                onComplete = { name, latency ->
                    viewModel.completeOnboarding(name, latency)
                }
            )
        }
        else -> {
            SchlafGutAppContent()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchlafGutAppContent() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val showNav = currentRoute in listOf(
        Screen.Dashboard.route,
        Screen.AllEntries.route,
        Screen.Statistics.route,
        Screen.Settings.route
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = showNav,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                // Header – klickbar, navigiert zum Dashboard
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .clickable {
                            scope.launch { drawerState.close() }
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Dashboard.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_splash_logo),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "SchlafGut",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                navigationItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label) },
                        selected = currentRoute == item.screen.route,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(item.screen.route) {
                                popUpTo(Screen.Dashboard.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (showNav && currentRoute != Screen.Dashboard.route && currentRoute != Screen.AllEntries.route) {
                    TopAppBar(
                        title = {
                            val label = navigationItems.find { it.screen.route == currentRoute }?.label ?: "SchlafGut"
                            Text(label)
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menü")
                            }
                        }
                    )
                }
            },
            floatingActionButton = {
                if (showNav) {
                    FloatingActionButton(
                        onClick = {
                            navController.navigate(Screen.sleepLoggerRoute())
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Schlaf eintragen")
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Dashboard.route) {
                    DashboardScreen(
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onEntryClick = { entryId ->
                            navController.navigate(Screen.sleepLoggerRoute(entryId))
                        },
                        onViewAllClick = {
                            navController.navigate(Screen.AllEntries.route)
                        }
                    )
                }

                composable(Screen.AllEntries.route) {
                    AllEntriesScreen(
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onEntryClick = { entryId ->
                            navController.navigate(Screen.sleepLoggerRoute(entryId))
                        }
                    )
                }

                composable(
                    route = "sleep_logger?entryId={entryId}",
                    arguments = listOf(
                        navArgument("entryId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) {
                    SleepLoggerScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Statistics.route) {
                    StatisticsScreen()
                }

                composable(Screen.Settings.route) {
                    SettingsScreen()
                }
            }
        }
    }
}
