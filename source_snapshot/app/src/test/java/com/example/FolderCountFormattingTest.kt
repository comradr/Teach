package com.example

import org.junit.Assert.assertEquals
import org.junit.Test

class FolderCountFormattingTest {
    @Test
    fun russianPluralFormsAreCorrect() {
        assertEquals("1 план", formatPlanCount(1))
        assertEquals("2 плана", formatPlanCount(2))
        assertEquals("5 планов", formatPlanCount(5))
        assertEquals("11 планов", formatPlanCount(11))
        assertEquals("21 план", formatPlanCount(21))
    }
}
