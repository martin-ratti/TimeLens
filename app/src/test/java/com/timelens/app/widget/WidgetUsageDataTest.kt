package com.timelens.app.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetUsageDataTest {

    private fun data(totalHours: Double, goal: Int = 6) = WidgetUsageData(
        totalMs = (totalHours * 3_600_000L).toLong(),
        goalHours = goal
    )

    @Test
    fun `status is OK below 80 percent of the limit`() {
        assertEquals(WidgetStatus.OK, data(3.0).status)
        assertEquals(WidgetStatus.OK, data(4.7).status)
    }

    @Test
    fun `status is NEAR_LIMIT between 80 and 99 percent`() {
        assertEquals(WidgetStatus.NEAR_LIMIT, data(4.8).status)
        assertEquals(WidgetStatus.NEAR_LIMIT, data(5.9).status)
    }

    @Test
    fun `status is EXCEEDED when reaching or surpassing the limit`() {
        assertEquals(WidgetStatus.EXCEEDED, data(6.0).status)
        assertEquals(WidgetStatus.EXCEEDED, data(8.5).status)
    }

    @Test
    fun `ratio is zero when goal is zero`() {
        assertEquals(0f, data(2.0, goal = 0).ratio, 0.0001f)
    }

    @Test
    fun `goalMs converts hours to milliseconds`() {
        assertEquals(4 * 3_600_000L, data(1.0, goal = 4).goalMs)
    }
}
