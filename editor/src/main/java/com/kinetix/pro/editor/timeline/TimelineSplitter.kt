package com.kinetix.pro.editor.timeline

import com.kinetix.pro.core.model.ClipSection
import com.kinetix.pro.core.model.TailRemainderRule

class TimelineSplitter {

    /**
     * Splits video into N equal parts.
     * Computes boundary as (i * totalDuration / count) to avoid float error accumulation.
     */
    fun splitIntoEqualParts(
        totalDurationMs: Long,
        numberOfParts: Int,
        mergeRemainderIntoLast: Boolean = true
    ): List<ClipSection> {
        val count = numberOfParts.coerceIn(1, 60)
        if (count == 1 || totalDurationMs <= 0L) {
            return listOf(ClipSection(0L, totalDurationMs, "Part 1"))
        }

        val result = mutableListOf<ClipSection>()
        for (i in 0 until count) {
            val startMs = (i.toLong() * totalDurationMs) / count
            val endMs = if (i == count - 1) {
                totalDurationMs
            } else {
                ((i + 1).toLong() * totalDurationMs) / count
            }
            result.add(
                ClipSection(
                    startMs = startMs,
                    endMs = endMs,
                    label = "Part ${String.format("%02d", i + 1)}"
                )
            )
        }
        return result
    }

    /**
     * Splits video into parts where each part is approximately targetSeconds long
     */
    fun splitByDuration(
        totalDurationMs: Long,
        targetPartSeconds: Int,
        mergeRemainderIntoLast: Boolean = true
    ): List<ClipSection> {
        val rule = if (mergeRemainderIntoLast) TailRemainderRule.MERGE_INTO_LAST else TailRemainderRule.SEPARATE_CLIP
        return splitByDurationWithTailRule(totalDurationMs, targetPartSeconds, rule)
    }

    /**
     * Automated Batch Split with Tail Remainder Rule (PRD Section 2.2)
     */
    fun splitByDurationWithTailRule(
        totalDurationMs: Long,
        targetPartSeconds: Int,
        tailRule: TailRemainderRule = TailRemainderRule.SEPARATE_CLIP
    ): List<ClipSection> {
        val partMs = (targetPartSeconds.coerceAtLeast(5) * 1000).toLong()
        if (totalDurationMs <= partMs) {
            return listOf(ClipSection(0L, totalDurationMs, "Part 1"))
        }

        val partsCount = (totalDurationMs / partMs).toInt()
        val remainderMs = totalDurationMs % partMs

        val result = mutableListOf<ClipSection>()
        for (i in 0 until partsCount) {
            val startMs = i * partMs
            val endMs = if (i == partsCount - 1 && tailRule == TailRemainderRule.MERGE_INTO_LAST) {
                totalDurationMs
            } else {
                (i + 1) * partMs
            }
            result.add(
                ClipSection(
                    startMs = startMs,
                    endMs = endMs,
                    label = "Part ${String.format("%02d", i + 1)}"
                )
            )
        }

        if (tailRule == TailRemainderRule.SEPARATE_CLIP && remainderMs > 500L) {
            result.add(
                ClipSection(
                    startMs = partsCount * partMs,
                    endMs = totalDurationMs,
                    label = "Part ${String.format("%02d", partsCount + 1)}"
                )
            )
        }

        return result
    }

    /**
     * Splits an existing clip into two clips at playheadMs.
     */
    fun splitAtPlayhead(
        clips: List<ClipSection>,
        playheadMs: Long
    ): List<ClipSection> {
        val targetIndex = clips.indexOfFirst { playheadMs > it.startMs && playheadMs < it.endMs }
        if (targetIndex == -1) return clips

        val target = clips[targetIndex]
        val clipA = target.copy(startMs = target.startMs, endMs = playheadMs, label = "${target.label}a")
        val clipB = target.copy(startMs = playheadMs, endMs = target.endMs, label = "${target.label}b")

        val newList = clips.toMutableList()
        newList.removeAt(targetIndex)
        newList.add(targetIndex, clipB)
        newList.add(targetIndex, clipA)
        return newList
    }

    /**
     * Inserts a 3.0s Freeze Frame at playheadMs (PRD Section 2.3)
     */
    fun insertFreezeFrame(
        clips: List<ClipSection>,
        playheadMs: Long,
        freezeDurationMs: Long = 3000L
    ): List<ClipSection> {
        val targetIndex = clips.indexOfFirst { playheadMs >= it.startMs && playheadMs <= it.endMs }
        if (targetIndex == -1) return clips

        val target = clips[targetIndex]
        val freezeClip = ClipSection(
            startMs = playheadMs,
            endMs = (playheadMs + freezeDurationMs),
            label = "Freeze",
            isFreezeFrame = true,
            speed = 0.001f
        )

        val newList = clips.toMutableList()
        if (playheadMs <= target.startMs + 200L) {
            newList.add(targetIndex, freezeClip)
        } else if (playheadMs >= target.endMs - 200L) {
            newList.add(targetIndex + 1, freezeClip)
        } else {
            val clipA = target.copy(startMs = target.startMs, endMs = playheadMs, label = "${target.label}a")
            val clipB = target.copy(startMs = playheadMs, endMs = target.endMs, label = "${target.label}b")
            newList.removeAt(targetIndex)
            newList.add(targetIndex, clipB)
            newList.add(targetIndex, freezeClip)
            newList.add(targetIndex, clipA)
        }
        return newList
    }

    /**
     * Duplicates a clip on the timeline (PRD Section 2.1)
     */
    fun duplicateClip(
        clips: List<ClipSection>,
        clipIndex: Int
    ): List<ClipSection> {
        if (clipIndex !in clips.indices) return clips
        val target = clips[clipIndex]
        val clone = target.copy(label = "${target.label} Copy")
        val newList = clips.toMutableList()
        newList.add(clipIndex + 1, clone)
        return newList
    }

    /**
     * Trims a single clip section with in/out boundaries
     */
    fun trimSection(
        clip: ClipSection,
        newStartMs: Long,
        newEndMs: Long,
        minDurationMs: Long = 500L
    ): ClipSection {
        val validStart = newStartMs.coerceAtLeast(0L)
        val validEnd = newEndMs.coerceAtLeast(validStart + minDurationMs)
        return clip.copy(startMs = validStart, endMs = validEnd)
    }

    /**
     * Reverses the playback order of clips on the timeline
     */
    fun reverseTimelineClips(clips: List<ClipSection>): List<ClipSection> {
        if (clips.size <= 1) {
            val single = clips.firstOrNull() ?: return clips
            return listOf(single.copy(isReversed = !single.isReversed))
        }
        return clips.reversed().mapIndexed { idx, clip ->
            clip.copy(label = "Part ${String.format("%02d", idx + 1)}", isReversed = !clip.isReversed)
        }
    }

    /**
     * Toggles reverse state on an active clip
     */
    fun toggleReverseClip(clips: List<ClipSection>, clipIndex: Int): List<ClipSection> {
        if (clipIndex !in clips.indices) return clips
        val list = clips.toMutableList()
        val cur = list[clipIndex]
        list[clipIndex] = cur.copy(isReversed = !cur.isReversed)
        return list
    }
}
