package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.game.persistence.DataLoader
import com.example.game.viewmodel.GameViewModel
import com.example.navigation.Screen
import com.example.navigation.YogaNavHost
import com.example.ui.components.FrostedGlassBackground
import com.example.ui.components.YogaBottomBar
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.YogaViewModel

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
            val viewModel: YogaViewModel = viewModel()
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

    override fun onNewIntent(intent: Intent) {
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
    gameViewModel: GameViewModel = viewModel(),
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
                    YogaBottomBar(
                        navController = navController,
                        viewModel = viewModel,
                        currentRoute = currentRoute
                    )
                }
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            YogaNavHost(
                navController = navController,
                viewModel = viewModel,
                gameViewModel = gameViewModel,
                isCountdownActive = isCountdownActive,
                isCompleted = isCompleted,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
}
