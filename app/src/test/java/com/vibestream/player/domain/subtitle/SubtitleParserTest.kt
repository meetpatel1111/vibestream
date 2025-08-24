package com.vibestream.player.domain.subtitle

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream

class SubtitleParserTest {

    private lateinit var subtitleParser: SubtitleParser

    @Before
    fun setUp() {
        subtitleParser = SubtitleParser()
    }

    @Test
    fun `parseSubtitle should detect SRT format correctly`() {
        val srtContent = """
            1
            00:00:01,000 --> 00:00:03,000
            Hello World
            
            2
            00:00:04,000 --> 00:00:06,000
            This is a test
        """.trimIndent()

        val inputStream = ByteArrayInputStream(srtContent.toByteArray())
        val result = subtitleParser.parseSubtitle(inputStream, "test.srt")

        assertTrue("Should successfully parse SRT", result.isSuccess)
        val entries = result.getOrNull()
        assertNotNull("Entries should not be null", entries)
        assertEquals("Should have 2 entries", 2, entries?.size)
        
        entries?.let {
            assertEquals("First entry text", "Hello World", it[0].text)
            assertEquals("First entry start time", 1000L, it[0].startTime)
            assertEquals("First entry end time", 3000L, it[0].endTime)
            
            assertEquals("Second entry text", "This is a test", it[1].text)
            assertEquals("Second entry start time", 4000L, it[1].startTime)
            assertEquals("Second entry end time", 6000L, it[1].endTime)
        }
    }

    @Test
    fun `parseSubtitle should detect VTT format correctly`() {
        val vttContent = """
            WEBVTT
            
            00:00:01.000 --> 00:00:03.000
            Hello VTT World
            
            00:00:04.000 --> 00:00:06.000
            This is a VTT test
        """.trimIndent()

        val inputStream = ByteArrayInputStream(vttContent.toByteArray())
        val result = subtitleParser.parseSubtitle(inputStream, "test.vtt")

        assertTrue("Should successfully parse VTT", result.isSuccess)
        val entries = result.getOrNull()
        assertNotNull("Entries should not be null", entries)
        assertEquals("Should have 2 entries", 2, entries?.size)
        
        entries?.let {
            assertEquals("First entry text", "Hello VTT World", it[0].text)
            assertEquals("First entry start time", 1000L, it[0].startTime)
            assertEquals("First entry end time", 3000L, it[0].endTime)
        }
    }

    @Test
    fun `parseSubtitle should detect ASS format correctly`() {
        val assContent = """
            [Script Info]
            Title: Test ASS
            
            [V4+ Styles]
            Format: Name, Fontname, Fontsize
            Style: Default,Arial,20
            
            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Hello ASS World
            Dialogue: 0,0:00:04.00,0:00:06.00,Default,,0,0,0,,This is an ASS test
        """.trimIndent()

        val inputStream = ByteArrayInputStream(assContent.toByteArray())
        val result = subtitleParser.parseSubtitle(inputStream, "test.ass")

        assertTrue("Should successfully parse ASS", result.isSuccess)
        val entries = result.getOrNull()
        assertNotNull("Entries should not be null", entries)
        assertEquals("Should have 2 entries", 2, entries?.size)
        
        entries?.let {
            assertEquals("First entry text", "Hello ASS World", it[0].text)
            assertEquals("First entry start time", 1000L, it[0].startTime)
            assertEquals("First entry end time", 3000L, it[0].endTime)
        }
    }

    @Test
    fun `parseSubtitle should handle malformed SRT gracefully`() {
        val malformedSrt = """
            1
            Invalid timestamp
            Hello World
            
            2
            00:00:04,000 --> 00:00:06,000
            This is valid
        """.trimIndent()

        val inputStream = ByteArrayInputStream(malformedSrt.toByteArray())
        val result = subtitleParser.parseSubtitle(inputStream, "test.srt")

        assertTrue("Should handle malformed SRT gracefully", result.isSuccess)
        val entries = result.getOrNull()
        assertNotNull("Entries should not be null", entries)
        assertEquals("Should only parse valid entries", 1, entries?.size)
        assertEquals("Valid entry text", "This is valid", entries?.get(0)?.text)
    }

    @Test
    fun `parseSubtitle should handle empty content`() {
        val emptyContent = ""
        val inputStream = ByteArrayInputStream(emptyContent.toByteArray())
        val result = subtitleParser.parseSubtitle(inputStream, "test.srt")

        assertTrue("Should handle empty content", result.isSuccess)
        val entries = result.getOrNull()
        assertNotNull("Entries should not be null", entries)
        assertTrue("Should have no entries", entries?.isEmpty() == true)
    }

