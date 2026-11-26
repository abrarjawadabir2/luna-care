package com.example.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

object CycleUtils {
    // STRICT resolver with 'uuuu' rejects impossible calendar dates such as 2026-02-29 or 2026-04-31.
    private val strictFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd", Locale.US)
        .withResolverStyle(ResolverStyle.STRICT)
    private val displayFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
    private val shortDisplayFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)

    /**
     * Parse entered cycle settings without silently replacing invalid input
     * with default values. A wide range accommodates unusual cycles;
     * these values are for personal tracking, not diagnosis.
     */
    fun parseCycleSettings(cycleText: String, periodText: String): Pair<Int, Int>? {
        val cycleDays = cycleText.trim().toIntOrNull() ?: return null
        val periodDays = periodText.trim().toIntOrNull() ?: return null
        if (cycleDays !in 1..365 || periodDays !in 1..cycleDays) return null
        return cycleDays to periodDays
    }

    fun getTodayString(): String {
        return LocalDate.now().format(strictFormatter)
    }

    fun parseDate(dateStr: String): LocalDate {
        return try {
            LocalDate.parse(dateStr, strictFormatter)
        } catch (e: Exception) {
            LocalDate.now()
        }
    }

    fun formatDate(date: LocalDate): String {
        return date.format(strictFormatter)
    }

    fun formatDisplayDate(dateStr: String): String {
        return try {
            val date = LocalDate.parse(dateStr, strictFormatter)
            date.format(displayFormatter)
        } catch (e: Exception) {
            dateStr
        }
    }

    fun formatShortDate(dateStr: String): String {
        return try {
            val date = LocalDate.parse(dateStr, strictFormatter)
            date.format(shortDisplayFormatter)
        } catch (e: Exception) {
            dateStr
        }
    }

    fun getDaysBetween(startStr: String, endStr: String): Long {
        return try {
            val start = LocalDate.parse(startStr, strictFormatter)
            val end = LocalDate.parse(endStr, strictFormatter)
            ChronoUnit.DAYS.between(start, end)
        } catch (e: Exception) {
            0
        }
    }

    fun addDays(dateStr: String, days: Int): String {
        return try {
            val date = LocalDate.parse(dateStr, strictFormatter)
            date.plusDays(days.toLong()).format(strictFormatter)
        } catch (e: Exception) {
            dateStr
        }
    }

    fun getCurrentCycleDay(lastPeriodStart: String, todayStr: String = getTodayString()): Int {
        val days = getDaysBetween(lastPeriodStart, todayStr)
        return (days + 1).toInt()
    }

    fun getPredictedNextPeriod(lastPeriodStart: String, cycleLength: Int): String {
        return addDays(lastPeriodStart, cycleLength)
    }

    fun getCyclePhase(cycleDay: Int, cycleLength: Int, periodLength: Int): String {
        if (cycleDay <= 0 || cycleLength <= 0) {
            return "Upcoming"
        }
        val safePeriod = periodLength.coerceIn(1, (cycleLength - 1).coerceAtLeast(1))
        val ovulationCenter = (cycleLength / 2).coerceAtLeast(safePeriod + 1)
        val follicularEnd = ovulationCenter - 2
        return when {
            cycleDay in 1..safePeriod -> "Menstrual Phase"
            follicularEnd >= safePeriod + 1 && cycleDay in (safePeriod + 1)..follicularEnd -> "Follicular Phase"
            cycleDay in (ovulationCenter - 1)..(ovulationCenter + 1) -> "Ovulatory Phase"
            else -> "Luteal Phase"
        }
    }

    fun isValidDate(dateStr: String): Boolean {
        if (dateStr.length != 10) return false
        return try {
            LocalDate.parse(dateStr, strictFormatter)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun isValidDateRange(startDate: String, endDate: String?): Boolean {
        if (!isValidDate(startDate)) return false
        if (endDate.isNullOrBlank()) return true
        if (!isValidDate(endDate)) return false
        return !LocalDate.parse(endDate, strictFormatter).isBefore(LocalDate.parse(startDate, strictFormatter))
    }

    fun getPhaseDescription(phase: String): String {
        return when (phase) {
            "Menstrual Phase" -> "Your body is shedding the uterine lining. Rest and gentle heat are your best companions."
            "Follicular Phase" -> "Estrogen rises, energy builds up. A perfect phase for learning, socializing, and creative ideas."
            "Ovulatory Phase" -> "Peak hormonal phase. You may feel highly confident, communicative, and physically active."
            "Luteal Phase" -> "Progesterone takes over, soothing the body. PMS may develop. Focus on warm comforts and private calm."
            "Upcoming" -> "Your recorded start date is in the future. Cycle day estimates will begin on that date."
            else -> "A peaceful phase of your cycle."
        }
    }
}
