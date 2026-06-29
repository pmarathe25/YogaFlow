package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.db.YogaSession
import com.example.model.XpCalculator
import com.example.ui.components.*
import com.example.viewmodel.YogaViewModel

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
                        
                        val xpEarned = com.example.model.XpCalculator.calculateSessionXp(duration, session.flowId)
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
                val totalSessionXp = com.example.model.XpCalculator.calculateSessionXp(session.durationMinutes, session.flowId)
                
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
