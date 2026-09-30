package com.example.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object CycleUtils {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val displayFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
    private val shortDisplayFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)

    fun getTodayString(): String {
        return LocalDate.now().format(formatter)
    }

    fun parseDate(dateStr: String): LocalDate {
        return try {
            LocalDate.parse(dateStr, formatter)
        } catch (e: Exception) {
            LocalDate.now()
        }
    }

    fun formatDate(date: LocalDate): String {
        return date.format(formatter)
    }

    fun formatDisplayDate(dateStr: String): String {
        return try {
            val date = LocalDate.parse(dateStr, formatter)
            date.format(displayFormatter)
        } catch (e: Exception) {
            dateStr
        }
    }

    fun formatShortDate(dateStr: String): String {
        return try {
            val date = LocalDate.parse(dateStr, formatter)
            date.format(shortDisplayFormatter)
        } catch (e: Exception) {
            dateStr
        }
    }

    fun getDaysBetween(startStr: String, endStr: String): Long {
        return try {
            val start = LocalDate.parse(startStr, formatter)
            val end = LocalDate.parse(endStr, formatter)
            ChronoUnit.DAYS.between(start, end)
        } catch (e: Exception) {
            0
        }
    }

    fun addDays(dateStr: String, days: Int): String {
        return try {
            val date = LocalDate.parse(dateStr, formatter)
            date.plusDays(days.toLong()).format(formatter)
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
        return when {
            cycleDay in 1..periodLength -> "Menstrual Phase"
            cycleDay in (periodLength + 1)..(cycleLength / 2 - 2) -> "Follicular Phase"
            cycleDay in (cycleLength / 2 - 1)..(cycleLength / 2 + 1) -> "Ovulatory Phase"
            else -> "Luteal Phase"
        }
    }

    fun isValidDate(dateStr: String): Boolean {
        return try {
            LocalDate.parse(dateStr, formatter)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getPhaseDescription(phase: String): String {
        return when (phase) {
            "Menstrual Phase" -> "Your body is shedding the uterine lining. Rest and gentle heat are your best companions."
            "Follicular Phase" -> "Estrogen rises, energy builds up. A perfect phase for learning, socializing, and creative ideas."
            "Ovulatory Phase" -> "Peak hormonal phase. You may feel highly confident, communicative, and physically active."
            "Luteal Phase" -> "Progesterone takes over, soothing the body. PMS may develop. Focus on warm comforts and private calm."
            else -> "A peaceful phase of your cycle."
        }
    }
}
