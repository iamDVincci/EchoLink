package com.kinetix.pro.editor.timeline

import com.kinetix.pro.core.model.ClipSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineSplitterTest {

    private val splitter = TimelineSplitter()

    @Test
    fun testSplitIntoEqualPartsCalculatesExactBoundariesWithoutAccumulationError() {
        val totalDurationMs = 60000L // 60 seconds
        val parts = 3

        val clips = splitter.splitIntoEqualParts(totalDurationMs, parts)

        assertEquals(3, clips.size)
        assertEquals(0L, clips[0].startMs)
        assertEquals(20000L, clips[0].endMs)

        assertEquals(20000L, clips[1].startMs)
        assertEquals(40000L, clips[1].endMs)

        assertEquals(40000L, clips[2].startMs)
        assertEquals(60000L, clips[2].endMs)
    }

    @Test
    fun testSplitIntoTenPartsOddDurationCoversFullLength() {
        val totalDurationMs = 45321L
        val parts = 10

        val clips = splitter.splitIntoEqualParts(totalDurationMs, parts)

        assertEquals(10, clips.size)
        assertEquals(0L, clips.first().startMs)
        assertEquals(totalDurationMs, clips.last().endMs)

        // Verify continuity
        for (i in 0 until clips.size - 1) {
            assertEquals(clips[i].endMs, clips[i + 1].startMs)
        }
    }

    @Test
    fun testSplitAtPlayhead() {
        val initialClips = listOf(ClipSection(0L, 30000L, "Clip 1"))
        val splitClips = splitter.splitAtPlayhead(initialClips, 12000L)

        assertEquals(2, splitClips.size)
        assertEquals(0L, splitClips[0].startMs)
        assertEquals(12000L, splitClips[0].endMs)
        assertEquals(12000L, splitClips[1].startMs)
        assertEquals(30000L, splitClips[1].endMs)
    }

    @Test
    fun testTrimSectionEnforcesMinimumDuration() {
        val clip = ClipSection(0L, 20000L, "Clip")
        val trimmed = splitter.trimSection(clip, 5000L, 5100L, minDurationMs = 1000L)

        assertEquals(5000L, trimmed.startMs)
        assertTrue(trimmed.endMs >= 6000L)
    }
}
