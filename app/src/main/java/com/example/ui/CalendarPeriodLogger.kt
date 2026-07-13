package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.data.CycleUtils
import com.example.data.PeriodLog
import com.example.ui.theme.*
import com.example.viewmodel.LunaViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarPeriodLogger(
    viewModel: LunaViewModel,
    periodLogs: List<PeriodLog>,
    onLogSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Range selection states
    var selectedStartDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedEndDate by remember { mutableStateOf<LocalDate?>(null) }
    
    // Month navigation states
    var calendarYear by remember { mutableStateOf(LocalDate.now().year) }
    var calendarMonth by remember { mutableStateOf(LocalDate.now().monthValue) }
    
    val firstDayOfMonth = LocalDate.of(calendarYear, calendarMonth, 1)
    val daysInMonth = firstDayOfMonth.lengthOfMonth()
    val firstDayOfWeekObj = firstDayOfMonth.dayOfWeek
    val firstDayOfWeekIndex = if (firstDayOfWeekObj.value == 7) 0 else firstDayOfWeekObj.value // Sunday = 0, Monday = 1...
    
    val totalCells = firstDayOfWeekIndex + daysInMonth

    // Flow & Symptoms input state
    var flowLevel by remember { mutableStateOf("Medium") }
    val symptomsList = listOf("Cramps", "Headache", "Back pain", "Breast tenderness", "Acne", "Fatigue", "Bloating", "Nausea", "Mood swings", "Anxiety", "Low mood", "Irritability", "Sleep issues")
    var selectedSymptoms by remember { mutableStateOf(setOf<String>()) }
    var notes by remember { mutableStateOf("") }
    var painLevel by remember { mutableStateOf<Int?>(null) }
    var productUsed by remember { mutableStateOf<String?>(null) }
    var changedProductFrequency by remember { mutableStateOf<String?>(null) }
    
    // Save success banner state
    var showSuccessMessage by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("calendar_period_logger_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Interactive Cycle Logger",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Clear Range Selection button
                if (selectedStartDate != null || selectedEndDate != null) {
                    TextButton(
                        onClick = {
                            selectedStartDate = null
                            selectedEndDate = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Selection", fontSize = 12.sp)
                    }
                }
            }

            Text(
                text = "Tap a day to log period start, and Tap a later day for period end.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )

            // Calendar Navigation Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = firstDayOfMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Row {
                    IconButton(
                        onClick = {
                            if (calendarMonth == 1) {
                                calendarMonth = 12
                                calendarYear--
                            } else {
                                calendarMonth--
                            }
                        },
                        modifier = Modifier.testTag("logger_prev_month")
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                    }
                    IconButton(
                        onClick = {
                            if (calendarMonth == 12) {
                                calendarMonth = 1
                                calendarYear++
                            } else {
                                calendarMonth++
                            }
                        },
                        modifier = Modifier.testTag("logger_next_month")
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                    }
                }
            }

            // Month Grid Columns Indicators (Su Mo Tu ...)
            Row(modifier = Modifier.fillMaxWidth()) {
                val weekdays = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
                weekdays.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            // Date Range selection calendar grid rendering
            val totalRows = (totalCells + 6) / 7
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (row in 0 until totalRows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (col in 0..6) {
                            val cellIdx = row * 7 + col
                            if (cellIdx >= totalCells || cellIdx < firstDayOfWeekIndex) {
                                Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                            } else {
                                val dayNum = cellIdx - firstDayOfWeekIndex + 1
                                val cellDate = LocalDate.of(calendarYear, calendarMonth, dayNum)

                                val isStart = selectedStartDate == cellDate
                                val isEnd = selectedEndDate == cellDate
                                val isInRange = selectedStartDate != null && selectedEndDate != null && 
                                        cellDate.isAfter(selectedStartDate) && cellDate.isBefore(selectedEndDate)
                                
                                val isSelected = isStart || isEnd || isInRange

                                // Check if this cell date is already overlapping or has an existing period logged
                                val alreadyLogged = periodLogs.any { log ->
                                    val logStart = try { LocalDate.parse(log.startDate) } catch (e: Exception) { null }
                                    val logEnd = try { if (log.endDate.isNullOrEmpty()) null else LocalDate.parse(log.endDate) } catch (e: Exception) { null }
                                    if (logStart != null) {
                                        if (logEnd != null) {
                                            (cellDate == logStart || cellDate == logEnd || (cellDate.isAfter(logStart) && cellDate.isBefore(logEnd)))
                                        } else {
                                            cellDate == logStart
                                        }
                                    } else {
                                        false
                                    }
                                }

                                val containerColor = when {
                                    isStart || isEnd -> MaterialTheme.colorScheme.primary
                                    isInRange -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    alreadyLogged -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                                    else -> Color.Transparent
                                }

                                val textColor = when {
                                    isStart || isEnd -> MaterialTheme.colorScheme.onPrimary
                                    isInRange -> MaterialTheme.colorScheme.onPrimaryContainer
                                    alreadyLogged -> MaterialTheme.colorScheme.onErrorContainer
                                    else -> MaterialTheme.colorScheme.onSurface
                                }

                                val shape = when {
                                    isStart && selectedEndDate != null -> RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50, topEndPercent = 0, bottomEndPercent = 0)
                                    isEnd -> RoundedCornerShape(topStartPercent = 0, bottomStartPercent = 0, topEndPercent = 50, bottomEndPercent = 50)
                                    isInRange -> RectangleShapeOrSquare() // Helper to get clean flat sides
                                    else -> CircleShape
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1.2f)
                                        .padding(vertical = 1.dp)
                                        .clip(shape)
                                        .background(containerColor)
                                        .border(
                                            width = if (cellDate == LocalDate.now() && !isSelected) 1.dp else 0.dp,
                                            color = if (cellDate == LocalDate.now() && !isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = shape
                                        )
                                        .clickable {
                                            // Selection mechanism
                                            val start = selectedStartDate
                                            val end = selectedEndDate
                                            
                                            when {
                                                start == null -> {
                                                    selectedStartDate = cellDate
                                                    selectedEndDate = null
                                                }
                                                end == null -> {
                                                    if (cellDate.isBefore(start)) {
                                                        selectedStartDate = cellDate
                                                        selectedEndDate = null
                                                    } else if (cellDate == start) {
                                                        selectedStartDate = null
                                                        selectedEndDate = null
                                                    } else {
                                                        selectedEndDate = cellDate
                                                    }
                                                }
                                                else -> {
                                                    selectedStartDate = cellDate
                                                    selectedEndDate = null
                                                }
                                            }
                                        }
                                        .testTag("logger_day_${calendarYear}_${calendarMonth}_${dayNum}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = dayNum.toString(),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isStart || isEnd || cellDate == LocalDate.now()) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = textColor
                                        )
                                        if (alreadyLogged && !isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.error)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 4.dp))

            // Display current selection with gorgeous Material Callout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Selected Range:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = buildString {
                            if (selectedStartDate != null) {
                                append(selectedStartDate!!.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)))
                            } else {
                                append("Select start date")
                            }
                            append(" ➔ ")
                            if (selectedEndDate != null) {
                                append(selectedEndDate!!.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)))
                            } else if (selectedStartDate != null) {
                                append("Ongoing")
                            } else {
                                append("Select end date")
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (selectedStartDate != null && selectedEndDate != null) {
                        val duration = java.time.temporal.ChronoUnit.DAYS.between(selectedStartDate, selectedEndDate) + 1
                        Text(
                            text = "$duration Day Period Duration",
                            style = MaterialTheme.typography.labelSmall,
                            color = SuccessGreen,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )
            }

            // Flow configuration options
            Text(
                text = "Flow Level", 
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            val levels = listOf("Spotting", "Light", "Medium", "Heavy", "Very heavy")
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                levels.forEach { level ->
                    val isSelected = flowLevel == level
                    val itemBg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    val itemTextColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(itemBg)
                            .clickable { flowLevel = level }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = level,
                            color = itemTextColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            if (flowLevel == "Very heavy" || selectedSymptoms.any { it in listOf("Dizziness", "Fainting", "Severe pain", "Fever", "Bad odor") }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = "Warning", tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LunaCare cannot diagnose the cause. These symptoms may require professional medical attention.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            
            // Product used
            Text(
                text = "Product Used", 
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            val productsList = listOf("Pad", "Period panty", "Menstrual cup", "Tampon", "Cloth", "Other", "Prefer not to say")
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                productsList.forEach { product ->
                    val isSelected = productUsed == product
                    FilterChip(
                        selected = isSelected,
                        onClick = { productUsed = if (isSelected) null else product },
                        label = { Text(product, fontSize = 11.sp) }
                    )
                }
            }
            
            // Change Frequency
            Text(
                text = "Change Frequency", 
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            val frequencyList = listOf("Every 1 hour", "Every 2–3 hours", "Every 4–6 hours", "Less often", "Not sure")
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                frequencyList.forEach { freq ->
                    val isSelected = changedProductFrequency == freq
                    FilterChip(
                        selected = isSelected,
                        onClick = { changedProductFrequency = if (isSelected) null else freq },
                        label = { Text(freq, fontSize = 11.sp) }
                    )
                }
            }

            // Quick Checklist Symptoms Tags
            Text(
                text = "Log Symptoms", 
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Flow Row of symptoms
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                symptomsList.forEach { symptom ->
                    val isChecked = selectedSymptoms.contains(symptom)
                    FilterChip(
                        selected = isChecked,
                        onClick = {
                            selectedSymptoms = if (isChecked) {
                                selectedSymptoms - symptom
                            } else {
                                selectedSymptoms + symptom
                            }
                        },
                        label = { Text(symptom, fontSize = 11.sp) },
                        leadingIcon = if (isChecked) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Personal physical notes (optional)", fontSize = 13.sp) },
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )

            // Submit Actions
            Button(
                onClick = {
                    val start = selectedStartDate
                    if (start != null) {
                        viewModel.addPeriodLog(
                            startDate = start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                            endDate = selectedEndDate?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                            flowLevel = flowLevel,
                            symptoms = selectedSymptoms.toList(),
                            notes = if (notes.isBlank()) null else notes,
                            painLevel = painLevel,
                            productUsed = productUsed,
                            changedProductFrequency = changedProductFrequency
                        )
                        // Trigger Success Indicator
                        showSuccessMessage = true
                        // Reset inputs
                        selectedStartDate = null
                        selectedEndDate = null
                        flowLevel = "Medium"
                        selectedSymptoms = emptySet()
                        notes = ""
                        painLevel = null
                        productUsed = null
                        changedProductFrequency = null
                        onLogSaved()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_calendar_log_button"),
                enabled = selectedStartDate != null,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Cycle Log via Calendar", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            // Spring animated confirmation banner
            AnimatedVisibility(
                visible = showSuccessMessage,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SuccessGreen.copy(alpha = 0.15f))
                        .border(1.dp, SuccessGreen.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = { showSuccessMessage = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Close Banner",
                                tint = SuccessGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Period Log Saved Successfully! 🎉",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                            Text(
                                text = "Your cycle predictions have been updated based on the new dates.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Diminish banner automatically after delay
                LaunchedEffect(showSuccessMessage) {
                    if (showSuccessMessage) {
                        kotlinx.coroutines.delay(4000)
                        showSuccessMessage = false
                    }
                }
            }
        }
    }
}

// Simple Helper for Flat Rectangles in Jetpack Compose range drawing
private fun RectangleShapeOrSquare(): RoundedCornerShape {
    return RoundedCornerShape(0.dp)
}

// Simple layout flow row implementation to avoid dependency issues on standard compose FlowRow
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable FlowRowScope.() -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = content
    )
}
