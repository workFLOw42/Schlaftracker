package de.schlafgut.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dagger.hilt.android.AndroidEntryPoint
import de.schlafgut.app.ui.RootViewModel
import de.schlafgut.app.ui.allentries.AllEntriesScreen
import de.schlafgut.app.ui.dashboard.DashboardScreen
import de.schlafgut.app.ui.logger.SleepLoggerScreen
import de.schlafgut.app.ui.navigation.Screen
import de.schlafgut.app.ui.navigation.navigationItems
import de.schlafgut.app.ui.navigation.topLevelScreens
import de.schlafgut.app.ui.onboarding.OnboardingScreen
import de.schlafgut.app.ui.settings.SettingsScreen
import de.schlafgut.app.ui.statistics.StatisticsScreen
import de.schlafgut.app.ui.theme.SchlafGutTheme
import de.schlafgut.app.ui.theme.TextSecondary
import de.schlafgut.app.util.BiometricHelper
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    /**
     * Wechselt die Zeitzone während die App läuft, wird die UI neu aufgebaut, damit alle
     * Zeiten sofort in der neuen Zone angezeigt werden. ViewModels und der gespeicherte
     * Navigationszustand bleiben dabei erhalten.
     */
    private val timeZoneReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_TIMEZONE_CHANGED) recreate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        ContextCompat.registerReceiver(
            this,
            timeZoneReceiver,
            IntentFilter(Intent.ACTION_TIMEZONE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
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

    override fun onDestroy() {
        unregisterReceiver(timeZoneReceiver)
        super.onDestroy()
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
    // Navigation 3: Der Backstack ist eine einfache, beobachtbare Liste typsicherer Routen
    // und übersteht dank @Serializable auch Rotation und Prozess-Tod.
    val backStack = rememberNavBackStack(Screen.Dashboard)
    val currentScreen = backStack.lastOrNull() as? Screen ?: Screen.Dashboard
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val showNav = currentScreen in topLevelScreens

    /** Drawer-Ziel: Dashboard bleibt immer unten, darüber höchstens ein Top-Level-Ziel. */
    fun navigateTopLevel(screen: Screen) {
        backStack.clear()
        backStack.add(Screen.Dashboard)
        if (screen != Screen.Dashboard) backStack.add(screen)
    }

    fun openDrawer() {
        scope.launch { drawerState.open() }
    }

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
                            navigateTopLevel(Screen.Dashboard)
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
                        selected = currentScreen == item.screen,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navigateTopLevel(item.screen)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (showNav && currentScreen != Screen.Dashboard && currentScreen != Screen.AllEntries) {
                    TopAppBar(
                        title = {
                            val label = navigationItems.find { it.screen == currentScreen }?.label ?: "SchlafGut"
                            Text(label)
                        },
                        navigationIcon = {
                            IconButton(onClick = { openDrawer() }) {
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
                            backStack.add(Screen.SleepLogger())
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
            SchlafGutNavDisplay(
                backStack = backStack,
                onMenuClick = { openDrawer() },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun SchlafGutNavDisplay(
    backStack: NavBackStack<NavKey>,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val openEntry: (String?) -> Unit = { entryId -> backStack.add(Screen.SleepLogger(entryId)) }
    val goBack: () -> Unit = { if (backStack.size > 1) backStack.removeLastOrNull() }

    NavDisplay(
        backStack = backStack,
        onBack = goBack,
        modifier = modifier,
        // Eigener ViewModel-Store pro Eintrag: jeder SleepLogger bekommt ein frisches
        // ViewModel, das beim Verlassen wieder aufgeräumt wird.
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<Screen.Dashboard> {
                DashboardScreen(
                    onMenuClick = onMenuClick,
                    onEntryClick = { openEntry(it) },
                    onViewAllClick = { backStack.add(Screen.AllEntries) }
                )
            }
            entry<Screen.AllEntries> {
                AllEntriesScreen(
                    onMenuClick = onMenuClick,
                    onEntryClick = { openEntry(it) }
                )
            }
            entry<Screen.SleepLogger> { route ->
                SleepLoggerScreen(
                    entryId = route.entryId,
                    onNavigateBack = goBack
                )
            }
            entry<Screen.Statistics> { StatisticsScreen() }
            entry<Screen.Settings> { SettingsScreen() }
        }
    )
}
