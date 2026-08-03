package pro.masterdoc.client.ui.screens

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import pro.fixaverse.design.theme.FixaverseLiteTokens

class ReportChartFormattingTest {
    @Test
    fun formatsWholeChartValuesWithoutDecimal() {
        assertEquals("10", formatChartValue(10f))
        assertEquals("0", formatChartValue(0f))
    }

    @Test
    fun roundsChartValuesToOneDecimalWithRussianComma() {
        assertEquals("4,6", formatChartValue(4.567f))
        assertEquals("18,5", formatChartValue(18.5f))
    }

    @Test
    fun detectsNonZeroChartSeries() {
        assertTrue(hasNonZeroChartSeries(listOf(ReportChartPoint("A", 0f), ReportChartPoint("B", 2f))))
        assertFalse(hasNonZeroChartSeries(listOf(ReportChartPoint("A", 0f), ReportChartPoint("B", 0f))))
        assertFalse(hasNonZeroChartSeries(emptyList()))
    }

    @Test
    fun chartMaxValueUsesLargestSampleOrOne() {
        assertEquals(18.5f, chartMaxValue(listOf(ReportChartPoint("A", 4f), ReportChartPoint("B", 18.5f))))
        assertEquals(1f, chartMaxValue(listOf(ReportChartPoint("A", 0f))))
        assertEquals(1f, chartMaxValue(emptyList()))
    }

    @Test
    fun reportChartBarColorCyclesBrandPalette() {
        assertEquals(ReportChartSeriesColors[0], reportChartBarColor(0))
        assertEquals(FixaverseLiteTokens.Flare, reportChartBarColor(1))
        assertEquals(FixaverseLiteTokens.Forest, reportChartBarColor(3))
        assertEquals(ReportChartSeriesColors[0], reportChartBarColor(ReportChartSeriesColors.size))
    }

    @Test
    fun reportChartBarHighlightIsBrighterThanBase() {
        val base = Color(0xFF1A6FFF)
        val highlight = reportChartBarHighlight(base)
        assertTrue(highlight.red >= base.red)
        assertTrue(highlight.green >= base.green)
        assertTrue(highlight.blue >= base.blue)
    }
}
