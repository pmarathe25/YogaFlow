package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import com.example.viewmodel.YogaViewModel
import com.example.ui.components.ReminderDialog

@Composable
fun ReminderItem(reminder: com.example.db.ReminderEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val timeString = String.format(java.util.Locale.US, "%02d:%02d", reminder.hour, reminder.minute)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Outlined.Notifications, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(text = timeString, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                if (reminder.daysOfWeek.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val daysStr = reminder.daysOfWeek.split(",")
                        .mapNotNull { dayIntStr -> 
                            when(dayIntStr) {
                                "1" -> "Sun"
                                "2" -> "Mon"
                                "3" -> "Tue"
                                "4" -> "Wed"
                                "5" -> "Thu"
                                "6" -> "Fri"
                                "7" -> "Sat"
                                else -> null
                            }
                        }.joinToString(", ")
                    Text(
                        text = daysStr, 
                        style = MaterialTheme.typography.labelSmall, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Reminder", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Reminder", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: YogaViewModel,
    onBack: () -> Unit
) {
    var showResetConfirmation by remember { mutableStateOf(false) }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Reset All Stats?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently erase all your completed sessions, XP progress, and level history? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllCompletedSessions()
                        showResetConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Reset Everything", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetConfirmation = false }
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            windowInsets = WindowInsets(0, 0, 0, 0),
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                val themeMode by viewModel.themeMode.collectAsState()
                Row(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp)).padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val modes = listOf(
                        "System" to Icons.Default.Settings,
                        "Light" to Icons.Default.Settings,
                        "Dark" to Icons.Default.Settings
                    )
                    modes.forEach { (name, icon) ->
                        val isSelected = themeMode == name
                        Box(
                            modifier = Modifier.weight(1f).background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, RoundedCornerShape(8.dp)).clickable { viewModel.setThemeMode(name) }.padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(name, style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal), color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
                )
                val isVoiceEnabled by viewModel.isVoiceEnabled.collectAsState()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Voice Guide", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Spoken instructions at each pose", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                    Switch(checked = isVoiceEnabled, onCheckedChange = { viewModel.toggleVoice(it) })
                }
                if (isVoiceEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Voice Cue Language", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(8.dp))
                    val preferredVoice by viewModel.preferredVoice.collectAsState()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("en" to "English Guide", "sa" to "Sanskrit").forEach { (code, name) ->
                            val isSelected = preferredVoice == code
                            Box(
                                modifier = Modifier.weight(1f).background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.25f), RoundedCornerShape(12.dp)).border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(12.dp)).clickable { viewModel.setPreferredVoice(code) }.padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) { Text(name, color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
                )
                val isMusicMuted by viewModel.isMusicMuted.collectAsState()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Background Music", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(if (isMusicMuted) "Ambient music disabled" else "Ambient music enabled", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                    Switch(checked = !isMusicMuted, onCheckedChange = { viewModel.setIsMusicMuted(!isMusicMuted) })
                }
                
                if (!isMusicMuted) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val currentTrackIndex by viewModel.currentTrackIndex.collectAsState()
                    val tracks = viewModel.ambientMusicManager.tracks
                    
                    val context = androidx.compose.ui.platform.LocalContext.current
                    var previewPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
                    var previewPlayingIndex by remember { mutableStateOf<Int?>(null) }
                    val scope = androidx.compose.runtime.rememberCoroutineScope()
                    var previewJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
                
                    androidx.compose.runtime.DisposableEffect(Unit) {
                        onDispose {
                            previewJob?.cancel()
                            previewPlayer?.release()
                            previewPlayer = null
                        }
                    }

                    Text("Background Tracks", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        tracks.forEachIndexed { origIndex, track ->
                            val isSelected = currentTrackIndex == origIndex
                            val isPreviewing = previewPlayingIndex == origIndex
                            Row(
                                modifier = Modifier.fillMaxWidth().background(if(isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.15f), RoundedCornerShape(10.dp)).clickable { viewModel.setCurrentTrackIndex(origIndex) }.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = if(isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(track.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if(isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick = {
                                        previewJob?.cancel()
                                        previewPlayer?.release()
                                        if (isPreviewing) {
                                            previewPlayer = null
                                            previewPlayingIndex = null
                                        } else {
                                            previewPlayer = android.media.MediaPlayer.create(context, track.resId)
                                            previewPlayer?.setVolume(0.2f, 0.2f)
                                            previewPlayer?.start()
                                            previewPlayingIndex = origIndex
                                            previewJob = scope.launch {
                                                kotlinx.coroutines.delay(15000)
                                                previewPlayer?.release()
                                                previewPlayer = null
                                                previewPlayingIndex = null
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPreviewing) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = "Preview",
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
                )
                Text("Device Preferences", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                
                // Keep Screen Awake
                val keepScreenAwake by viewModel.keepScreenAwake.collectAsState()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Keep Screen Awake", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                        Text("Prevent screen from dimming or sleeping during a session", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                    Switch(
                        checked = keepScreenAwake,
                        onCheckedChange = { viewModel.setKeepScreenAwake(it) },
                        modifier = Modifier.testTag("keep_screen_awake_switch")
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Background Audio Playback
                val backgroundAudioEnabled by viewModel.backgroundAudioEnabled.collectAsState()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Background Audio Playback", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                        Text("Continue playing voice guide and music when screen is off", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                    Switch(
                        checked = backgroundAudioEnabled,
                        onCheckedChange = { viewModel.setBackgroundAudioEnabled(it) },
                        modifier = Modifier.testTag("background_audio_switch")
                    )
                }
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
                )
                
                val reminders by viewModel.allReminders.collectAsState(initial = emptyList())
                val coroutineScope = rememberCoroutineScope()
                val context = LocalContext.current
                var editingReminder by remember { mutableStateOf<com.example.db.ReminderEntity?>(null) }
                var remindersExpanded by remember { mutableStateOf(false) }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { remindersExpanded = !remindersExpanded }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Manage Reminders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("${reminders.size} reminders active", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                    Icon(
                        imageVector = if (remindersExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand Reminders"
                    )
                }
                
                if (editingReminder != null) {
                    val reminder = editingReminder!!
                    val initialDays = reminder.daysOfWeek.split(",").filter { it.isNotEmpty() }.mapNotNull { it.toIntOrNull() }.toSet()
                    ReminderDialog(
                        flowName = reminder.flowName,
                        flowId = reminder.flowId,
                        initialHour = reminder.hour,
                        initialMinute = reminder.minute,
                        initialDays = initialDays,
                        onDismiss = { editingReminder = null },
                        onSave = { hour, minute, days ->
                            val updatedReminder = reminder.copy(
                                hour = hour,
                                minute = minute,
                                daysOfWeek = days.joinToString(",")
                            )
                            viewModel.updateReminder(updatedReminder)
                            editingReminder = null
                            android.widget.Toast.makeText(context, "Reminder updated", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                
                androidx.compose.animation.AnimatedVisibility(visible = remindersExpanded) {
                    if (reminders.isEmpty()) {
                        Text("No active reminders", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 12.dp)) {
                            val groupedReminders = reminders.groupBy { it.flowName }
                            groupedReminders.forEach { (flowName, flowReminders) ->
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = flowName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                                    )
                                    flowReminders.forEach { reminder ->
                                        ReminderItem(
                                            reminder = reminder,
                                            onEdit = { editingReminder = reminder },
                                            onDelete = {
                                                coroutineScope.launch {
                                                    com.example.db.ReminderManager.cancelAlarm(context, reminder.id)
                                                    viewModel.deleteReminder(reminder)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
                )
                Text("Data & Stats", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { showResetConfirmation = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("reset_stats_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset All Stats", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
