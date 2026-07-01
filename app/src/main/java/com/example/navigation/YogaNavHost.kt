package com.example.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.game.ui.components.GameApp
import com.example.game.viewmodel.GameViewModel
import com.example.model.FlowLoader
import com.example.ui.screens.*
import com.example.viewmodel.YogaViewModel

@Composable
fun YogaNavHost(
    navController: NavHostController,
    viewModel: YogaViewModel,
    gameViewModel: GameViewModel,
    isCountdownActive: Boolean,
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
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