    @Test
    fun `parseSubtitle should handle unsupported format`() {
        val unsupportedContent = "This is not a subtitle file"
        val inputStream = ByteArrayInputStream(unsupportedContent.toByteArray())
        val result = subtitleParser.parseSubtitle(inputStream, "test.txt")

        assertTrue("Should fail for unsupported format", result.isFailure)
    }

    @Test
    fun `parseSrtTime should parse standard SRT timestamp correctly`() {
        val timestamp = "00:01:23,456"
        val expectedTime = (1 * 60 + 23) * 1000 + 456 // 83456ms
        
        val result = subtitleParser.parseSrtTime(timestamp)
        assertEquals("Should parse SRT timestamp correctly", expectedTime, result)
    }

    @Test
    fun `parseVttTime should parse VTT timestamp correctly`() {
        val timestamp = "00:01:23.456"
        val expectedTime = (1 * 60 + 23) * 1000 + 456 // 83456ms
        
        val result = subtitleParser.parseVttTime(timestamp)
        assertEquals("Should parse VTT timestamp correctly", expectedTime, result)
    }

    @Test
    fun `parseAssTime should parse ASS timestamp correctly`() {
        val timestamp = "0:01:23.46" // ASS uses centiseconds
        val expectedTime = (1 * 60 + 23) * 1000 + 460 // 83460ms
        
        val result = subtitleParser.parseAssTime(timestamp)
        assertEquals("Should parse ASS timestamp correctly", expectedTime, result)
    }

    @Test
    fun `parseSrtTime should handle malformed timestamp`() {
        val invalidTimestamp = "invalid:timestamp"
        val result = subtitleParser.parseSrtTime(invalidTimestamp)
        assertEquals("Should return 0 for invalid timestamp", 0L, result)
    }

    @Test
    fun `stripAssFormatting should remove ASS tags correctly`() {
        val formattedText = "{\\b1}Bold text{\\b0} and {\\i1}italic text{\\i0}"
        val expectedText = "Bold text and italic text"
        
        val result = subtitleParser.stripAssFormatting(formattedText)
        assertEquals("Should strip ASS formatting tags", expectedText, result)
    }

    @Test
    fun `stripAssFormatting should handle text without formatting`() {
        val plainText = "This is plain text"
        val result = subtitleParser.stripAssFormatting(plainText)
        assertEquals("Should return text unchanged", plainText, result)
    }

    @Test
    fun `isValidSubtitleFormat should detect supported formats`() {
        assertTrue("Should detect SRT", subtitleParser.isValidSubtitleFormat("test.srt"))
        assertTrue("Should detect VTT", subtitleParser.isValidSubtitleFormat("test.vtt"))
        assertTrue("Should detect ASS", subtitleParser.isValidSubtitleFormat("test.ass"))
        assertTrue("Should detect SSA", subtitleParser.isValidSubtitleFormat("test.ssa"))
        
        assertFalse("Should reject unsupported format", subtitleParser.isValidSubtitleFormat("test.txt"))
        assertFalse("Should reject no extension", subtitleParser.isValidSubtitleFormat("test"))
    }

    @Test
    fun `parseSubtitle should handle complex SRT with HTML tags`() {
        val srtWithHtml = """
            1
            00:00:01,000 --> 00:00:03,000
            <i>Italic text</i> and <b>bold text</b>
            
            2
            00:00:04,000 --> 00:00:06,000
            <font color="#FF0000">Red text</font>
        """.trimIndent()

        val inputStream = ByteArrayInputStream(srtWithHtml.toByteArray())
        val result = subtitleParser.parseSubtitle(inputStream, "test.srt")

        assertTrue("Should parse SRT with HTML tags", result.isSuccess)
        val entries = result.getOrNull()
        assertEquals("Should have 2 entries", 2, entries?.size)
        
        entries?.let {
            assertEquals("First entry text", "<i>Italic text</i> and <b>bold text</b>", it[0].text)
            assertEquals("Second entry text", "<font color=\"#FF0000\">Red text</font>", it[1].text)
        }
    }

    @Test
    fun `parseSubtitle should handle multiline subtitle entries`() {
        val multilineSrt = """
            1
            00:00:01,000 --> 00:00:03,000
            First line
            Second line
            Third line
            
            2
            00:00:04,000 --> 00:00:06,000
            Single line
        """.trimIndent()

        val inputStream = ByteArrayInputStream(multilineSrt.toByteArray())
        val result = subtitleParser.parseSubtitle(inputStream, "test.srt")

        assertTrue("Should parse multiline entries", result.isSuccess)
        val entries = result.getOrNull()
        assertEquals("Should have 2 entries", 2, entries?.size)
        
        entries?.let {
            assertEquals("Multiline text should be joined", "First line\nSecond line\nThird line", it[0].text)
            assertEquals("Single line text", "Single line", it[1].text)
        }
    }
}