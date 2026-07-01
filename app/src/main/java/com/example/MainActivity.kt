package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.YogaViewModel

import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.model.FlowLoader
import com.example.navigation.Screen
import com.example.game.persistence.DataLoader
import com.example.game.ui.components.GameApp
import com.example.game.viewmodel.GameViewModel
import com.example.ui.components.*
import com.example.ui.screens.*

class MainActivity : ComponentActivity() {
    private val flowIdState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        DataLoader.init(this)
        
        val flowId = intent?.getStringExtra("FLOW_ID")
        if (flowId != null) {
            flowIdState.value = flowId
        }

        setContent {
            val viewModel: YogaViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                "Dark" -> true
                "Light" -> false
                else -> isSystemInDarkTheme()
            }
            MyApplicationTheme(darkTheme = darkTheme) {
                YogaAppContent(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize(),
                    notificationFlowId = flowIdState.value,
                    onHandledNotification = { flowIdState.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val flowId = intent.getStringExtra("FLOW_ID")
        if (flowId != null) {
            flowIdState.value = flowId
        }
    }
}

@Composable
fun YogaAppContent(
    viewModel: YogaViewModel,
    gameViewModel: GameViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    modifier: Modifier = Modifier,
    notificationFlowId: String? = null,
    onHandledNotification: () -> Unit = {}
) {
    val isCountdownActive by viewModel.isCountdownActive.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isCompleted by viewModel.isSessionCompleted.collectAsState()
    val keepScreenAwake by viewModel.keepScreenAwake.collectAsState()

    val context = LocalContext.current
    val navController = rememberNavController()

    DisposableEffect(isPlaying, isCountdownActive, keepScreenAwake) {
        val activity = context as? android.app.Activity
        val shouldKeepAwake = (isPlaying || isCountdownActive) && keepScreenAwake
        if (activity != null && shouldKeepAwake) {
            activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            if (activity != null) {
                activity.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    LaunchedEffect(notificationFlowId) {
        if (notificationFlowId != null) {
            navController.navigate(Screen.FlowDetails.createRoute(notificationFlowId))
            onHandledNotification()
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isInPlayerFlow = currentRoute == Screen.Player.route
    val showBottomBar = !isInPlayerFlow
        && currentRoute != Screen.SessionComplete.route
        && currentRoute != Screen.ZenBattle.route

    FrostedGlassBackground(
        modifier = modifier.fillMaxSize()
    ) {
        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        modifier = Modifier.testTag("bottom_nav_bar")
                    ) {
                        val currentLevel by viewModel.currentLevel.collectAsState()

                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                            label = { Text("Dashboard") },
                            selected = currentRoute == Screen.Dashboard.route,
                            onClick = {
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.Dashboard.route) { inclusive = true }
                                }
                            },
                            modifier = Modifier.testTag("bottom_nav_dashboard")
                        )
                        NavigationBarItem(
                            icon = {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.secondary
                                                )
                                            ),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$currentLevel",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            },
                            label = { Text("Journey") },
                            selected = currentRoute == Screen.ExpandedDashboard.route,
                            onClick = {
                                navController.navigate(Screen.ExpandedDashboard.route) {
                                    popUpTo(Screen.Dashboard.route)
                                    launchSingleTop = true
                                }
                            },
                            modifier = Modifier.testTag("bottom_nav_journey")
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.History, contentDescription = "History") },
                            label = { Text("History") },
                            selected = currentRoute == Screen.History.route,
                            onClick = {
                                navController.navigate(Screen.History.route) {
                                    popUpTo(Screen.Dashboard.route)
                                    launchSingleTop = true
                                }
                            },
                            modifier = Modifier.testTag("bottom_nav_history")
                        )
                    }
                }
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                composable(Screen.Dashboard.route) {
                    YogaDashboardScreen(
                        viewModel = viewModel,
                        onViewFlowDetails = { flow ->
                            navController.navigate(Screen.FlowDetails.createRoute(flow.id))
                        },
                        onOpenSettings = {
                            navController.navigate(Screen.Settings.route)
                        }
                    )
                }

                composable(
                    route = Screen.FlowDetails.route,
                    arguments = listOf(navArgument("flowId") { type = NavType.StringType })
                ) { entry ->
                    val flowId = entry.arguments?.getString("flowId") ?: ""
                    val flow = FlowLoader.getFlowById(context, flowId)
                    if (flow != null) {
                        YogaFlowDetailsScreen(
                            flow = flow,
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onStartFlow = {
                                viewModel.selectFlow(flow)
                                viewModel.restartSession()
                                navController.navigate(Screen.Player.route) {
                                    popUpTo(Screen.Dashboard.route) { inclusive = false }
                                }
                            },
                            onSelectPose = { index ->
                                viewModel.selectFlow(flow)
                                viewModel.selectPoseDirectly(index)
                                navController.navigate(Screen.Player.route) {
                                    popUpTo(Screen.Dashboard.route) { inclusive = false }
                                }
                            }
                        )
                    }
                }

                composable(Screen.Player.route) {
                    BackHandler {
                        if (isCountdownActive) {
                            viewModel.cancelCountdown()
                        } else {
                            viewModel.resetForDashboard()
                        }
                        navController.popBackStack(Screen.Dashboard.route, inclusive = false)
                    }
                    when {
                        isCompleted -> SessionCompleteScreen(
                            viewModel = viewModel,
                            onDone = {
                                viewModel.resetForDashboard()
                                navController.popBackStack(Screen.Dashboard.route, inclusive = false)
                            }
                        )
                        isCountdownActive -> CountdownStartScreen(
                            viewModel = viewModel,
                            onCancel = {
                                viewModel.cancelCountdown()
                                navController.popBackStack(Screen.Dashboard.route, inclusive = false)
                            }
                        )
                        else -> YogaPlayerScreen(
                            viewModel = viewModel,
                            onExit = {
                                viewModel.resetForDashboard()
                                navController.popBackStack(Screen.Dashboard.route, inclusive = false)
                            }
                        )
                    }
                }

                composable(Screen.ExpandedDashboard.route) {
                    ExpandedDashboardScreen(
                        viewModel = viewModel,
                        gameViewModel = gameViewModel,
                        onBack = { navController.popBackStack() },
                        onNavigateToHistory = {
                            navController.navigate(Screen.History.route)
                        },
                        onOpenSettings = {
                            navController.navigate(Screen.Settings.route) {
                                popUpTo(Screen.Dashboard.route)
                            }
                        },
                        onNavigateToBattle = {
                            navController.navigate(Screen.ZenBattle.route) {
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable(Screen.History.route) {
                    PracticeHistoryScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onOpenSettings = {
                            navController.navigate(Screen.Settings.route) {
                                popUpTo(Screen.Dashboard.route)
                            }
                        }
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.ZenBattle.route) {
                    BackHandler { navController.popBackStack() }
                    GameApp(
                        viewModel = gameViewModel,
                        onExitHub = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
