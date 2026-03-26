package de.schlafgut.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import de.schlafgut.app.data.entity.UserSettingsEntity
import de.schlafgut.app.data.repository.SleepRepository
import de.schlafgut.app.ui.dashboard.DashboardScreen
import de.schlafgut.app.ui.logger.SleepLoggerScreen
import de.schlafgut.app.ui.navigation.Screen
import de.schlafgut.app.ui.navigation.bottomNavItems
import de.schlafgut.app.ui.onboarding.OnboardingScreen
import de.schlafgut.app.ui.settings.SettingsScreen
import de.schlafgut.app.ui.statistics.StatisticsScreen
import de.schlafgut.app.ui.theme.Night900
import de.schlafgut.app.ui.theme.SchlafGutTheme
import de.schlafgut.app.ui.theme.TextSecondary
import de.schlafgut.app.util.BiometricHelper
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var repository: SleepRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SchlafGutTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SchlafGutRoot(repository = repository, activity = this)
                }
            }
        }
    }
}

@Composable
fun SchlafGutRoot(
    repository: SleepRepository,
    activity: FragmentActivity
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var needsOnboarding by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val settings = repository.getSettingsOnce()
        needsOnboarding = !settings.onboardingCompleted
        isLocked = settings.appLockEnabled
        isLoading = false

        if (isLocked) {
            BiometricHelper.authenticate(
                activity = activity,
                onSuccess = { isLocked = false },
                onError = { authError = it }
            )
        }
    }

    when {
        isLoading -> {
            // Splash / loading
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("\uD83C\uDF19", style = MaterialTheme.typography.headlineLarge)
            }
        }
        isLocked -> {
            // Lock screen
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = authError ?: "Bitte entsperre die App",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )
            }
        }
        needsOnboarding -> {
            OnboardingScreen(
                onComplete = { name, latency ->
                    scope.launch {
                        repository.saveSettings(
                            UserSettingsEntity(
                                userName = name,
                                defaultSleepLatency = latency,
                                onboardingCompleted = true
                            )
                        )
                        needsOnboarding = false
                    }
                }
            )
        }
        else -> {
            SchlafGutAppContent()
        }
    }
}

@Composable
fun SchlafGutAppContent() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        Screen.Dashboard.route,
        Screen.Statistics.route,
        Screen.Settings.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Night900,
                    tonalElevation = 0.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = navBackStackEntry?.destination?.hierarchy?.any {
                            it.route == item.screen.route
                        } == true

                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (showBottomBar) {
                FloatingActionButton(
                    onClick = {
                        navController.navigate(Screen.sleepLoggerRoute())
                    },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Schlaf eintragen",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
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
                    onEntryClick = { entryId ->
                        navController.navigate(Screen.sleepLoggerRoute(entryId))
                    },
                    onViewAllClick = {
                        navController.navigate(Screen.Statistics.route)
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
