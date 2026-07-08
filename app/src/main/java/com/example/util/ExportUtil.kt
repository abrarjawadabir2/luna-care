package com.example.util

import com.example.data.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportUtil {
    fun generateHealthLogsSummary(
        profile: Profile?,
        periodLogs: List<PeriodLog>,
        moodLogs: List<MoodLog>,
        journalEntries: List<JournalEntry>
    ): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        val sb = StringBuilder()

        sb.append("LUNACARE HEALTH LOGS SUMMARY\n")
        sb.append("=============================\n\n")

        if (profile != null) {
            sb.append("PROFILE\n")
            sb.append("-----------------------------\n")
            sb.append("Name: ${profile.displayName.ifEmpty { "Not set" }}\n")
            sb.append("Average Cycle Length: ${profile.averageCycleLength} days\n")
            sb.append("Average Period Length: ${profile.averagePeriodLength} days\n\n")
        }

        sb.append("PERIOD LOGS (${periodLogs.size})\n")
        sb.append("-----------------------------\n")
        if (periodLogs.isEmpty()) {
            sb.append("No period logs found.\n")
        } else {
            periodLogs.sortedByDescending { it.startDate }.forEach { log ->
                val endStr = log.endDate ?: "Ongoing"
                sb.append("- ${log.startDate} to $endStr (Flow: ${log.flowLevel})\n")
                if (log.symptoms.isNotEmpty()) {
                    sb.append("  Symptoms: ${log.symptoms.joinToString(", ")}\n")
                }
            }
        }
        sb.append("\n")

        sb.append("MOOD LOGS (${moodLogs.size})\n")
        sb.append("-----------------------------\n")
        if (moodLogs.isEmpty()) {
            sb.append("No mood logs found.\n")
        } else {
            moodLogs.sortedByDescending { it.date }.forEach { log ->
                sb.append("- ${log.date}: ${log.mood} (Energy: ${log.energy}/10, Stress: ${log.stress}/10)\n")
                if (!log.notes.isNullOrBlank()) {
                    sb.append("  Notes: ${log.notes}\n")
                }
            }
        }
        sb.append("\n")

        sb.append("JOURNAL ENTRIES (${journalEntries.size})\n")
        sb.append("-----------------------------\n")
        if (journalEntries.isEmpty()) {
            sb.append("No journal entries found.\n")
        } else {
            journalEntries.sortedByDescending { it.date }.forEach { entry ->
                sb.append("- ${entry.date}: ${entry.title}\n")
                sb.append("  ${entry.body}\n")
            }
        }
        sb.append("\n")
        
        sb.append("=============================\n")
        sb.append("Exported on ${sdf.format(Date())}\n")

        return sb.toString()
    }
}
