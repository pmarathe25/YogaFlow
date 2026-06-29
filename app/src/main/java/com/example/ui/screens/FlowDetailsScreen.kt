package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.db.ReminderEntity
import com.example.db.ReminderManager
import com.example.ui.components.*
import com.example.viewmodel.YogaViewModel
import kotlinx.coroutines.launch

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
