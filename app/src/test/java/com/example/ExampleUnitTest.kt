package com.example

import com.example.data.CycleUtils
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, (2 + 2).toLong())
  }

  @Test
  fun testCyclePrediction() {
    val lastPeriodStart = "2026-06-01"
    val cycleLength = 28
    val predicted = CycleUtils.getPredictedNextPeriod(lastPeriodStart, cycleLength)
    assertEquals("2026-06-29", predicted)
  }

  @Test
  fun testCycleDayCalculation() {
    val lastPeriodStart = "2026-06-15"
    val testToday = "2026-06-20"
    val cycleDay = CycleUtils.getCurrentCycleDay(lastPeriodStart, testToday)
    assertEquals(6, cycleDay.toLong())
  }

  @Test
  fun testCyclePhaseSelection() {
    val phase1 = CycleUtils.getCyclePhase(cycleDay = 3, cycleLength = 28, periodLength = 5)
    assertEquals("Menstrual Phase", phase1)

    val phase2 = CycleUtils.getCyclePhase(cycleDay = 10, cycleLength = 28, periodLength = 5)
    assertEquals("Follicular Phase", phase2)

    val phase3 = CycleUtils.getCyclePhase(cycleDay = 14, cycleLength = 28, periodLength = 5)
    assertEquals("Ovulatory Phase", phase3)

    val phase4 = CycleUtils.getCyclePhase(cycleDay = 21, cycleLength = 28, periodLength = 5)
    assertEquals("Luteal Phase", phase4)
  }
}
