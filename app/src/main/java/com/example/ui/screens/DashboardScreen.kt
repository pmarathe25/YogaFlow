package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.*
import com.example.viewmodel.YogaViewModel

@Composable
fun YogaDashboardScreen(
    viewModel: YogaViewModel,
    onViewFlowDetails: (com.example.model.YogaFlow) -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val allFlows = remember { com.example.model.FlowLoader.loadFlows(context) }
    val themeMode by viewModel.themeMode.collectAsState()
    val currentLevel by viewModel.currentLevel.collectAsState()
    val totalXp by viewModel.totalXp.collectAsState()
    val favoriteFlowIds by viewModel.favoriteFlowIds.collectAsState()
    val favoriteFlows = allFlows.filter { favoriteFlowIds.contains(it.id) }
    val beginnerFlows = allFlows.filter { it.difficulty.equals("Beginner", ignoreCase = true) }
    val intermediateFlows = allFlows.filter { it.difficulty.equals("Intermediate", ignoreCase = true) }
    val advancedFlows = allFlows.filter { it.difficulty.equals("Advanced", ignoreCase = true) }
    val allReminders by viewModel.allReminders.collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = "App Icon",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Text(
                        text = "Yoga Flow",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Settings IconButton in the top right corner
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("settings_top_right_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        // Favorites Section
        if (favoriteFlows.isNotEmpty()) {
            item {
                CategoryHeader(
                    title = "Favorite Practices",
                    subtitle = "Quick access to your preferred sequences",
                    badgeColor = Color(0xFFFFD700)
                )
            }
            items(favoriteFlows) { flowItem ->
                val hasActiveReminders = allReminders.any { it.flowId == flowItem.id }
                FlowCard(
                    flowItem = flowItem,
                    isFavorite = true,
                    hasActiveReminders = hasActiveReminders,
                    onToggleFavorite = { viewModel.toggleFavoriteFlow(flowItem.id) },
                    onViewFlowDetails = onViewFlowDetails
                )
            }
        }

        // Beginner Section
        if (beginnerFlows.isNotEmpty()) {
            item {
                CategoryHeader(
                    title = "Beginner Path",
                    subtitle = "Gentle foundations and restorative practices",
                    badgeColor = MaterialTheme.colorScheme.primary
                )
            }
            items(beginnerFlows) { flowItem ->
                val hasActiveReminders = allReminders.any { it.flowId == flowItem.id }
                FlowCard(
                    flowItem = flowItem,
                    isFavorite = favoriteFlowIds.contains(flowItem.id),
                    hasActiveReminders = hasActiveReminders,
                    onToggleFavorite = { viewModel.toggleFavoriteFlow(flowItem.id) },
                    onViewFlowDetails = onViewFlowDetails
                )
            }
        }

        // Intermediate Section
        if (intermediateFlows.isNotEmpty()) {
            item {
                CategoryHeader(
                    title = "Intermediate Path",
                    subtitle = "Build strength, balance, and flow synergy",
                    badgeColor = MaterialTheme.colorScheme.secondary
                )
            }
            items(intermediateFlows) { flowItem ->
                val hasActiveReminders = allReminders.any { it.flowId == flowItem.id }
                FlowCard(
                    flowItem = flowItem,
                    isFavorite = favoriteFlowIds.contains(flowItem.id),
                    hasActiveReminders = hasActiveReminders,
                    onToggleFavorite = { viewModel.toggleFavoriteFlow(flowItem.id) },
                    onViewFlowDetails = onViewFlowDetails
                )
            }
        }

        // Advanced Section
        if (advancedFlows.isNotEmpty()) {
            item {
                CategoryHeader(
                    title = "Advanced Path",
                    subtitle = "Master deep stability, power, and focus",
                    badgeColor = MaterialTheme.colorScheme.tertiary
                )
            }
            items(advancedFlows) { flowItem ->
                val hasActiveReminders = allReminders.any { it.flowId == flowItem.id }
                FlowCard(
                    flowItem = flowItem,
                    isFavorite = favoriteFlowIds.contains(flowItem.id),
                    hasActiveReminders = hasActiveReminders,
                    onToggleFavorite = { viewModel.toggleFavoriteFlow(flowItem.id) },
                    onViewFlowDetails = onViewFlowDetails
                )
            }
        }
    }
}
