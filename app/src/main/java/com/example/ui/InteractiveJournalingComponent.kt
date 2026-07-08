package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import java.time.LocalDate
import com.example.data.CycleUtils
import com.example.data.JournalEntry
import com.example.ui.theme.*
import com.example.viewmodel.LunaViewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.entry.FloatEntry
import java.time.format.DateTimeFormatter

private val JOURNAL_SYMPTOM_OPTIONS = listOf(
    "Cramps" to "⚡",
    "Headache" to "🧠",
    "Fatigue" to "💤",
    "Bloating" to "🎈",
    "Backache" to "🩹",
    "Nausea" to "🤢",
    "Mood Swings" to "🎭",
    "Insomnia" to "🦉"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractiveJournalingComponent(
    viewModel: LunaViewModel,
    journalEntries: List<JournalEntry>,
    modifier: Modifier = Modifier
) {
    // Current input state
    var journalTitle by remember { mutableStateOf("") }
    var journalBody by remember { mutableStateOf("") }
    var selectedMood by remember { mutableStateOf<MoodOption?>(null) }
    var selectedCategory by remember { mutableStateOf("General") }
    var selectedSymptoms by remember { mutableStateOf(setOf<String>()) }
    var searchKeyword by remember { mutableStateOf("") }
    var selectedMoodFilter by remember { mutableStateOf<String>("All") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    
    // UI Expand/Collapse states
    var isWritingActive by remember { mutableStateOf(false) }
    var showSuccessBanner by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf("write") } // "write", "trends"
    
    val moods = listOf(
        MoodOption("Radiant", "💖", MutedRosePrimary, PrimaryContainer.copy(alpha = 0.25f)),
        MoodOption("Peaceful", "🌸", LavenderSecondary, SecondaryContainer.copy(alpha = 0.25f)),
        MoodOption("Calm", "🍵", SuccessGreen, SuccessGreen.copy(alpha = 0.25f)),
        MoodOption("Tired", "💤", WarningAmber, WarningAmber.copy(alpha = 0.25f)),
        MoodOption("Low", "💧", Color(0xFF4A90E2), Color(0xFF4A90E2).copy(alpha = 0.2f)),
        MoodOption("Stressed", "⚡", AlertRed, AlertRed.copy(alpha = 0.15f))
    )

    val journalSymptomOptions = JOURNAL_SYMPTOM_OPTIONS

    val moodAffirmations = mapOf(
        "Radiant" to "Full of energy, sunshine, and positive vibes 🌟",
        "Peaceful" to "Serene, content, and beautifully in flow 🌸",
        "Calm" to "Grounded, quiet, and peacefully present 🍵",
        "Tired" to "Low on energy, needing deep restorative rest 💤",
        "Low" to "Quiet, gentle with yourself, emotional space needed 💧",
        "Stressed" to "Feeling overwhelmed, taking a deep breath ⚡"
    )

    val reflectionPrompts = listOf(
        "What is one small thing you are truly grateful for today?",
        "How does your physical body feel right now?",
        "Describe a kind message you want to tell yourself today.",
        "What helped you feel calm, present, or supported today?",
        "Is there any tension you want to release today?"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("interactive_journal_component"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        // 1. Gorgeous offline privacy card with a shield icon
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
            ),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Privacy Secure",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Your Privacy is Fully Protected",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Reflections are kept strictly in a sandboxed SQLite Room database on this physical phone. Zero server syncing, zero tracking, 100% offline security.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Tab Row Switcher
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { activeTab = "write" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeTab == "write") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (activeTab == "write") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(42.dp).testTag("journal_write_tab")
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Write Reflection", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { activeTab = "trends" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeTab == "trends") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (activeTab == "trends") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(42.dp).testTag("journal_trends_tab")
            ) {
                Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Wellbeing Trends", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (activeTab == "write") {
            // 2. Writing section (collapsible/expandable for clean space management)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(
                width = 1.5.dp,
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
                // Expandable Writing Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isWritingActive = !isWritingActive }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "How are you feeling today?",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (!isWritingActive) {
                                Text(
                                    text = "Tap to record your mood & write down reflections",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                    
                    IconButton(
                        onClick = { isWritingActive = !isWritingActive },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isWritingActive) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isWritingActive) "Collapse Writer" else "Expand Writer",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isWritingActive,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // A. Mood Picker
                        Text(
                            text = "1. Log Your Current Mood",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            moods.forEach { option ->
                                val isSelected = selectedMood == option
                                
                                // Bouncy scaling animation for tactile physical feedback
                                val scale by animateFloatAsState(
                                    targetValue = if (isSelected) 1.25f else 1.0f,
                                    animationSpec = spring(
                                        dampingRatio = 0.5f, // Highly organic bouncy feedback
                                        stiffness = 250f
                                    ),
                                    label = "emojiScale"
                                )

                                // Upward float when selected
                                val translationY by animateFloatAsState(
                                    targetValue = if (isSelected) (-5f) else 0f,
                                    animationSpec = spring(
                                        dampingRatio = 0.6f,
                                        stiffness = 300f
                                    ),
                                    label = "emojiTranslation"
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                            this.translationY = translationY
                                        }
                                        .padding(horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) option.selectedColor 
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) option.primaryColor 
                                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                                                shape = CircleShape
                                            )
                                            .clickable { 
                                                selectedMood = if (isSelected) null else option 
                                            }
                                            .testTag("emoji_mood_button_${option.name}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = option.emoji,
                                            fontSize = 28.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        // Beautiful animated description helper instead of crowded text lists
                        AnimatedContent(
                            targetState = selectedMood,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                            },
                            label = "moodAffirmationContent"
                        ) { targetMood ->
                            if (targetMood != null) {
                                val affirmation = moodAffirmations[targetMood.name] ?: "Logging ${targetMood.name}"
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = targetMood.selectedColor.copy(alpha = 0.15f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = targetMood.emoji,
                                            fontSize = 18.sp
                                        )
                                        Column {
                                            Text(
                                                text = targetMood.name,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = targetMood.primaryColor
                                            )
                                            Text(
                                                text = affirmation,
                                                fontSize = 10.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = "✨ Tap an emoji to express how you are feeling right now",
                                    fontSize = 11.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                )
                            }
                        }

                        // B. Prompt suggestions Carousel
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "💡 Interactive Reflection Prompts",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Select a prompt below to inspire your private diary writing:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(reflectionPrompts) { prompt ->
                                val isChosen = journalBody.contains(prompt)
                                Card(
                                    modifier = Modifier
                                        .width(220.dp)
                                        .clickable {
                                            if (!isChosen) {
                                                journalBody = if (journalBody.isBlank()) {
                                                    "Prompt: $prompt\n\n"
                                                } else {
                                                    "$journalBody\n\nPrompt: $prompt\n\n"
                                                }
                                                if (journalTitle.isBlank()) {
                                                    journalTitle = "Daily Reflection"
                                                }
                                            }
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isChosen) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    ),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isChosen) MaterialTheme.colorScheme.primary else Color.Transparent
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = prompt,
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        // C. Entry Inputs
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "2. Compose Reflection",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = journalTitle,
                            onValueChange = { journalTitle = it },
                            label = { Text("Title (e.g. Grateful Morning, Quiet Evening)", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth().testTag("journal_title_input")
                        )

                        OutlinedTextField(
                            value = journalBody,
                            onValueChange = { journalBody = it },
                            label = { Text("How was your day? What are you holding onto or letting go of?", fontSize = 12.sp) },
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .testTag("journal_body_input")
                        )

                        // Characters and Mood warning validation
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Chars: ${journalBody.length}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            if (selectedMood == null) {
                                Text(
                                    text = "⚠️ Please pick a mood above",
                                    fontSize = 11.sp,
                                    color = WarningAmber,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // 3. Physical Symptoms Checklist
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "3. Check Physical Symptoms",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Select any symptoms to correlate physical health with your mood:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            journalSymptomOptions.forEach { (symptomName, emoji) ->
                                val isSelected = selectedSymptoms.contains(symptomName)
                                Card(
                                    modifier = Modifier
                                        .clickable {
                                            selectedSymptoms = if (isSelected) {
                                                selectedSymptoms - symptomName
                                            } else {
                                                selectedSymptoms + symptomName
                                            }
                                        }
                                        .testTag("journal_symptom_chip_$symptomName"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    ),
                                    border = BorderStroke(
                                        width = 1.5.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(emoji, fontSize = 14.sp)
                                        Text(
                                            text = symptomName,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // D. Category Tagging Selection
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "4. Category Tag",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Tag your entry for easier organization and trends:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            items(journalCategories) { cat ->
                                val isSelected = selectedCategory == cat.name
                                Card(
                                    modifier = Modifier
                                        .clickable { selectedCategory = cat.name }
                                        .testTag("category_selector_${cat.name}"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) cat.color.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    ),
                                    border = BorderStroke(
                                        width = 1.5.dp,
                                        color = if (isSelected) cat.color else Color.Transparent
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(cat.emoji, fontSize = 13.sp)
                                        Text(
                                            text = cat.name,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) cat.color else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Save Button
                        Button(
                            onClick = {
                                if (journalBody.isNotBlank()) {
                                    val finalTitle = if (journalTitle.isBlank()) {
                                        selectedMood?.let { "${it.emoji} ${it.name} Reflections" } ?: "Daily Reflections"
                                    } else {
                                        journalTitle
                                    }
                                    
                                    val symptomsString = if (selectedSymptoms.isNotEmpty()) {
                                        selectedSymptoms.joinToString(",")
                                    } else {
                                        null
                                    }
                                    
                                    viewModel.addJournalEntry(
                                        title = finalTitle,
                                        body = journalBody,
                                        moodTag = selectedMood?.name,
                                        cyclePhase = null,
                                        category = selectedCategory,
                                        symptoms = symptomsString
                                    )
                                    
                                    // Reset fields
                                    journalTitle = ""
                                    journalBody = ""
                                    selectedMood = null
                                    selectedCategory = "General"
                                    selectedSymptoms = emptySet()
                                    isWritingActive = false
                                    showSuccessBanner = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_journal_entry_btn"),
                            enabled = journalBody.isNotBlank() && selectedMood != null,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Private Entry", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // 3. Save Confirmation banner
        AnimatedVisibility(
            visible = showSuccessBanner,
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
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = SuccessGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Reflection Logged Securely! 🎉",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                        Text(
                            text = "Your private thoughts have been stored locally. No cloud sync requested.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { showSuccessBanner = false },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Banner",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                LaunchedEffect(showSuccessBanner) {
                    if (showSuccessBanner) {
                        kotlinx.coroutines.delay(4000)
                        showSuccessBanner = false
                    }
                }
            }
        }

        // 4. Search and Filter row
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "My Private Reflection History",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Search Bar
            OutlinedTextField(
                value = searchKeyword,
                onValueChange = { searchKeyword = it },
                placeholder = { Text("Search title, body, or thoughts...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) },
                trailingIcon = if (searchKeyword.isNotEmpty()) {
                    {
                        IconButton(onClick = { searchKeyword = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                        }
                    }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().testTag("journal_history_search")
            )

            // Filter chips row (Moods)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChip(
                        selected = selectedMoodFilter == "All",
                        onClick = { selectedMoodFilter = "All" },
                        label = { Text("All Moods", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                items(moods) { option ->
                    val isChecked = selectedMoodFilter == option.name
                    FilterChip(
                        selected = isChecked,
                        onClick = { selectedMoodFilter = if (isChecked) "All" else option.name },
                        label = { Text("${option.emoji} ${option.name}", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = option.selectedColor,
                            selectedLabelColor = option.primaryColor
                        )
                    )
                }
            }

            // Filter chips row (Categories)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChip(
                        selected = selectedCategoryFilter == "All",
                        onClick = { selectedCategoryFilter = "All" },
                        label = { Text("All Categories", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }

                items(journalCategories) { cat ->
                    val isChecked = selectedCategoryFilter == cat.name
                    FilterChip(
                        selected = isChecked,
                        onClick = { selectedCategoryFilter = if (isChecked) "All" else cat.name },
                        label = { Text("${cat.emoji} ${cat.name}", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = cat.color.copy(alpha = 0.2f),
                            selectedLabelColor = cat.color
                        )
                    )
                }
            }
        }

        // 5. Entries List
        val filteredEntries = journalEntries.filter { entry ->
            val matchKeyword = searchKeyword.isBlank() || 
                    entry.title.contains(searchKeyword, ignoreCase = true) ||
                    entry.body.contains(searchKeyword, ignoreCase = true)
            
            val matchMood = selectedMoodFilter == "All" || entry.moodTag == selectedMoodFilter
            
            val matchCategory = selectedCategoryFilter == "All" || entry.category == selectedCategoryFilter
            
            matchKeyword && matchMood && matchCategory
        }.sortedByDescending { it.date }

        if (filteredEntries.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = if (searchKeyword.isNotEmpty() || selectedMoodFilter != "All" || selectedCategoryFilter != "All") "No matches found" else "No reflections saved yet",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (searchKeyword.isNotEmpty() || selectedMoodFilter != "All" || selectedCategoryFilter != "All") 
                            "Try modifying your filter, category, or keyword to locate entries."
                            else "Your logged moods and private thoughts will be safely listed here.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                filteredEntries.forEach { entry ->
                    val matchingMoodOption = moods.find { it.name == entry.moodTag }
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("journal_history_item_${entry.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = matchingMoodOption?.primaryColor?.copy(alpha = 0.2f) 
                                ?: MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Text(
                                            text = CycleUtils.formatDisplayDate(entry.date),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        
                                        // Category Badge
                                        val catObj = journalCategories.find { it.name == entry.category }
                                        val displayEmoji = catObj?.emoji ?: "📝"
                                        val displayColor = catObj?.color ?: Color.Gray
                                        
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(displayColor.copy(alpha = 0.12f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Text(displayEmoji, fontSize = 9.sp)
                                                Text(
                                                    text = entry.category,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = displayColor
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Mood Tag Badge
                                    if (matchingMoodOption != null) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(matchingMoodOption.selectedColor)
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(matchingMoodOption.emoji, fontSize = 12.sp)
                                                Text(
                                                    text = matchingMoodOption.name,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = matchingMoodOption.primaryColor
                                                )
                                            }
                                        }
                                    }
                                    
                                    IconButton(
                                        onClick = { viewModel.deleteJournalEntry(entry.id) },
                                        modifier = Modifier.size(28.dp).testTag("delete_journal_${entry.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Entry",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                            
                            Text(
                                text = entry.body,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 17.sp
                            )

                            if (!entry.symptoms.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Logged Symptoms:",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        val symptomsList = entry.symptoms.split(",").filter { it.isNotBlank() }
                                        symptomsList.forEach { symptom ->
                                            val emoji = journalSymptomOptions.find { it.first == symptom }?.second ?: "⚡"
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                    .border(
                                                        width = 1.dp,
                                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                                                        shape = RoundedCornerShape(6.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                ) {
                                                    Text(emoji, fontSize = 9.sp)
                                                    Text(
                                                        text = symptom,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
        } else {
            WellbeingTrendsDashboard(viewModel, journalEntries, moods)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WellbeingTrendsDashboard(
    viewModel: LunaViewModel,
    journalEntries: List<JournalEntry>,
    moods: List<MoodOption>
) {
    val activeEntries = journalEntries.filter { it.moodTag != null }
    val moodWeights = mapOf(
        "Radiant" to 5,
        "Peaceful" to 4,
        "Calm" to 3,
        "Tired" to 2,
        "Low" to 1,
        "Stressed" to 0
    )

    // Calculate score
    val avgScore = if (activeEntries.isNotEmpty()) {
        activeEntries.map { moodWeights[it.moodTag] ?: 3 }.average()
    } else {
        3.0
    }
    val wellBeingIndex = (avgScore / 5.0 * 100).toInt()

    // Find dominant mood
    val dominantMoodName = if (activeEntries.isNotEmpty()) {
        activeEntries.groupBy { it.moodTag }.maxByOrNull { it.value.size }?.key
    } else {
        null
    }
    val dominantMood = moods.find { it.name == dominantMoodName }

    // Calculate weekly streak (consecutive days of journal entries)
    val today = LocalDate.now()
    var streak = 0
    var checkDate = today
    val datesSet = journalEntries.map { it.date }.toSet()
    while (datesSet.contains(checkDate.toString())) {
        streak++
        checkDate = checkDate.minusDays(1)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Wellbeing Quick Stat Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left Stat Card: Wellbeing Index
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Wellbeing Index",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(60.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { avgScore.toFloat() / 5f },
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 6.dp,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        )
                        Text(
                            text = "$wellBeingIndex%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Right Stat Card: Current Streak
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Reflective Streak",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    
                    Text(
                        text = "🔥 $streak days",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    
                    Text(
                        text = if (streak > 0) "Keep it up!" else "Begin your streak!",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Custom Well-being Progression Timeline Chart
        WellbeingTrendChart(journalEntries, moods)

        // Dominant Mood Frequency Breakdown Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Mood Frequency Breakdown",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (activeEntries.isEmpty()) {
                    Text(
                        text = "No logs yet. Statistics will appear as you check in.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                } else {
                    val totalLogs = activeEntries.size
                    moods.forEach { mood ->
                        val count = activeEntries.count { it.moodTag == mood.name }
                        val pct = if (totalLogs > 0) count.toFloat() / totalLogs.toFloat() else 0f

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(mood.emoji, fontSize = 20.sp, modifier = Modifier.width(24.dp))
                            Text(mood.name, fontSize = 12.sp, modifier = Modifier.width(64.dp), fontWeight = FontWeight.Medium)
                            
                            Box(modifier = Modifier.weight(1f).height(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(pct)
                                        .clip(CircleShape)
                                        .background(mood.primaryColor)
                                )
                            }
                            
                            Text(
                                text = "$count (${(pct * 100).toInt()}%)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = mood.primaryColor,
                                modifier = Modifier.width(44.dp),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }

        // Physical Symptoms & Mood Correlations Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Physical Symptom & Mood Correlations",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "Understand how your physical sensations relate to your logged emotional states.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                val symptomEntries = journalEntries.filter { !it.symptoms.isNullOrBlank() }
                
                if (symptomEntries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "💡 Log physical symptoms with your mood in the journal entry form above to unlock your correlation insights!",
                            fontSize = 11.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    val symptomToMoods = mutableMapOf<String, MutableList<String>>()
                    symptomEntries.forEach { entry ->
                        val entrySymptoms = entry.symptoms?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
                        val entryMood = entry.moodTag
                        if (entryMood != null) {
                            entrySymptoms.forEach { symptom ->
                                symptomToMoods.getOrPut(symptom) { mutableListOf() }.add(entryMood)
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        symptomToMoods.forEach { (symptomName, moodList) ->
                            val totalSymptomLogs = moodList.size
                            val dominantMoodName = moodList.groupBy { it }.maxByOrNull { it.value.size }?.key
                            val dominantMoodCount = moodList.count { it == dominantMoodName }
                            val pct = (dominantMoodCount.toFloat() / totalSymptomLogs.toFloat() * 100).toInt()
                            
                            val matchingMoodOption = moods.find { it.name == dominantMoodName }
                            val symptomEmoji = JOURNAL_SYMPTOM_OPTIONS.find { it.first == symptomName }?.second ?: "⚡"
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(symptomEmoji, fontSize = 18.sp)
                                        }
                                        
                                        Column {
                                            Text(
                                                text = symptomName,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Logged $totalSymptomLogs ${if (totalSymptomLogs == 1) "time" else "times"}",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                    
                                    if (matchingMoodOption != null) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "Dominant Mood",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(matchingMoodOption.selectedColor)
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                    ) {
                                                        Text(matchingMoodOption.emoji, fontSize = 10.sp)
                                                        Text(
                                                            text = matchingMoodOption.name,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = matchingMoodOption.primaryColor
                                                        )
                                                    }
                                                }
                                            }
                                            
                                            Box(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(matchingMoodOption.primaryColor.copy(alpha = 0.12f))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "$pct%",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = matchingMoodOption.primaryColor
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

        // Emotional Health Guidance Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Spa,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Sanctuary Wellbeing Guidance",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                val guidanceText = when (dominantMood?.name) {
                    "Radiant" -> "Your emotional state is vibrant and thriving! Take this beautiful wave of positivity to do something creative, spread kindness to a friend, or capture your happy thoughts in detailed reflection logs."
                    "Peaceful" -> "A state of calm clarity is a beautiful space to inhabit. This steady equilibrium is perfect for reflective walks, journaling with focus questions, and anchoring yourself in mindfulness."
                    "Calm" -> "You are experiencing a quiet, pleasant stability. Enjoy this gentle pace. It is an excellent time to read, meditate, and enjoy slow moments of private peace."
                    "Tired" -> "Your body is whispering to you to slow down. Please respect this need for recovery. Reduce physical/mental output, close your screen 1 hour before sleep, and drink warm comforting tea."
                    "Low" -> "It is completely okay to feel blue or have lower vitality today. Give yourself absolute permission to just exist without expectations. Rest, self-compassion, and gentle movement are supportive."
                    "Stressed" -> "Your system is in high alert. Let's ground ourselves together: practice a 4-7-8 breathing loop, un-clench your jaw, lower your shoulders, and write down your concerns to release them."
                    else -> "Track your daily emotional health with text reflection logs and mood selection icons. Your trends and holistic offline insights will dynamically update here to guide your wellness journey."
                }

                Text(
                    text = guidanceText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }

        // Demo data seeder & reset buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { seedWellbeingHistory(viewModel) },
                modifier = Modifier.weight(1f).testTag("seed_journal_demo_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Seed Demo Logs", fontSize = 11.sp)
            }

            OutlinedButton(
                onClick = {
                    activeEntries.forEach { viewModel.deleteJournalEntry(it.id) }
                },
                modifier = Modifier.weight(1f).testTag("clear_journal_btn"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset History", fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun WellbeingTrendChart(entries: List<JournalEntry>, moods: List<MoodOption>) {
    var selectedTimeframe by remember { mutableStateOf(1) } // 0 = 7 Entries, 1 = 30 Days

    val moodWeights = mapOf(
        "Radiant" to 5f,
        "Peaceful" to 4f,
        "Calm" to 3f,
        "Tired" to 2f,
        "Low" to 1f,
        "Stressed" to 0f
    )

    val filteredEntries = remember(entries, selectedTimeframe) {
        val withMood = entries.filter { it.moodTag != null }.sortedBy { it.date }
        if (selectedTimeframe == 0) {
            withMood.takeLast(7)
        } else {
            withMood.takeLast(30)
        }
    }

    if (filteredEntries.size < 2) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Awaiting More Data",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Track at least 2 entries with mood tags to visualize your 30-day emotional progression.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    val chartEntryModel = remember(filteredEntries) {
        val entries = filteredEntries.map { entry ->
            moodWeights[entry.moodTag] ?: 3f
        }
        entryModelOf(*entries.toTypedArray())
    }

    val dateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd")

    Card(
        modifier = Modifier.fillMaxWidth().testTag("wellbeing_trend_chart_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(0.6f)) {
                    Text(
                        text = "📈 Wellbeing Progression",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Mood trends over time",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Row(
                    modifier = Modifier
                        .wrapContentSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("7 Logs", "30 Days").forEachIndexed { idx, label ->
                        val isSelected = selectedTimeframe == idx
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { selectedTimeframe = idx }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Chart(
                chart = lineChart(),
                model = chartEntryModel,
                startAxis = rememberStartAxis(
                    valueFormatter = { value, _ ->
                        val weight = value.toInt()
                        moodWeights.entries.find { it.value.toInt() == weight }?.key ?: ""
                    }
                ),
                bottomAxis = rememberBottomAxis(
                    valueFormatter = { value, _ ->
                        val index = value.toInt()
                        if (index >= 0 && index < filteredEntries.size) {
                            try {
                                val date = LocalDate.parse(filteredEntries[index].date)
                                date.format(dateTimeFormatter)
                            } catch (e: Exception) {
                                ""
                            }
                        } else {
                            ""
                        }
                    },
                    labelRotationDegrees = -45f
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
    }
}

fun seedWellbeingHistory(viewModel: LunaViewModel) {
    val today = LocalDate.now()
    val titlesAndBodies = listOf(
        Triple("Cozy Sunday Reset", "Woke up without an alarm. Made blueberry pancakes, read in bed, and listened to the peaceful rain outside. Heart feels exceptionally light.", "Peaceful"),
        Triple("Stressed Monday Morning", "Felt highly anxious about the upcoming project review. Spent 5 mins on deep abdominal breathing loops. Feeling slightly stabilized.", "Stressed"),
        Triple("Quiet Walk Outdoors", "Took a short walk outside under the sun. The breeze felt lovely on my face. Feeling a bit of mental fatigue releasing.", "Calm"),
        Triple("Quiet Reflections", "Read a chapter of my book and drank warm chamomile tea. Felt peaceful and grounded in my cozy reading chair.", "Peaceful"),
        Triple("Restless Night Struggles", "Tossed and turned. Work anxiety kept me awake for hours. Need to strictly limit screen time tonight.", "Tired"),
        Triple("Gentle Morning Start", "Slept in a bit. Took a warm shower and ate a healthy breakfast. Feeling more stable and present today.", "Calm"),
        Triple("Radiant Coffee Meetup", "Had an amazing conversation with a dear friend. Shared some hearty laughs and felt a strong, deep sense of belonging.", "Radiant"),
        Triple("Low Focus Day", "Felt a bit sluggish and blue. Didn't get much done, but reminded myself that productivity doesn't define my worth.", "Low"),
        Triple("Peaceful Solitude", "Spent the evening gardening and planting herbs. Connecting with the soil helped quiet my racing thoughts.", "Peaceful"),
        Triple("Productive Breakthrough", "Completed my personal goal for the week! Celebrated with a self-care evening. Feeling highly radiant and proud of the progress.", "Radiant"),
        Triple("Cozy Rainy Reading", "Rainy afternoon perfect for hot tea and fiction. Allowed myself to fully disconnect from notifications.", "Peaceful"),
        Triple("Midweek Overwhelmed", "Inbox is overflowing. Feeling pulling in too many directions. Scheduled a deep breathing block.", "Stressed"),
        Triple("Mindful Evening Yoga", "Did a gentle 15-minute restorative yoga flow. Stretched out back tension. Sleeping much better tonight.", "Calm"),
        Triple("Sluggish Afternoon slump", "Felt a bit of brain fog and slow cognitive speed. Stepped away to stretch and hydrate.", "Tired"),
        Triple("Deep Heart-to-Heart", "Had a highly authentic call with family. Feeling very emotionally supported and peaceful.", "Peaceful"),
        Triple("Stunning Golden Sunset", "Walked around the park during sunset. Sky was a brilliant shade of purple and orange. Nature is medicine.", "Radiant"),
        Triple("Slow Creative Evening", "Sketched for an hour while listening to ambient music. No goals, just pure visual expression.", "Calm"),
        Triple("Restful Deep Sleep", "Slept a solid 8.5 hours. Energy levels are elevated today! Ready to conquer the tasks ahead.", "Radiant")
    )
    
    titlesAndBodies.forEachIndexed { index, (title, body, mood) ->
        val dateStr = today.minusDays((24 - index).toLong()).toString()
        val category = when (mood) {
            "Radiant" -> "Self-Care"
            "Peaceful" -> "Reflection"
            "Calm" -> "Self-Care"
            "Tired" -> "Symptoms"
            "Stressed" -> "Mental Health"
            "Low" -> "Reflection"
            else -> "General"
        }
        viewModel.addJournalEntryWithDate(
            date = dateStr,
            title = title,
            body = body,
            moodTag = mood,
            category = category
        )
    }
}

data class MoodOption(
    val name: String,
    val emoji: String,
    val primaryColor: Color,
    val selectedColor: Color
)

data class JournalCategory(
    val name: String,
    val emoji: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)

val journalCategories = listOf(
    JournalCategory("General", "📝", Icons.Default.Edit, Color(0xFF6B7280)), // Slate gray
    JournalCategory("Self-Care", "🌸", Icons.Default.Spa, Color(0xFFE27396)), // Rose/pink
    JournalCategory("Mental Health", "🧠", Icons.Default.CheckCircle, Color(0xFF4A90E2)), // Bright Blue
    JournalCategory("Symptoms", "🤒", Icons.Default.Timeline, Color(0xFFE07A5F)), // Coral/Orange
    JournalCategory("Gratitude", "✨", Icons.Default.Favorite, Color(0xFFD6A045)), // Gold/Amber
    JournalCategory("Reflection", "💭", Icons.Default.MenuBook, Color(0xFF81B29A)) // Sage Green
)
