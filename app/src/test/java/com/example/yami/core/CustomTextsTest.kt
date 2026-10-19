package com.example.yami.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

class CustomTextsTest {

    @Test
    fun testPickBreakNoneEnabled() {
        val list = listOf(
            BreakMessage("1", "Msg 1", enabled = false, builtIn = true),
            BreakMessage("2", "Msg 2", enabled = false, builtIn = false)
        )
        val picked = CustomTexts.pickBreak(list, last = null, random = Random(42))
        assertNull(picked)
    }

    @Test
    fun testPickBreakSingleEnabledEqualToLast() {
        val list = listOf(
            BreakMessage("1", "Only Message", enabled = true, builtIn = true),
            BreakMessage("2", "Disabled Message", enabled = false, builtIn = false)
        )
        val picked = CustomTexts.pickBreak(list, last = "Only Message", random = Random(42))
        assertEquals("Only Message", picked)
    }

    @Test
    fun testPickBreakNeverRepeatsLastWithThreeEnabled() {
        val list = listOf(
            BreakMessage("1", "First", enabled = true, builtIn = true),
            BreakMessage("2", "Second", enabled = true, builtIn = true),
            BreakMessage("3", "Third", enabled = true, builtIn = false)
        )
        val random = Random(12345)
        var last: String? = null
        for (i in 0 until 200) {
            val picked = CustomTexts.pickBreak(list, last = last, random = random)
            assertNotNull(picked)
            if (last != null) {
                assertNotEquals("Should never repeat last pick at iteration $i", last, picked)
            }
            last = picked
        }
    }

    @Test
    fun testFillNumber() {
        val template = "+%d, %d%%"
        val result = CustomTexts.fillNumber(template, 7)
        assertEquals("+7, 7%%", result)
    }

    @Test
    fun testClean() {
        assertNull(CustomTexts.clean(""))
        assertNull(CustomTexts.clean("    "))
        assertNull(CustomTexts.clean(" \t \n "))

        val longText = "a".repeat(100)
        val cleanedLong = CustomTexts.clean(longText)
        assertNotNull(cleanedLong)
        assertEquals(80, cleanedLong?.length)
        assertEquals("a".repeat(80), cleanedLong)

        assertEquals("a b", CustomTexts.clean("  a   b "))
    }
}
