package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.audio.AudioCueManager
import com.example.model.YogaFlow
import com.example.model.YogaPose
import com.example.ui.theme.*
import com.example.viewmodel.YogaViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val flowIdState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
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
    modifier: Modifier = Modifier,
    notificationFlowId: String? = null,
    onHandledNotification: () -> Unit = {}
) {
    val isCompleted by viewModel.isSessionCompleted.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isCountdownActive by viewModel.isCountdownActive.collectAsState()
    val keepScreenAwake by viewModel.keepScreenAwake.collectAsState()

    val context = LocalContext.current
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
    
    // We can track if player is active (even if paused, we want to stay in player view)
    var isPlayerViewActive by remember { mutableStateOf(false) }

    // If session is playing or completed, adjust view states
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            isPlayerViewActive = true
        }
    }

    LaunchedEffect(isCompleted) {
        if (isCompleted) {
            isPlayerViewActive = false
        }
    }

    // Active flow for detail screen
    var activeFlowForDetails by remember { mutableStateOf<com.example.model.YogaFlow?>(null) }
    var showExpandedDashboard by remember { mutableStateOf(false) }
    var showSettingsScreen by remember { mutableStateOf(false) }
    var showHistoryScreen by remember { mutableStateOf(false) }
    var showZenGardenScreen by remember { mutableStateOf(false) }

    LaunchedEffect(notificationFlowId) {
        if (notificationFlowId != null) {
            val flow = com.example.model.YogaFlowRepository.allFlows.find { it.id == notificationFlowId }
            if (flow != null) {
                showSettingsScreen = false
                showHistoryScreen = false
                showZenGardenScreen = false
                showExpandedDashboard = false
                isPlayerViewActive = false
                activeFlowForDetails = flow
            }
            onHandledNotification()
        }
    }

    FrostedGlassBackground(
        modifier = modifier.fillMaxSize()
    ) {
        val showBottomBar = !isPlayerViewActive && !isCompleted && activeFlowForDetails == null && !showSettingsScreen

        BackHandler(
            enabled = isCompleted || showSettingsScreen || isPlayerViewActive || showZenGardenScreen || activeFlowForDetails != null || showHistoryScreen || showExpandedDashboard
        ) {
            when {
                showSettingsScreen -> showSettingsScreen = false
                isCompleted -> {
                    viewModel.resetForDashboard()
                    isPlayerViewActive = false
                    activeFlowForDetails = null
                }
                isPlayerViewActive -> {
                    if (isCountdownActive) {
                        viewModel.cancelCountdown()
                    } else {
                        viewModel.resetForDashboard()
                    }
                    isPlayerViewActive = false
                }
                showZenGardenScreen -> showZenGardenScreen = false
                activeFlowForDetails != null -> activeFlowForDetails = null
                showHistoryScreen -> showHistoryScreen = false
                showExpandedDashboard -> showExpandedDashboard = false
            }
        }

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
                            selected = !showExpandedDashboard && !showHistoryScreen && !showSettingsScreen && !showZenGardenScreen,
                            onClick = {
                                showSettingsScreen = false
                                showHistoryScreen = false
                                showExpandedDashboard = false
                                showZenGardenScreen = false
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
                            selected = showExpandedDashboard && !showHistoryScreen && !showSettingsScreen && !showZenGardenScreen,
                            onClick = {
                                showSettingsScreen = false
                                showHistoryScreen = false
                                showExpandedDashboard = true
                                showZenGardenScreen = false
                            },
                            modifier = Modifier.testTag("bottom_nav_journey")
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.History, contentDescription = "History") },
                            label = { Text("History") },
                            selected = showHistoryScreen && !showExpandedDashboard && !showSettingsScreen && !showZenGardenScreen,
                            onClick = {
                                showSettingsScreen = false
                                showExpandedDashboard = false
                                showHistoryScreen = true
                                showZenGardenScreen = false
                            },
                            modifier = Modifier.testTag("bottom_nav_history")
                        )
                        NavigationBarItem(
                            icon = { Text("⚔️", fontSize = 20.sp) },
                            label = { Text("Zen Battle") },
                            selected = showZenGardenScreen && !showExpandedDashboard && !showHistoryScreen && !showSettingsScreen,
                            onClick = {
                                showSettingsScreen = false
                                showExpandedDashboard = false
                                showHistoryScreen = false
                                showZenGardenScreen = true
                            },
                            modifier = Modifier.testTag("bottom_nav_garden")
                        )
                    }
                }
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when {
                    showSettingsScreen -> {
                        SettingsScreen(
                            viewModel = viewModel,
                            onBack = { showSettingsScreen = false }
                        )
                    }
                    isCompleted -> {
                        SessionCompleteScreen(
                            viewModel = viewModel,
                            onDone = {
                                viewModel.resetForDashboard()
                                isPlayerViewActive = false
                                activeFlowForDetails = null
                            }
                        )
                    }
                    isPlayerViewActive -> {
                        val isCountdownActive by viewModel.isCountdownActive.collectAsState()
                        if (isCountdownActive) {
                            CountdownStartScreen(
                                viewModel = viewModel,
                                onCancel = {
                                    viewModel.cancelCountdown()
                                    isPlayerViewActive = false
                                }
                            )
                        } else {
                            YogaPlayerScreen(
                                viewModel = viewModel,
                                onExit = {
                                    viewModel.resetForDashboard()
                                    isPlayerViewActive = false
                                }
                            )
                        }
                    }
                    showZenGardenScreen -> {
                        ZenGardenScreen(
                            viewModel = viewModel,
                            onNavigateBack = { showZenGardenScreen = false }
                        )
                    }
                    activeFlowForDetails != null -> {
                        YogaFlowDetailsScreen(
                            flow = activeFlowForDetails!!,
                            viewModel = viewModel,
                            onBack = {
                                activeFlowForDetails = null
                            },
                            onStartFlow = {
                                viewModel.selectFlow(activeFlowForDetails!!)
                                viewModel.restartSession()
                                isPlayerViewActive = true
                            },
                            onSelectPose = { index ->
                                viewModel.selectFlow(activeFlowForDetails!!)
                                viewModel.selectPoseDirectly(index)
                                isPlayerViewActive = true
                            }
                        )
                    }
                    else -> {
                        if (showHistoryScreen) {
                            PracticeHistoryScreen(
                                viewModel = viewModel,
                                onBack = { showHistoryScreen = false },
                                onOpenSettings = {
                                    showSettingsScreen = true
                                    showHistoryScreen = false
                                    showExpandedDashboard = false
                                }
                            )
                        } else if (showExpandedDashboard) {
                            ExpandedDashboardScreen(
                                viewModel = viewModel,
                                onBack = { showExpandedDashboard = false },
                                onNavigateToHistory = { showHistoryScreen = true },
                                onOpenSettings = {
                                    showSettingsScreen = true
                                    showHistoryScreen = false
                                    showExpandedDashboard = false
                                }
                            )
                        } else {
                            YogaDashboardScreen(
                                viewModel = viewModel,
                                onViewFlowDetails = { flow ->
                                    activeFlowForDetails = flow
                                },
                                onOpenSettings = {
                                    showSettingsScreen = true
                                    showHistoryScreen = false
                                    showExpandedDashboard = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun YogaDashboardScreen(
    viewModel: YogaViewModel,
    onViewFlowDetails: (com.example.model.YogaFlow) -> Unit,
    onOpenSettings: () -> Unit
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val currentLevel by viewModel.currentLevel.collectAsState()
    val totalXp by viewModel.totalXp.collectAsState()

    val allFlows = com.example.model.YogaFlowRepository.allFlows
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

@Composable
fun CategoryHeader(
    title: String,
    subtitle: String,
    badgeColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp, 20.dp)
                    .background(color = badgeColor, shape = RoundedCornerShape(3.dp))
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 14.dp, top = 2.dp)
        )
    }
}

fun getColorForFlow(flowId: String): Color {
    return when (flowId) {
        "sun_salutation" -> Color(0xFFFFB74D)          // Amber/Orange
        "warrior_flow" -> Color(0xFFE57373)            // Light Red/Coral
        "restorative_yin" -> Color(0xFF64B5F6)          // Light Blue
        "morning_energizer" -> Color(0xFFFFF176)        // Yellow
        "bedtime_wind_down" -> Color(0xFF9575CD)        // Purple
        "core_balance" -> Color(0xFF4DB6AC)            // Teal
        "heart_opening_vinyasa" -> Color(0xFFF06292)   // Pink
        "power_vinyasa_ascent" -> Color(0xFFFF8A65)    // Deep Orange
        "ashtanga_core_power" -> Color(0xFFAED581)     // Light Green
        "advanced_balance_mastery" -> Color(0xFF4DD0E1) // Cyan
        else -> Color(0xFF81C784)                       // Default Green
    }
}

@Composable
fun FlowCard(
    flowItem: com.example.model.YogaFlow,
    isFavorite: Boolean,
    hasActiveReminders: Boolean = false,
    onToggleFavorite: () -> Unit,
    onViewFlowDetails: (com.example.model.YogaFlow) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onViewFlowDetails(flowItem) }
            .testTag("flow_library_card_${flowItem.id}"),
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Left color-coded vertical indicator bar
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(
                        color = getColorForFlow(flowItem.id),
                        shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
                    )
            )
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = flowItem.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("favorite_btn_${flowItem.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (isFavorite) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                if (hasActiveReminders) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Active Reminders",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Reminders active",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = flowItem.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FlowLibraryBadge(label = "${flowItem.poses.size} Poses")
                        FlowLibraryBadge(label = "${flowItem.totalDurationMinutes} Min")
                    }
                    
                    Button(
                        onClick = { onViewFlowDetails(flowItem) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                        ),
                        modifier = Modifier.testTag("view_flow_btn_${flowItem.id}")
                    ) {
                        Text("Practice", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FlowLibraryBadge(label: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun YogaFlowDetailsScreen(
    flow: com.example.model.YogaFlow,
    viewModel: YogaViewModel,
    onBack: () -> Unit,
    onStartFlow: () -> Unit,
    onSelectPose: (Int) -> Unit
) {
    val isVoiceEnabled by viewModel.isVoiceEnabled.collectAsState()
    val favoriteFlowIds by viewModel.favoriteFlowIds.collectAsState()
    val isFavorite = favoriteFlowIds.contains(flow.id)
    var expandedPoseIndex by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Navigation Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("details_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Library",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Flow Details",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.weight(1f))
                
                val context = LocalContext.current
                var showRemindersListDialog by remember { mutableStateOf(false) }
                var showAddEditDialog by remember { mutableStateOf(false) }
                var editingReminder by remember { mutableStateOf<com.example.db.ReminderEntity?>(null) }
                val coroutineScope = rememberCoroutineScope()

                val allReminders by viewModel.allReminders.collectAsState(initial = emptyList())
                val flowReminders = allReminders.filter { it.flowId == flow.id }
                val hasReminder = flowReminders.isNotEmpty()
                
                if (showRemindersListDialog) {
                    FlowRemindersListDialog(
                        flowName = flow.name,
                        flowId = flow.id,
                        reminders = flowReminders,
                        onDismiss = { showRemindersListDialog = false },
                        onAdd = {
                            editingReminder = null
                            showAddEditDialog = true
                        },
                        onEdit = { reminder ->
                            editingReminder = reminder
                            showAddEditDialog = true
                        },
                        onDelete = { reminder ->
                            coroutineScope.launch {
                                com.example.db.ReminderManager.cancelAlarm(context, reminder.id)
                                viewModel.deleteReminder(reminder)
                            }
                        }
                    )
                }

                if (showAddEditDialog) {
                    val initialHour = editingReminder?.hour ?: 8
                    val initialMinute = editingReminder?.minute ?: 0
                    val initialDays = editingReminder?.daysOfWeek?.split(",")?.filter { it.isNotEmpty() }?.mapNotNull { it.toIntOrNull() }?.toSet() ?: setOf()
                    
                    ReminderDialog(
                        flowName = flow.name,
                        flowId = flow.id,
                        initialHour = initialHour,
                        initialMinute = initialMinute,
                        initialDays = initialDays,
                        onDismiss = { showAddEditDialog = false },
                        onSave = { hour, minute, days ->
                            val daysStr = days.joinToString(",")
                            if (editingReminder != null) {
                                val updatedReminder = editingReminder!!.copy(hour = hour, minute = minute, daysOfWeek = daysStr)
                                viewModel.updateReminder(updatedReminder)
                                android.widget.Toast.makeText(context, "Reminder updated", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                val reminder = com.example.db.ReminderEntity(
                                    flowId = flow.id,
                                    flowName = flow.name,
                                    hour = hour,
                                    minute = minute,
                                    daysOfWeek = daysStr
                                )
                                viewModel.addReminder(reminder) { success ->
                                    if (success) {
                                        android.widget.Toast.makeText(context, "Reminder saved", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        android.widget.Toast.makeText(context, "A matching reminder already exists", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                            showAddEditDialog = false
                        }
                    )
                }

                IconButton(
                    onClick = {
                        viewModel.toggleFavoriteFlow(flow.id)
                    },
                    modifier = Modifier.testTag("details_favorite_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (isFavorite) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            android.widget.Toast.makeText(context, "Please enable notification permissions", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        showRemindersListDialog = true
                    }
                ) {
                    Icon(
                        imageVector = if (hasReminder) Icons.Filled.Notifications else Icons.Outlined.NotificationAdd,
                        contentDescription = "Set Reminder",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Hero Details Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("details_hero_card"),
                shape = RoundedCornerShape(24.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "SEQUENCE DETAILS",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = flow.name,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = flow.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FlowStatBadge(icon = Icons.Default.Info, label = "${flow.poses.size} Poses")
                        FlowStatBadge(icon = Icons.Default.Info, label = "${flow.totalDurationMinutes} Min")
                        FlowStatBadge(icon = Icons.Default.Info, label = flow.difficulty)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onStartFlow()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("details_start_flow_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Begin Session",
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Start",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // Settings / Options Card removed, moved to Settings Screen

        // Pose List Sequence Section Header
        item {
            Text(
                text = "Flow Sequence (${flow.poses.size} Poses)",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Poses List for selected flow
        itemsIndexed(flow.poses) { index, pose ->
            val isExpanded = expandedPoseIndex == index
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        expandedPoseIndex = if (isExpanded) null else index
                    }
                    .testTag("details_pose_item_$index"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = pose.sanskritName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = pose.englishName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column(
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Left side: Pose Visual (Guide)
                                YogaPoseVisual(
                                    poseId = pose.id,
                                    modifier = Modifier
                                        .size(110.dp)
                                        .align(Alignment.Top)
                                )
                                
                                // Right side: Benefits & Steps
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "BENEFITS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = pose.benefits,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "INSTRUCTIONS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    pose.instructions.forEach { step ->
                                        Row(
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "• ",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = step,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FlowStatBadge(icon: ImageVector, label: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun YogaPlayerScreen(
    viewModel: YogaViewModel,
    onExit: () -> Unit
) {
    val flow by viewModel.flow.collectAsState()
    val currentPoseIndex by viewModel.currentPoseIndex.collectAsState()
    val currentPose by viewModel.currentPose.collectAsState()
    val remainingTimeSec by viewModel.remainingTimeSec.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val speechState by viewModel.speechState.collectAsState()

    if (currentPose == null) return

    val pose = currentPose!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Immersive Player Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onExit,
                modifier = Modifier.testTag("player_exit_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Exit to Dashboard",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Pose ${currentPoseIndex + 1} of ${flow.poses.size}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            // Empty placeholder for centering balance
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large Sanskrit and English Headers
        Text(
            text = pose.sanskritName,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp
            ),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = pose.englishName,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Center Content: Front and Side Angles Side-by-Side Sketchpads
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Front Angle Sketchpad
            YogaPoseVisual(
                poseId = pose.id,
                angle = PoseAngle.FRONT,
                modifier = Modifier
                    .weight(1f)
                    .height(170.dp)
            )

            // Side Angle Sketchpad
            YogaPoseVisual(
                poseId = pose.id,
                angle = PoseAngle.SIDE,
                modifier = Modifier
                    .weight(1f)
                    .height(170.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Large Timer and Player Navigation Panel (Combined for ergonomic efficiency)
        val primaryColor = MaterialTheme.colorScheme.primary
        val secondaryColor = MaterialTheme.colorScheme.primaryContainer
        val isDarkTheme = MaterialTheme.colorScheme.background == DarkBackground
        val glassBorder = if (isDarkTheme) GlassBorderDark else GlassBorderLight

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Pose Button
            FilledTonalIconButton(
                onClick = { viewModel.skipBackward() },
                modifier = Modifier
                    .size(56.dp)
                    .testTag("skip_prev_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous Pose",
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(28.dp))

            // Centered Circular Countdown Timer which is also the Play/Pause Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(130.dp) // slightly larger for excellent touch target
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = CircleShape
                    )
                    .border(
                        width = 1.5.dp,
                        color = glassBorder,
                        shape = CircleShape
                    )
                    .clickable { viewModel.togglePlay() }
                    .drawBehind {
                        // Draw outer circular track
                        drawCircle(
                            color = secondaryColor.copy(alpha = 0.4f),
                            radius = size.minDimension / 2 - 8.dp.toPx(),
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                        // Draw active countdown arc
                        val sweepAngle = (remainingTimeSec.toFloat() / 30f) * 360f
                        drawArc(
                            color = primaryColor,
                            startAngle = -90f,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = androidx.compose.ui.geometry.Offset(8.dp.toPx(), 8.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(size.width - 16.dp.toPx(), size.height - 16.dp.toPx()),
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    .testTag("countdown_circle")
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = primaryColor,
                        modifier = Modifier.size(28.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${remainingTimeSec}s",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (isPlaying) "HOLD POSE" else "PAUSED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp
                        ),
                        color = if (isPlaying) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f) else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(28.dp))

            // Next Pose Button
            FilledTonalIconButton(
                onClick = { viewModel.skipForward() },
                modifier = Modifier
                    .size(56.dp)
                    .testTag("skip_next_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next Pose",
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Scrolling Instructions and Benefits Card in high-fidelity GlassCard
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "HOW TO HOLD IT",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                pose.instructions.forEachIndexed { stepIdx, step ->
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text(
                            text = "${stepIdx + 1}. ",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "KEY BENEFITS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = pose.benefits,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { viewModel.restartSession() },
                modifier = Modifier.testTag("restart_session_button")
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Restart Practice", fontWeight = FontWeight.SemiBold)
            }

            val isMusicMuted by viewModel.isMusicMuted.collectAsState()
            val currentTrackIndex by viewModel.currentTrackIndex.collectAsState()
            val tracks = viewModel.ambientMusicManager.tracks
            val activeTrackName = if (currentTrackIndex in tracks.indices) tracks[currentTrackIndex].name else "Calm Sound"

            TextButton(
                onClick = { viewModel.toggleMusicMute() }
            ) {
                Icon(
                    imageVector = if (isMusicMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isMusicMuted) "Muted" else activeTrackName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun PauseIcon(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    Box(
        modifier = modifier
            .drawBehind {
                val strokeWidth = 5.dp.toPx()
                // Draw first vertical bar
                drawRect(
                    color = tint,
                    topLeft = androidx.compose.ui.geometry.Offset(x = size.width * 0.32f - strokeWidth / 2, y = size.height * 0.25f),
                    size = androidx.compose.ui.geometry.Size(width = strokeWidth, height = size.height * 0.5f)
                )
                // Draw second vertical bar
                drawRect(
                    color = tint,
                    topLeft = androidx.compose.ui.geometry.Offset(x = size.width * 0.68f - strokeWidth / 2, y = size.height * 0.25f),
                    size = androidx.compose.ui.geometry.Size(width = strokeWidth, height = size.height * 0.5f)
                )
            }
    )
}

@Composable
fun VolumeUpIcon(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    Box(
        modifier = modifier
            .drawBehind {
                // Draw speaker cone body
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width * 0.2f, size.height * 0.35f)
                    lineTo(size.width * 0.42f, size.height * 0.35f)
                    lineTo(size.width * 0.65f, size.height * 0.15f)
                    lineTo(size.width * 0.65f, size.height * 0.85f)
                    lineTo(size.width * 0.42f, size.height * 0.65f)
                    lineTo(size.width * 0.2f, size.height * 0.65f)
                    close()
                }
                drawPath(path = path, color = tint)
                
                // Draw sound waves
                drawArc(
                    color = tint,
                    startAngle = -40f,
                    sweepAngle = 80f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.35f, size.height * 0.2f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.5f, size.height * 0.6f),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
    )
}

@Composable
fun speechStateBanner(
    speechState: AudioCueManager.SpeechState,
    onReplay: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        color = when (speechState) {
            is AudioCueManager.SpeechState.Loading -> MaterialTheme.colorScheme.surfaceVariant
            is AudioCueManager.SpeechState.Playing -> MaterialTheme.colorScheme.primaryContainer
            is AudioCueManager.SpeechState.Error -> MaterialTheme.colorScheme.errorContainer
            else -> Color.Transparent
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                when (speechState) {
                    is AudioCueManager.SpeechState.Loading -> {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Loading Voice",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = pulseAlpha),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Synthesizing AI Spoken Guide...",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = pulseAlpha)
                        )
                    }
                    is AudioCueManager.SpeechState.Playing -> {
                        VolumeUpIcon(
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Guided: Speaking via ${speechState.source}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    is AudioCueManager.SpeechState.Error -> {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Speech: Using fallback device TTS",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    else -> {
                        // Silent state
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Audio cues ready",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Voice cue ready",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            if (speechState is AudioCueManager.SpeechState.Idle || speechState is AudioCueManager.SpeechState.Playing) {
                IconButton(
                    onClick = onReplay,
                    modifier = Modifier.size(24.dp)
                ) {
                    VolumeUpIcon(
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun SessionCompleteScreen(
    viewModel: YogaViewModel,
    onDone: () -> Unit
) {
    val flow by viewModel.flow.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Success check",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Namaste",
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Your practice is complete.",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = flow.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "12",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Postures Held",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "360s",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Practice Time",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        Text(
            text = "You have mindfully completed the 12 steps of the Sun Salutation flow. Carry this sense of calm, posture, and strength with you throughout your day.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("complete_done_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "Return to Dashboard",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun FrostedGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    val orbPurple = if (isDark) OrbPurpleDark else OrbPurpleLight
    val orbBlue = if (isDark) OrbBlueDark else OrbBlueLight
    val bg = if (isDark) DarkBackground else LightBackground

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bg)
            .drawBehind {
                // Draw top-left purple-pink orb
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(orbPurple, Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.25f, size.height * 0.25f),
                        radius = size.minDimension * 0.55f
                    ),
                    radius = size.minDimension * 0.55f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.25f, size.height * 0.25f)
                )
                // Draw bottom-right blue orb
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(orbBlue, Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.75f, size.height * 0.75f),
                        radius = size.minDimension * 0.55f
                    ),
                    radius = size.minDimension * 0.55f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.75f, size.height * 0.75f)
                )
            }
    ) {
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(24.dp),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    val borderColor = if (isDark) GlassBorderDark else GlassBorderLight
    
    Card(
        modifier = modifier
            .border(width = 1.dp, color = borderColor, shape = shape),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            content()
        }
    }
}

@Composable
fun DrawerStatisticsBadge(
    viewModel: YogaViewModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLevel by viewModel.currentLevel.collectAsState()
    val currentLevelName by viewModel.currentLevelName.collectAsState()
    val totalSessions by viewModel.totalSessions.collectAsState()
    val totalSparks by viewModel.totalSparks.collectAsState()
    val totalXp by viewModel.totalXp.collectAsState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Mini Level Badge
        Box(
            modifier = Modifier
                .size(36.dp)
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
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        // Progress Summary info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$currentLevelName • $totalXp XP",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "🧘 $totalSessions • ✨ $totalSparks",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun CompactStatisticsPanel(
    viewModel: YogaViewModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLevel by viewModel.currentLevel.collectAsState()
    val currentLevelName by viewModel.currentLevelName.collectAsState()
    val levelProgress by viewModel.levelProgress.collectAsState()
    val totalSessions by viewModel.totalSessions.collectAsState()
    val totalSparks by viewModel.totalSparks.collectAsState()
    val totalXp by viewModel.totalXp.collectAsState()

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Mini Level Badge
            Box(
                modifier = Modifier
                    .size(44.dp)
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
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            // Progress Summary info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$currentLevelName • $totalXp XP",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "🧘 $totalSessions • ✨ $totalSparks",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                LinearProgressIndicator(
                    progress = { levelProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    strokeCap = StrokeCap.Round
                )
            }

            // Arrow to expand
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Expand Dashboard",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ExpandedDashboardScreen(
    viewModel: YogaViewModel,
    onBack: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Your Journey",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
                color = MaterialTheme.colorScheme.primary
            )

        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            StatisticsPanel(viewModel = viewModel)
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun PracticeHistoryScreen(
    viewModel: YogaViewModel,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sessions by viewModel.allSessions.collectAsState(initial = emptyList())
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "History & Calendar",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
                color = MaterialTheme.colorScheme.primary
            )

        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "PRACTICE CALENDAR",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            
            // Monthly Calendar
            PracticeCalendarView(sessions = sessions)
            
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                modifier = Modifier.padding(vertical = 4.dp)
            )
            
            // Complete Practice history
            SessionHistorySection(viewModel = viewModel, maxItems = 25)
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PracticeCalendarView(
    sessions: List<com.example.db.YogaSession>,
    modifier: Modifier = Modifier
) {
    var calendar by remember { mutableStateOf(java.util.Calendar.getInstance()) }
    val currentMonthName = remember(calendar) {
        calendar.getDisplayName(java.util.Calendar.MONTH, java.util.Calendar.LONG, java.util.Locale.getDefault()) ?: ""
    }
    val currentYear = remember(calendar) {
        calendar.get(java.util.Calendar.YEAR)
    }
    
    // Calculate days in the current month shown
    val daysInMonth = remember(calendar) {
        val tempCal = calendar.clone() as java.util.Calendar
        tempCal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = tempCal.get(java.util.Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday, etc.
        val maxDays = tempCal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        Pair(firstDayOfWeek, maxDays)
    }
    
    val firstDayOfWeek = daysInMonth.first
    val maxDays = daysInMonth.second
    
    var selectedDay by remember { mutableStateOf<Triple<Int, Int, Int>?>(null) }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(16.dp)
    ) {
        // Month navigation header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val newCal = calendar.clone() as java.util.Calendar
                    newCal.add(java.util.Calendar.MONTH, -1)
                    calendar = newCal
                },
                modifier = Modifier.testTag("prev_month_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous Month",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Text(
                text = "$currentMonthName $currentYear",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            IconButton(
                onClick = {
                    val newCal = calendar.clone() as java.util.Calendar
                    newCal.add(java.util.Calendar.MONTH, 1)
                    calendar = newCal
                },
                modifier = Modifier.testTag("next_month_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next Month",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Days of week row
        val daysOfWeek = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Days grid
        val totalCells = (firstDayOfWeek - 1) + maxDays
        val rowsCount = (totalCells + 6) / 7
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (r in 0 until rowsCount) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    for (c in 0..6) {
                        val cellIndex = r * 7 + c
                        val dayNumber = cellIndex - (firstDayOfWeek - 2)
                        
                        if (dayNumber in 1..maxDays) {
                            val cellCal = calendar.clone() as java.util.Calendar
                            cellCal.set(java.util.Calendar.DAY_OF_MONTH, dayNumber)
                            
                            val daySessions = sessions.filter { session ->
                                val sessionCal = java.util.Calendar.getInstance().apply { timeInMillis = session.timestamp }
                                sessionCal.get(java.util.Calendar.YEAR) == cellCal.get(java.util.Calendar.YEAR) &&
                                        sessionCal.get(java.util.Calendar.DAY_OF_YEAR) == cellCal.get(java.util.Calendar.DAY_OF_YEAR)
                            }
                            
                            val isToday = java.util.Calendar.getInstance().let { today ->
                                today.get(java.util.Calendar.YEAR) == cellCal.get(java.util.Calendar.YEAR) &&
                                        today.get(java.util.Calendar.DAY_OF_YEAR) == cellCal.get(java.util.Calendar.DAY_OF_YEAR)
                            }
                            
                            val cellYear = cellCal.get(java.util.Calendar.YEAR)
                            val cellMonth = cellCal.get(java.util.Calendar.MONTH)
                            val isSelected = selectedDay?.let { (y, m, d) ->
                                y == cellYear && m == cellMonth && d == dayNumber
                            } ?: false
                            
                            CalendarCell(
                                dayNumber = dayNumber,
                                isToday = isToday,
                                isSelected = isSelected,
                                sessions = daySessions,
                                onClick = {
                                    selectedDay = if (isSelected) null else Triple(cellYear, cellMonth, dayNumber)
                                }
                            )
                        } else {
                            Box(modifier = Modifier.size(36.dp))
                        }
                    }
                }
            }
        }
        
        // Expandable day section
        val activeSelectedDay = selectedDay
        if (activeSelectedDay != null) {
            val (y, m, d) = activeSelectedDay
            val selectedDayCal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.YEAR, y)
                set(java.util.Calendar.MONTH, m)
                set(java.util.Calendar.DAY_OF_MONTH, d)
            }
            val selectedDaySessions = sessions.filter { session ->
                val sessionCal = java.util.Calendar.getInstance().apply { timeInMillis = session.timestamp }
                sessionCal.get(java.util.Calendar.YEAR) == y &&
                        sessionCal.get(java.util.Calendar.MONTH) == m &&
                        sessionCal.get(java.util.Calendar.DAY_OF_MONTH) == d
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(12.dp))
            
            val formattedDate = remember(activeSelectedDay) {
                val monthName = selectedDayCal.getDisplayName(java.util.Calendar.MONTH, java.util.Calendar.SHORT, java.util.Locale.getDefault()) ?: ""
                "$monthName $d, $y"
            }
            
            Text(
                text = "Completed on $formattedDate",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            if (selectedDaySessions.isEmpty()) {
                Text(
                    text = "No flows completed on this day.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            } else {
                val startTimeFormat = remember {
                    java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    selectedDaySessions.forEach { session ->
                        val flowName = session.flowName
                        val duration = session.durationMinutes
                        val flowColor = getColorForFlow(session.flowId)
                        
                        val base = 150
                        val durationXp = duration * 10
                        val difficultyBonus = when (session.flowId) {
                            "restorative_yin", "morning_energizer", "bedtime_wind_down" -> 30
                            "sun_salutation", "heart_opening" -> 50
                            "warrior_flow", "core_balance" -> 100
                            "power_vinyasa", "ashtanga_core", "balance_mastery" -> 150
                            else -> 30
                        }
                        val xpEarned = base + durationXp + difficultyBonus
                        val formattedStartTime = remember(session.timestamp) {
                            startTimeFormat.format(java.util.Date(session.timestamp))
                        }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = flowColor.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = flowColor.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(flowColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = flowName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "$duration min • $formattedStartTime",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            Text(
                                text = "+$xpEarned XP",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarCell(
    dayNumber: Int,
    isToday: Boolean,
    isSelected: Boolean,
    sessions: List<com.example.db.YogaSession>,
    onClick: () -> Unit
) {
    val hasSessions = sessions.isNotEmpty()
    
    val cellBackground = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
    } else if (isToday && !hasSessions) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    } else if (hasSessions) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    } else {
        Color.Transparent
    }
    
    val cellBorderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else if (isToday) {
        MaterialTheme.colorScheme.primary
    } else if (hasSessions) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    } else {
        Color.Transparent
    }
    
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(color = cellBackground, shape = RoundedCornerShape(8.dp))
            .border(
                width = if (isSelected) 2.dp else if (isToday || hasSessions) 1.dp else 0.dp,
                color = cellBorderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$dayNumber",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (isToday || hasSessions || isSelected) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else if (hasSessions) {
                    MaterialTheme.colorScheme.primary
                } else if (isToday) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            
            if (hasSessions) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 1.dp)
                ) {
                    sessions.take(3).forEach { session ->
                        val dotColor = getColorForFlow(session.flowId)
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(dotColor, shape = CircleShape)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LevelsInfoDialog(
    currentLevel: Int,
    totalXp: Int,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_levels_dialog_btn")
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🌸", fontSize = 24.sp)
                Text(
                    text = "Yoga Levels & Path",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Earn XP by completing sessions. Each practice brings you closer to the next level of mindfulness.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    val levels = listOf(
                        Triple(1, "Prana Sprout", "0 - 300 XP"),
                        Triple(2, "Zen Seeker", "301 - 900 XP"),
                        Triple(3, "Flow Initiate", "901 - 2100 XP"),
                        Triple(4, "Mindfulness Guide", "2101 - 4500 XP"),
                        Triple(5, "Prana Master", "4501 - 9300 XP"),
                        Triple(6, "Cosmic Flow Yogi", "9301 - 18900 XP"),
                        Triple(7, "Inner Light Sage", "18901 - 38100 XP"),
                        Triple(8, "Chakra Harmonizer", "38101 - 76500 XP"),
                        Triple(9, "Nirvana Ascendant", "76501 - 153300 XP"),
                        Triple(10, "Infinite Samadhi", "153301+ XP")
                    )
                    
                    levels.forEach { (lvl, name, xp) ->
                        val isCurrent = currentLevel == lvl
                        val cardColor = if (isCurrent) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                        }
                        val borderColor = if (isCurrent) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Transparent
                        }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(color = cardColor, shape = RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isCurrent) 1.5.dp else 0.dp,
                                    color = borderColor,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$lvl",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = xp,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                if (isCurrent) {
                                    Box(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "CURRENT",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun SessionHistorySection(
    viewModel: YogaViewModel,
    modifier: Modifier = Modifier,
    maxItems: Int = Int.MAX_VALUE
) {
    val sessions by viewModel.allSessions.collectAsState(initial = emptyList())
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (maxItems == Int.MAX_VALUE) "PRACTICE & XP HISTORY" else "RECENT FLOWS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        
        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("🌱", style = MaterialTheme.typography.headlineLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your journal is empty",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Completed sessions and XP logs will appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            val dateFormat = remember {
                java.text.SimpleDateFormat("MMM dd, yyyy • hh:mm a", java.util.Locale.getDefault())
            }
            
            val displaySessions = if (maxItems == Int.MAX_VALUE) sessions else sessions.take(maxItems)
            val groupedSessions = displaySessions.groupBy { session ->
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = session.timestamp }
                Triple(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH))
            }
            
            groupedSessions.forEach { (dateTuple, daySessions) ->
                val (y, m, d) = dateTuple
                val dateCal = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.YEAR, y)
                    set(java.util.Calendar.MONTH, m)
                    set(java.util.Calendar.DAY_OF_MONTH, d)
                }
                val formattedDate = remember(dateTuple) {
                    val monthName = dateCal.getDisplayName(java.util.Calendar.MONTH, java.util.Calendar.SHORT, java.util.Locale.getDefault()) ?: ""
                    "$monthName $d, $y"
                }
                
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
                
                daySessions.forEach { session ->
                // Calculate XP for this session
                val base = 150
                val durationXp = session.durationMinutes * 10
                val difficultyBonus = when (session.flowId) {
                    "restorative_yin", "morning_energizer", "bedtime_wind_down" -> 30
                    "sun_salutation", "heart_opening" -> 50
                    "warrior_flow", "core_balance" -> 100
                    "power_vinyasa", "ashtanga_core", "balance_mastery" -> 150
                    else -> 30
                }
                val totalSessionXp = base + durationXp + difficultyBonus
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(14.dp)
                        .testTag("session_history_item_${session.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = session.flowName,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = dateFormat.format(java.util.Date(session.timestamp)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                        
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "+$totalSessionXp XP",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${session.durationMinutes} min",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
fun CountdownStartScreen(
    viewModel: YogaViewModel,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val countdownRemaining by viewModel.countdownRemaining.collectAsState()
    val preferredVoice by viewModel.preferredVoice.collectAsState()

    // Animate the countdown number whenever it changes for a pulsing effect
    val scale = remember(countdownRemaining) { androidx.compose.animation.core.Animatable(0.5f) }
    LaunchedEffect(countdownRemaining) {
        scale.animateTo(
            targetValue = 1.2f,
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessLow
            )
        )
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = androidx.compose.animation.core.tween(durationMillis = 200)
        )
    }

    Scaffold(
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Lotus flower icon
                Text(
                    text = "🌸",
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Text(
                    text = "Prepare Your Mind & Body",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                val firstPose = viewModel.flow.collectAsState().value.poses.firstOrNull()
                Text(
                    text = if (preferredVoice == "sa") {
                        "प्रथमं आसनं सिद्धं भवतु: ${firstPose?.sanskritName ?: ""}"
                    } else {
                        "Prepare for your first pose: ${firstPose?.englishName ?: ""}"
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(48.dp))

                // The pulsing countdown circle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(160.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                        .border(
                            width = 4.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                ) {
                    Text(
                        text = countdownRemaining.toString(),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 80.sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.graphicsLayer(
                            scaleX = scale.value,
                            scaleY = scale.value
                        )
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                Text(
                    text = "Beginning in a few moments...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Control actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Cancel Practice")
                    }
                    Button(
                        onClick = { viewModel.skipCountdownAndStart() },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Start Now")
                    }
                }
            }
        }
    }
}

@Composable
fun StatisticsPanel(
    viewModel: YogaViewModel,
    modifier: Modifier = Modifier
) {
    val totalSessions by viewModel.totalSessions.collectAsState()
    val totalXp by viewModel.totalXp.collectAsState()
    val currentLevel by viewModel.currentLevel.collectAsState()
    val currentLevelName by viewModel.currentLevelName.collectAsState()
    val levelProgress by viewModel.levelProgress.collectAsState()
    val totalSparks by viewModel.totalSparks.collectAsState()
    val dailyQuestCompleted by viewModel.dailyQuestCompleted.collectAsState()
    val achievements by viewModel.achievements.collectAsState()

    var showLevelsDialog by remember { mutableStateOf(false) }
    var activeDialogType by remember { mutableStateOf<String?>(null) } // "sessions", "xp", "sparks"

    if (showLevelsDialog) {
        LevelsInfoDialog(
            currentLevel = currentLevel,
            totalXp = totalXp,
            onDismiss = { showLevelsDialog = false }
        )
    }



    Column(modifier = modifier.fillMaxWidth()) {
        // Level & XP Bar Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showLevelsDialog = true }
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                    )
                    .padding(16.dp)
                    .testTag("lvl_section_card")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Level Circle Badge
                    Box(
                        modifier = Modifier
                            .size(56.dp)
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
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "LVL",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 9.sp
                                ),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = "$currentLevel",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    // XP Details
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = currentLevelName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$totalXp XP",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        // XP Progress Bar
                        LinearProgressIndicator(
                            progress = { levelProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            strokeCap = StrokeCap.Round
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        val remainingXpText = when (currentLevel) {
                            1 -> "${300 - totalXp} XP to level up!"
                            2 -> "${900 - totalXp} XP to level up!"
                            3 -> "${2100 - totalXp} XP to level up!"
                            4 -> "${4500 - totalXp} XP to level up!"
                            5 -> "${9300 - totalXp} XP to level up!"
                            6 -> "${18900 - totalXp} XP to level up!"
                            7 -> "${38100 - totalXp} XP to level up!"
                            8 -> "${76500 - totalXp} XP to level up!"
                            9 -> "${153300 - totalXp} XP to level up!"
                            else -> "Maximum level achieved!"
                        }
                        Text(
                            text = remainingXpText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Daily practice quest banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (dailyQuestCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (dailyQuestCompleted) "✨" else "🌸",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Column {
                        Text(
                            text = if (dailyQuestCompleted) "Today's Quest Completed!" else "Today's Practice Quest",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (dailyQuestCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (dailyQuestCompleted) 
                                "You've earned today's Daily Zen Spark and +150 XP bonus!" 
                                else "Complete any yoga flow today to earn today's Daily Zen Spark and +150 XP bonus!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Three Stats Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    value = "$totalSessions",
                    label = "Total Sessions",
                    icon = Icons.Default.CheckCircle,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { activeDialogType = if (activeDialogType == "sessions") null else "sessions" },
                    isSelected = activeDialogType == "sessions",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    value = "$totalXp",
                    label = "Karma XP",
                    icon = Icons.Default.Star,
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = { activeDialogType = if (activeDialogType == "xp") null else "xp" },
                    isSelected = activeDialogType == "xp",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    value = "$totalSparks",
                    label = "Zen Sparks",
                    icon = Icons.Default.Favorite,
                    color = MaterialTheme.colorScheme.tertiary,
                    onClick = { activeDialogType = if (activeDialogType == "sparks") null else "sparks" },
                    isSelected = activeDialogType == "sparks",
                    modifier = Modifier.weight(1f)
                )
            }

            // Lightweight Material Inline Dropdown / Detail Card
            AnimatedVisibility(
                visible = activeDialogType != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                if (activeDialogType != null) {
                    val infoTitle = when (activeDialogType) {
                        "sessions" -> "Total Sessions 🧘"
                        "xp" -> "Karma XP ⭐"
                        else -> "Zen Sparks ✨"
                    }
                    val infoDescription = when (activeDialogType) {
                        "sessions" -> "This tracks your overall dedication. It shows the total number of yoga and meditation sessions you have completed. Every session helps cultivate self-awareness and physical vitality."
                        "xp" -> "Karma Experience Points (XP) represent the spiritual energy and effort you put into your practice. You earn XP by completing sessions, keeping active streaks, and completing daily quests. Gathering XP helps you level up and progress along your journey."
                        else -> "Zen Sparks represent daily mindfulness and consistent practice. You earn exactly 1 Zen Spark for each unique calendar day you practice. Each Zen Spark also grants a +150 XP bonus! Collect 3 Zen Sparks on different days to unlock the 'Zen Spark Collector' badge."
                    }
                    val infoIcon = when (activeDialogType) {
                        "sessions" -> Icons.Default.CheckCircle
                        "xp" -> Icons.Default.Star
                        else -> Icons.Default.Favorite
                    }
                    val infoColor = when (activeDialogType) {
                        "sessions" -> MaterialTheme.colorScheme.primary
                        "xp" -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.tertiary
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = infoColor.copy(alpha = 0.05f)
                        ),
                        border = BorderStroke(1.dp, infoColor.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(infoColor.copy(alpha = 0.15f), shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = infoIcon,
                                    contentDescription = null,
                                    tint = infoColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = infoTitle,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    IconButton(
                                        onClick = { activeDialogType = null },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss info",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = infoDescription,
                                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Achievements Badge Section
            if (achievements.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "UNLOCKED HARMONY BADGES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Render badges in a clean horizontal scroll list
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(achievements) { achievement ->
                        AchievementBadgeCard(achievement = achievement)
                    }
                }
            }
        }
}

@Composable
fun AchievementBadgeCard(
    achievement: com.example.viewmodel.Achievement,
    modifier: Modifier = Modifier
) {
    val alpha = if (achievement.isUnlocked) 1.0f else 0.4f
    val cardBg = if (achievement.isUnlocked) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
    }
    val borderColor = if (achievement.isUnlocked) {
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
    }

    Box(
        modifier = modifier
            .width(135.dp)
            .background(color = cardBg, shape = RoundedCornerShape(16.dp))
            .padding(10.dp)
            .alpha(alpha)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Badge Circle
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = if (achievement.isUnlocked) {
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (achievement.id) {
                        "first_breath" -> "🌬️"
                        "zen_spark_collector" -> "✨"
                        "tri_fold_harmony" -> "🌿"
                        "yogi_adept" -> "🎯"
                        "deep_devotee" -> "🧘"
                        else -> "🏆"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = achievement.title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = achievement.description,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 2,
                minLines = 2,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 2.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))
            
            Box(
                modifier = Modifier
                    .background(
                        color = if (achievement.isUnlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (achievement.isUnlocked) "UNLOCKED" else achievement.progressText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (achievement.isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun StatCard(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .background(if (isSelected) color.copy(alpha = 0.16f) else color.copy(alpha = 0.08f))
            .then(
                if (isSelected) Modifier.border(1.5.dp, color, RoundedCornerShape(16.dp))
                else Modifier
            )
            .padding(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun StatDetailsDialog(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(color.copy(alpha = 0.15f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Got it", fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}



enum class PoseAngle {
    FRONT,
    SIDE,
    PERSPECTIVE
}



class PoseSkeleton(
    val head: Offset,
    val shoulder: Offset,
    val hip: Offset,
    val limbs: List<Pair<Offset, Offset>>,
    val joints: List<Offset> = emptyList()
)

fun getPoseSkeleton(poseId: Int, angle: PoseAngle, w: Float, h: Float, matY: Float): PoseSkeleton {
    var head = Offset(w * 0.5f, h * 0.25f)
    var shoulder = Offset(w * 0.5f, h * 0.41f)
    var hip = Offset(w * 0.5f, h * 0.65f)
    val limbs = mutableListOf<Pair<Offset, Offset>>()
    val joints = mutableListOf<Offset>()

    when (poseId) {
        1, 12 -> { // Prayer Pose
            if (angle == PoseAngle.SIDE) {
                head = Offset(w * 0.48f, h * 0.25f)
                shoulder = Offset(w * 0.48f, h * 0.41f)
                hip = Offset(w * 0.48f, h * 0.65f)
                limbs.add(hip to Offset(w * 0.48f, matY))
                limbs.add(shoulder to Offset(w * 0.58f, h * 0.50f))
                limbs.add(Offset(w * 0.58f, h * 0.50f) to Offset(w * 0.58f, h * 0.42f))
                joints.addAll(listOf(shoulder, hip, Offset(w * 0.58f, h * 0.50f)))
            } else {
                val dx = if (angle == PoseAngle.FRONT) 0f else 0.02f
                head = Offset(w * (0.5f + dx), h * 0.25f)
                shoulder = Offset(w * (0.5f + dx), h * 0.41f)
                hip = Offset(w * 0.5f, h * 0.65f)
                limbs.add(hip to Offset(w * 0.46f, matY))
                limbs.add(hip to Offset(w * 0.54f, matY))
                val elbowL = Offset(w * 0.38f, h * 0.52f)
                val elbowR = Offset(w * 0.62f, h * 0.52f)
                val hands = Offset(w * 0.5f, h * 0.48f)
                limbs.add(shoulder to elbowL)
                limbs.add(elbowL to hands)
                limbs.add(shoulder to elbowR)
                limbs.add(elbowR to hands)
                joints.addAll(listOf(shoulder, hip, elbowL, elbowR))
            }
        }
        2, 11 -> { // Raised Arms
            if (angle == PoseAngle.SIDE) {
                head = Offset(w * 0.42f, h * 0.26f)
                shoulder = Offset(w * 0.46f, h * 0.41f)
                hip = Offset(w * 0.53f, h * 0.67f)
                limbs.add(hip to Offset(w * 0.53f, matY))
                limbs.add(shoulder to Offset(w * 0.30f, h * 0.16f))
            } else {
                val dx = if (angle == PoseAngle.FRONT) 0f else -0.02f
                head = Offset(w * (0.5f + dx), h * 0.24f)
                shoulder = Offset(w * (0.5f + dx), h * 0.40f)
                hip = Offset(w * (0.5f + dx), h * 0.65f)
                limbs.add(hip to Offset(w * 0.48f, matY))
                limbs.add(hip to Offset(w * 0.54f, matY))
                limbs.add(shoulder to Offset(w * 0.32f, h * 0.15f))
                limbs.add(shoulder to Offset(w * 0.62f, h * 0.15f))
            }
            joints.addAll(listOf(shoulder, hip))
        }
        3, 10 -> { // Standing Forward Bend
            val dx = if (angle == PoseAngle.FRONT) 0f else if (angle == PoseAngle.SIDE) 0.05f else 0.03f
            hip = Offset(w * (0.5f + dx), h * 0.46f)
            shoulder = Offset(w * (0.5f - dx * 0.5f), h * 0.72f)
            head = Offset(w * (0.5f - dx), h * 0.82f)
            limbs.add(hip to Offset(w * (0.5f + dx), matY))
            limbs.add(shoulder to Offset(w * (0.45f - dx), h * 0.84f))
            joints.addAll(listOf(shoulder, hip))
        }
        4, 9 -> { // Lunge
            if (angle == PoseAngle.FRONT) {
                head = Offset(w * 0.5f, h * 0.35f)
                shoulder = Offset(w * 0.5f, h * 0.48f)
                hip = Offset(w * 0.5f, h * 0.68f)
                val kneeL = Offset(w * 0.42f, h * 0.78f)
                limbs.add(hip to kneeL)
                limbs.add(kneeL to Offset(w * 0.42f, matY))
                val kneeR = Offset(w * 0.58f, h * 0.78f)
                limbs.add(hip to kneeR)
                limbs.add(kneeR to Offset(w * 0.58f, matY))
                limbs.add(shoulder to Offset(w * 0.40f, h * 0.60f))
                limbs.add(shoulder to Offset(w * 0.60f, h * 0.60f))
                joints.addAll(listOf(shoulder, hip, kneeL, kneeR))
            } else {
                hip = Offset(w * 0.42f, h * 0.65f)
                shoulder = Offset(w * 0.44f, h * 0.45f)
                head = Offset(w * 0.45f, h * 0.32f)
                val kneeFront = Offset(w * 0.56f, h * 0.63f)
                limbs.add(hip to kneeFront)
                limbs.add(kneeFront to Offset(w * 0.62f, matY))
                limbs.add(hip to Offset(w * 0.25f, matY))
                limbs.add(shoulder to Offset(w * 0.5f, matY))
                joints.addAll(listOf(shoulder, hip, kneeFront))
            }
        }
        5 -> { // Plank
            if (angle == PoseAngle.FRONT) {
                head = Offset(w * 0.5f, h * 0.40f)
                shoulder = Offset(w * 0.5f, h * 0.50f)
                hip = Offset(w * 0.5f, h * 0.62f)
                limbs.add(shoulder to Offset(w * 0.44f, matY))
                limbs.add(shoulder to Offset(w * 0.56f, matY))
                limbs.add(hip to Offset(w * 0.5f, matY))
                joints.addAll(listOf(shoulder, hip))
            } else {
                hip = Offset(w * 0.45f, h * 0.60f)
                shoulder = Offset(w * 0.68f, h * 0.48f)
                head = Offset(w * 0.76f, h * 0.41f)
                limbs.add(hip to Offset(w * 0.22f, matY))
                limbs.add(shoulder to Offset(w * 0.68f, matY))
                joints.addAll(listOf(shoulder, hip))
            }
        }
        6 -> { // Eight-Limbed
            if (angle == PoseAngle.FRONT) {
                head = Offset(w * 0.5f, h * 0.55f)
                shoulder = Offset(w * 0.5f, h * 0.65f)
                hip = Offset(w * 0.5f, h * 0.58f)
                val elbowL = Offset(w * 0.38f, h * 0.72f)
                val elbowR = Offset(w * 0.62f, h * 0.72f)
                limbs.add(shoulder to elbowL)
                limbs.add(elbowL to Offset(w * 0.40f, matY))
                limbs.add(shoulder to elbowR)
                limbs.add(elbowR to Offset(w * 0.60f, matY))
                limbs.add(hip to Offset(w * 0.45f, matY))
                limbs.add(hip to Offset(w * 0.55f, matY))
                joints.addAll(listOf(shoulder, hip, elbowL, elbowR))
            } else {
                hip = Offset(w * 0.50f, h * 0.58f)
                shoulder = Offset(w * 0.66f, h * 0.70f)
                head = Offset(w * 0.75f, h * 0.62f)
                val knees = Offset(w * 0.38f, matY)
                limbs.add(knees to Offset(w * 0.22f, matY))
                limbs.add(hip to knees)
                val elbow = Offset(w * 0.58f, h * 0.66f)
                limbs.add(shoulder to elbow)
                limbs.add(elbow to Offset(w * 0.62f, matY))
                joints.addAll(listOf(shoulder, hip, knees))
            }
        }
        7, 302 -> { // Cobra / Sphinx
            if (angle == PoseAngle.FRONT) {
                head = Offset(w * 0.5f, h * 0.32f)
                shoulder = Offset(w * 0.5f, h * 0.48f)
                hip = Offset(w * 0.5f, matY)
                limbs.add(shoulder to Offset(w * 0.40f, matY))
                limbs.add(shoulder to Offset(w * 0.60f, matY))
                limbs.add(hip to shoulder)
                joints.addAll(listOf(shoulder, hip))
            } else {
                hip = Offset(w * 0.45f, matY)
                shoulder = Offset(w * 0.66f, h * 0.50f)
                head = Offset(w * 0.72f, h * 0.34f)
                limbs.add(hip to Offset(w * 0.22f, matY))
                val contactY = if (poseId == 302) h * 0.72f else matY
                limbs.add(shoulder to Offset(w * 0.64f, contactY))
                joints.addAll(listOf(shoulder, hip))
            }
        }
        8 -> { // Downward Dog
            if (angle == PoseAngle.FRONT) {
                hip = Offset(w * 0.5f, h * 0.42f)
                shoulder = Offset(w * 0.5f, h * 0.66f)
                head = Offset(w * 0.5f, h * 0.74f)
                limbs.add(hip to Offset(w * 0.42f, matY))
                limbs.add(hip to Offset(w * 0.58f, matY))
                limbs.add(shoulder to Offset(w * 0.40f, matY))
                limbs.add(shoulder to Offset(w * 0.60f, matY))
                joints.addAll(listOf(shoulder, hip))
            } else {
                hip = Offset(w * 0.50f, h * 0.40f)
                shoulder = Offset(w * 0.68f, h * 0.66f)
                head = Offset(w * 0.72f, h * 0.74f)
                limbs.add(hip to Offset(w * 0.28f, matY))
                limbs.add(shoulder to Offset(w * 0.78f, matY))
                joints.addAll(listOf(shoulder, hip))
            }
        }
        201 -> { // Mountain Pose
            if (angle == PoseAngle.SIDE) {
                head = Offset(w * 0.5f, h * 0.25f)
                shoulder = Offset(w * 0.5f, h * 0.40f)
                hip = Offset(w * 0.5f, h * 0.65f)
                limbs.add(hip to Offset(w * 0.5f, matY))
                limbs.add(shoulder to Offset(w * 0.5f, h * 0.55f))
                joints.addAll(listOf(shoulder, hip))
            } else {
                head = Offset(w * 0.5f, h * 0.25f)
                shoulder = Offset(w * 0.5f, h * 0.40f)
                hip = Offset(w * 0.5f, h * 0.65f)
                limbs.add(hip to Offset(w * 0.47f, matY))
                limbs.add(hip to Offset(w * 0.53f, matY))
                limbs.add(shoulder to Offset(w * 0.42f, h * 0.55f))
                limbs.add(shoulder to Offset(w * 0.58f, h * 0.55f))
                joints.addAll(listOf(shoulder, hip))
            }
        }
        202 -> { // Warrior I
            if (angle == PoseAngle.FRONT) {
                head = Offset(w * 0.5f, h * 0.20f)
                shoulder = Offset(w * 0.5f, h * 0.35f)
                hip = Offset(w * 0.5f, h * 0.60f)
                val kneeL = Offset(w * 0.40f, h * 0.68f)
                limbs.add(hip to kneeL)
                limbs.add(kneeL to Offset(w * 0.38f, matY))
                val kneeR = Offset(w * 0.60f, h * 0.68f)
                limbs.add(hip to kneeR)
                limbs.add(kneeR to Offset(w * 0.62f, matY))
                limbs.add(shoulder to Offset(w * 0.44f, h * 0.10f))
                limbs.add(shoulder to Offset(w * 0.56f, h * 0.10f))
                joints.addAll(listOf(shoulder, hip, kneeL, kneeR))
            } else {
                head = Offset(w * 0.48f, h * 0.20f)
                shoulder = Offset(w * 0.48f, h * 0.35f)
                hip = Offset(w * 0.45f, h * 0.60f)
                val kneeFront = Offset(w * 0.65f, h * 0.65f)
                limbs.add(hip to kneeFront)
                limbs.add(kneeFront to Offset(w * 0.65f, matY))
                limbs.add(hip to Offset(w * 0.25f, matY))
                limbs.add(shoulder to Offset(w * 0.44f, h * 0.10f))
                limbs.add(shoulder to Offset(w * 0.52f, h * 0.10f))
                joints.addAll(listOf(shoulder, hip, kneeFront))
            }
        }
        203 -> { // Warrior II
            if (angle == PoseAngle.FRONT) {
                head = Offset(w * 0.5f, h * 0.28f)
                shoulder = Offset(w * 0.5f, h * 0.42f)
                hip = Offset(w * 0.5f, h * 0.63f)
                val kneeL = Offset(w * 0.40f, h * 0.68f)
                limbs.add(hip to kneeL)
                limbs.add(kneeL to Offset(w * 0.38f, matY))
                val kneeR = Offset(w * 0.60f, h * 0.68f)
                limbs.add(hip to kneeR)
                limbs.add(kneeR to Offset(w * 0.62f, matY))
                limbs.add(shoulder to Offset(w * 0.25f, h * 0.42f))
                limbs.add(shoulder to Offset(w * 0.75f, h * 0.42f))
                joints.addAll(listOf(shoulder, hip, kneeL, kneeR))
            } else {
                head = Offset(w * 0.48f, h * 0.28f)
                shoulder = Offset(w * 0.48f, h * 0.42f)
                hip = Offset(w * 0.48f, h * 0.63f)
                val kneeFront = Offset(w * 0.62f, h * 0.68f)
                limbs.add(hip to kneeFront)
                limbs.add(kneeFront to Offset(w * 0.62f, matY))
                limbs.add(hip to Offset(w * 0.22f, matY))
                limbs.add(shoulder to Offset(w * 0.18f, h * 0.42f))
                limbs.add(shoulder to Offset(w * 0.78f, h * 0.42f))
                joints.addAll(listOf(shoulder, hip, kneeFront))
            }
        }
        204 -> { // Triangle
            if (angle == PoseAngle.FRONT) {
                hip = Offset(w * 0.5f, h * 0.58f)
                shoulder = Offset(w * 0.42f, h * 0.70f)
                head = Offset(w * 0.35f, h * 0.74f)
                limbs.add(hip to Offset(w * 0.35f, matY))
                limbs.add(hip to Offset(w * 0.65f, matY))
                limbs.add(shoulder to Offset(w * 0.38f, h * 0.85f))
                limbs.add(shoulder to Offset(w * 0.50f, h * 0.40f))
                joints.addAll(listOf(shoulder, hip))
            } else {
                hip = Offset(w * 0.48f, h * 0.58f)
                shoulder = Offset(w * 0.40f, h * 0.70f)
                head = Offset(w * 0.33f, h * 0.74f)
                limbs.add(hip to Offset(w * 0.32f, matY))
                limbs.add(hip to Offset(w * 0.68f, matY))
                limbs.add(shoulder to Offset(w * 0.36f, h * 0.85f))
                limbs.add(shoulder to Offset(w * 0.54f, h * 0.40f))
                joints.addAll(listOf(shoulder, hip))
            }
        }
        205, 301 -> { // Child's Pose
            if (angle == PoseAngle.FRONT) {
                head = Offset(w * 0.5f, matY - 10f)
                shoulder = Offset(w * 0.5f, matY - 18f)
                hip = Offset(w * 0.5f, matY - 6f)
                limbs.add(hip to Offset(w * 0.42f, matY))
                limbs.add(hip to Offset(w * 0.58f, matY))
                limbs.add(shoulder to Offset(w * 0.40f, matY - 22f))
                limbs.add(shoulder to Offset(w * 0.60f, matY - 22f))
                joints.addAll(listOf(shoulder, hip))
            } else {
                val knee = Offset(w * 0.45f, matY)
                hip = Offset(w * 0.28f, matY - 6f)
                shoulder = Offset(w * 0.62f, matY - 14f)
                head = Offset(w * 0.72f, matY - 8f)
                limbs.add(hip to knee)
                limbs.add(shoulder to Offset(w * 0.88f, matY))
                joints.addAll(listOf(shoulder, hip))
            }
        }
        303 -> { // Butterfly
            if (angle == PoseAngle.SIDE) {
                head = Offset(w * 0.45f, h * 0.35f)
                shoulder = Offset(w * 0.45f, h * 0.50f)
                hip = Offset(w * 0.45f, h * 0.74f)
                val knee = Offset(w * 0.58f, matY - 4f)
                limbs.add(hip to knee)
                limbs.add(knee to Offset(w * 0.52f, matY))
                limbs.add(shoulder to Offset(w * 0.52f, matY - 2f))
                joints.addAll(listOf(shoulder, hip))
            } else {
                head = Offset(w * 0.5f, h * 0.35f)
                shoulder = Offset(w * 0.5f, h * 0.50f)
                hip = Offset(w * 0.5f, h * 0.74f)
                val kneeL = Offset(w * 0.32f, matY - 4f)
                val kneeR = Offset(w * 0.68f, matY - 4f)
                val feet = Offset(w * 0.5f, matY)
                limbs.add(hip to kneeL)
                limbs.add(kneeL to feet)
                limbs.add(hip to kneeR)
                limbs.add(kneeR to feet)
                limbs.add(shoulder to Offset(w * 0.48f, matY - 2f))
                limbs.add(shoulder to Offset(w * 0.52f, matY - 2f))
                joints.addAll(listOf(shoulder, hip))
            }
        }
        304 -> { // Spinal Twist
            if (angle == PoseAngle.SIDE) {
                head = Offset(w * 0.48f, h * 0.40f)
                shoulder = Offset(w * 0.48f, h * 0.55f)
                hip = Offset(w * 0.48f, h * 0.78f)
                limbs.add(shoulder to Offset(w * 0.65f, h * 0.62f))
                val knee = Offset(w * 0.60f, h * 0.75f)
                limbs.add(hip to knee)
                limbs.add(knee to Offset(w * 0.52f, matY))
                joints.addAll(listOf(shoulder, hip))
            } else {
                head = Offset(w * 0.5f, h * 0.40f)
                shoulder = Offset(w * 0.5f, h * 0.55f)
                hip = Offset(w * 0.5f, h * 0.78f)
                limbs.add(shoulder to Offset(w * 0.22f, h * 0.55f))
                limbs.add(shoulder to Offset(w * 0.78f, h * 0.55f))
                val kneeL = Offset(w * 0.35f, h * 0.78f)
                limbs.add(hip to kneeL)
                limbs.add(kneeL to Offset(w * 0.38f, matY))
                joints.addAll(listOf(shoulder, hip))
            }
        }
        305 -> { // Savasana
            if (angle == PoseAngle.SIDE) {
                head = Offset(w * 0.25f, matY - 6f)
                shoulder = Offset(w * 0.35f, matY - 8f)
                hip = Offset(w * 0.55f, matY - 6f)
                limbs.add(shoulder to hip)
                limbs.add(hip to Offset(w * 0.85f, matY - 4f))
                limbs.add(shoulder to Offset(w * 0.42f, matY - 6f))
                joints.addAll(listOf(shoulder, hip))
            } else {
                head = Offset(w * 0.5f, h * 0.28f)
                shoulder = Offset(w * 0.5f, h * 0.43f)
                hip = Offset(w * 0.5f, h * 0.65f)
                limbs.add(hip to Offset(w * 0.43f, matY))
                limbs.add(hip to Offset(w * 0.57f, matY))
                limbs.add(shoulder to Offset(w * 0.38f, h * 0.55f))
                limbs.add(shoulder to Offset(w * 0.62f, h * 0.55f))
                joints.addAll(listOf(shoulder, hip))
            }
        }
        else -> { // Default meditation/succinct pose
            head = Offset(w * 0.5f, h * 0.32f)
            shoulder = Offset(w * 0.5f, h * 0.48f)
            hip = Offset(w * 0.5f, h * 0.72f)
            val kneeL = Offset(w * 0.35f, h * 0.78f)
            val kneeR = Offset(w * 0.65f, h * 0.78f)
            limbs.add(hip to kneeL)
            limbs.add(hip to kneeR)
            limbs.add(shoulder to kneeL)
            limbs.add(shoulder to kneeR)
            joints.addAll(listOf(shoulder, hip))
        }
    }

    limbs.add(0, shoulder to hip) // Connect torso at index 0

    return PoseSkeleton(head, shoulder, hip, limbs, joints)
}

@Composable
fun YogaPoseVisual(
    poseId: Int,
    angle: PoseAngle = PoseAngle.FRONT,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val matY = height * 0.88f
            
            // Draw clean background grid lines (sketchpad style)
            val gridColor = androidx.compose.ui.graphics.Color(0xFF888888).copy(alpha = 0.15f)
            val gridSize = 20.dp.toPx()
            var x = 0f
            while (x < width) {
                drawLine(color = gridColor, start = androidx.compose.ui.geometry.Offset(x, 0f), end = androidx.compose.ui.geometry.Offset(x, height), strokeWidth = 0.5.dp.toPx())
                x += gridSize
            }
            var y = 0f
            while (y < height) {
                drawLine(color = gridColor, start = androidx.compose.ui.geometry.Offset(0f, y), end = androidx.compose.ui.geometry.Offset(width, y), strokeWidth = 0.5.dp.toPx())
                y += gridSize
            }

            // Draw Ground Shadow
            val graphiteColor = androidx.compose.ui.graphics.Color(0xFF333333).copy(alpha = 0.7f)
            val groundY = matY
            drawLine(color = graphiteColor.copy(alpha = 0.25f), start = androidx.compose.ui.geometry.Offset(width * 0.15f, groundY), end = androidx.compose.ui.geometry.Offset(width * 0.85f, groundY), strokeWidth = 1.dp.toPx())

            // Get anatomical skeleton for this exact pose & angle
            val skeleton = getPoseSkeleton(poseId, angle, width, height, matY)

            val headRadius = 13.dp.toPx()
            val limbThickness = 12.dp.toPx()
            val torsoThickness = 18.dp.toPx()

            val primaryAccent = androidx.compose.ui.graphics.Color(0xFF3DDC84)

            skeleton.limbs.forEachIndexed { index, pair ->
                val start = pair.first
                val end = pair.second
                val isTorso = index == 0
                val thickness = if (isTorso) torsoThickness else limbThickness
                
                drawLine(color = primaryAccent.copy(alpha = 0.2f), start = start, end = end, strokeWidth = thickness, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(color = graphiteColor, start = start, end = end, strokeWidth = 1.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }

            drawCircle(color = primaryAccent.copy(alpha = 0.2f), radius = headRadius, center = skeleton.head)
            drawCircle(color = graphiteColor, radius = headRadius, center = skeleton.head, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()))
            
            val faceOffset = if (angle == PoseAngle.SIDE) {
                if (poseId in listOf(4, 9, 5, 6, 7, 302, 205, 301, 8)) headRadius * 0.8f else -headRadius * 0.8f
            } else 0f
            
            if (faceOffset != 0f) {
                drawLine(color = graphiteColor, start = skeleton.head + androidx.compose.ui.geometry.Offset(faceOffset * 0.5f, 0f), end = skeleton.head + androidx.compose.ui.geometry.Offset(faceOffset, 0f), strokeWidth = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }

            skeleton.joints.forEach { joint ->
                drawCircle(color = androidx.compose.ui.graphics.Color.White, radius = 3.5.dp.toPx(), center = joint)
                drawCircle(color = graphiteColor, radius = 3.5.dp.toPx(), center = joint, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()))
            }
        }
        
        Text(
            text = if (angle == PoseAngle.FRONT) "FRONT" else "SIDE",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontSize = 10.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 8.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ReminderDialog(
    flowName: String,
    flowId: String,
    initialHour: Int = 8,
    initialMinute: Int = 0,
    initialDays: Set<Int> = setOf(),
    onDismiss: () -> Unit,
    onSave: (hour: Int, minute: Int, days: Set<Int>) -> Unit
) {
    val context = LocalContext.current
    var hour by remember { mutableStateOf(initialHour) }
    var minute by remember { mutableStateOf(initialMinute) }
    var days by remember { mutableStateOf(initialDays) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Reminder", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                Text("Flow: $flowName", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Time: ${String.format("%02d:%02d", hour, minute)}", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.weight(1f))
                    Button(onClick = {
                        android.app.TimePickerDialog(
                            context,
                            { _, h, m -> hour = h; minute = m },
                            hour, minute, false
                        ).show()
                    }) {
                        Text("Select")
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Repeat (Days of Week)", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    val daysList = listOf("S" to 1, "M" to 2, "T" to 3, "W" to 4, "T" to 5, "F" to 6, "S" to 7)
                    daysList.forEach { (label, dayValue) ->
                        val selected = days.contains(dayValue)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                                .clickable {
                                    if (selected) days = days - dayValue else days = days + dayValue
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(hour, minute, days) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FlowRemindersListDialog(
    flowName: String,
    flowId: String,
    reminders: List<com.example.db.ReminderEntity>,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (com.example.db.ReminderEntity) -> Unit,
    onDelete: (com.example.db.ReminderEntity) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reminders: $flowName", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (reminders.isEmpty()) {
                    Text("No reminders set for this flow.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(reminders) { reminder ->
                            ReminderItem(
                                reminder = reminder,
                                onEdit = { onEdit(reminder) },
                                onDelete = { onDelete(reminder) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onAdd) {
                Text("Add Reminder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
