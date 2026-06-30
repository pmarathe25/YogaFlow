package com.example.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object FlowDetails : Screen("flow_details/{flowId}") {
        fun createRoute(flowId: String) = "flow_details/$flowId"
    }
    object Player : Screen("player")
    object SessionComplete : Screen("session_complete")
    object ExpandedDashboard : Screen("expanded_dashboard")
    object History : Screen("history")
    object Settings : Screen("settings")
}
