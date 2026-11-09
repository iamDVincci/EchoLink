package com.kinetix.pro.downloader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UniversalLinkParserTest {

    private val parser = UniversalLinkParser()

    @Test
    fun testPlatformDetection() {
        assertEquals(SupportedPlatform.YOUTUBE, parser.detectPlatform("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals(SupportedPlatform.YOUTUBE, parser.detectPlatform("https://youtu.be/dQw4w9WgXcQ"))
        assertEquals(SupportedPlatform.TIKTOK, parser.detectPlatform("https://www.tiktok.com/@creator/video/123456789"))
        assertEquals(SupportedPlatform.INSTAGRAM, parser.detectPlatform("https://www.instagram.com/reel/C8xyz123/"))
        assertEquals(SupportedPlatform.X_TWITTER, parser.detectPlatform("https://x.com/AndroidDev/status/123456789"))
        assertEquals(SupportedPlatform.REDDIT, parser.detectPlatform("https://www.reddit.com/r/Android/comments/abc/"))
        assertEquals(SupportedPlatform.VIMEO, parser.detectPlatform("https://vimeo.com/76979871"))
        assertEquals(SupportedPlatform.DIRECT_MEDIA, parser.detectPlatform("https://example.com/audio/podcast.mp3"))
        assertEquals(SupportedPlatform.DIRECT_MEDIA, parser.detectPlatform("https://cdn.site.org/assets/video_1080p.mp4"))
    }

    @Test
    fun testIsDirectFileUrl() {
        assertTrue(parser.isDirectFileUrl("https://example.com/song.mp3"))
        assertTrue(parser.isDirectFileUrl("https://example.com/clip.mp4?token=123"))
        assertTrue(parser.isDirectFileUrl("https://example.com/audio.wav"))
        assertTrue(parser.isDirectFileUrl("https://example.com/sound.flac"))
    }
}
