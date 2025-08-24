package com.vibestream.player.domain.subtitle

import java.io.InputStream

/**
 * Subtitle entry representing a single subtitle item
 */
data class SubtitleEntry(
    val startTime: Long, // milliseconds
    val endTime: Long,   // milliseconds
    val text: String,
    val styling: SubtitleStyling? = null
)

/**
 * Subtitle styling information
 */
data class SubtitleStyling(
    val fontName: String? = null,
    val fontSize: Float? = null,
    val color: Int? = null,
    val backgroundColor: Int? = null,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val alignment: SubtitleAlignment = SubtitleAlignment.CENTER
)

enum class SubtitleAlignment {
    LEFT, CENTER, RIGHT
}

/**
 * Subtitle format types
 */
enum class SubtitleFormat {
    SRT, ASS, SSA, VTT, SUB, UNKNOWN
}

/**
 * Interface for subtitle parsers
 */
interface SubtitleParser {
    fun canParse(format: SubtitleFormat): Boolean
    fun parse(inputStream: InputStream, encoding: String = "UTF-8"): List<SubtitleEntry>
}

/**
 * SRT subtitle parser implementation
 */
class SrtSubtitleParser : SubtitleParser {
    
    override fun canParse(format: SubtitleFormat): Boolean = format == SubtitleFormat.SRT
    
    override fun parse(inputStream: InputStream, encoding: String): List<SubtitleEntry> {
        val entries = mutableListOf<SubtitleEntry>()
        
        try {
            val content = inputStream.bufferedReader(charset(encoding)).readText()
            val blocks = content.split("\n\n").filter { it.trim().isNotEmpty() }
            
            for (block in blocks) {
                val lines = block.trim().split("\n")
                if (lines.size >= 3) {
                    try {
                        // Skip index line (first line)
                        val timeLine = lines[1]
                        val textLines = lines.drop(2)
                        
                        val (startTime, endTime) = parseTimeRange(timeLine)
                        val text = textLines.joinToString("\n")
                        
                        entries.add(SubtitleEntry(startTime, endTime, text))
                    } catch (e: Exception) {
                        // Skip malformed entries
                        continue
                    }
                }
            }
        } catch (e: Exception) {
            throw SubtitleParseException("Failed to parse SRT subtitle", e)
        }
        
        return entries.sortedBy { it.startTime }
    }
    
    private fun parseTimeRange(timeLine: String): Pair<Long, Long> {
        val parts = timeLine.split(" --> ")
        if (parts.size != 2) throw IllegalArgumentException("Invalid time format")
        
        val startTime = parseTimeString(parts[0].trim())
        val endTime = parseTimeString(parts[1].trim())
        
        return Pair(startTime, endTime)
    }
    
    private fun parseTimeString(timeString: String): Long {
        // Format: HH:MM:SS,mmm or HH:MM:SS.mmm
        val cleanTime = timeString.replace(',', '.')
        val parts = cleanTime.split(":")
        
        if (parts.size != 3) throw IllegalArgumentException("Invalid time format: $timeString")
        
        val hours = parts[0].toLong()
        val minutes = parts[1].toLong()
        val secondsParts = parts[2].split(".")
        val seconds = secondsParts[0].toLong()
        val milliseconds = if (secondsParts.size > 1) {
            secondsParts[1].padEnd(3, '0').take(3).toLong()
        } else 0
        
        return hours * 3600000 + minutes * 60000 + seconds * 1000 + milliseconds
    }
}

/**
 * WebVTT subtitle parser implementation
 */
class VttSubtitleParser : SubtitleParser {
    
    override fun canParse(format: SubtitleFormat): Boolean = format == SubtitleFormat.VTT
    
    override fun parse(inputStream: InputStream, encoding: String): List<SubtitleEntry> {
        val entries = mutableListOf<SubtitleEntry>()
        
        try {
            val content = inputStream.bufferedReader(charset(encoding)).readText()
            val lines = content.split("\n")
            
            var i = 0
            // Skip WEBVTT header
            while (i < lines.size && !lines[i].startsWith("WEBVTT")) i++
            i++ // Skip header line
            
            while (i < lines.size) {
                val line = lines[i].trim()
                
                if (line.isEmpty()) {
                    i++
                    continue
                }
                
                // Check if this line contains timing
                if (line.contains("-->")) {
                    try {
                        val (startTime, endTime) = parseVttTimeRange(line)
                        val textLines = mutableListOf<String>()
                        
                        i++
                        while (i < lines.size && lines[i].trim().isNotEmpty()) {
                            textLines.add(lines[i].trim())
                            i++
                        }
                        
                        if (textLines.isNotEmpty()) {
                            val text = textLines.joinToString("\n")
                            entries.add(SubtitleEntry(startTime, endTime, text))
                        }
                    } catch (e: Exception) {
                        // Skip malformed entries
                        i++
                        continue
                    }
                } else {
                    i++
                }
            }
        } catch (e: Exception) {
            throw SubtitleParseException("Failed to parse VTT subtitle", e)
        }
        
        return entries.sortedBy { it.startTime }
    }
    
    private fun parseVttTimeRange(timeLine: String): Pair<Long, Long> {
        val parts = timeLine.split(" --> ")
        if (parts.size < 2) throw IllegalArgumentException("Invalid VTT time format")
        
        val startTime = parseVttTimeString(parts[0].trim())
        val endTime = parseVttTimeString(parts[1].trim().split(" ")[0]) // Remove any settings
        
        return Pair(startTime, endTime)
    }
    
    private fun parseVttTimeString(timeString: String): Long {
        // Format: MM:SS.mmm or HH:MM:SS.mmm
        val parts = timeString.split(":")
        
        return when (parts.size) {
            2 -> {
                // MM:SS.mmm
                val minutes = parts[0].toLong()
                val secondsParts = parts[1].split(".")
                val seconds = secondsParts[0].toLong()
                val milliseconds = if (secondsParts.size > 1) {
                    secondsParts[1].padEnd(3, '0').take(3).toLong()
                } else 0
                
                minutes * 60000 + seconds * 1000 + milliseconds
            }
            3 -> {
                // HH:MM:SS.mmm
                val hours = parts[0].toLong()
                val minutes = parts[1].toLong()
                val secondsParts = parts[2].split(".")
                val seconds = secondsParts[0].toLong()
                val milliseconds = if (secondsParts.size > 1) {
                    secondsParts[1].padEnd(3, '0').take(3).toLong()
                } else 0
                
                hours * 3600000 + minutes * 60000 + seconds * 1000 + milliseconds
            }
            else -> throw IllegalArgumentException("Invalid VTT time format: $timeString")
        }
    }
}

/**
 * ASS/SSA subtitle parser implementation
 */
class AssSubtitleParser : SubtitleParser {
    
    override fun canParse(format: SubtitleFormat): Boolean = 
        format == SubtitleFormat.ASS || format == SubtitleFormat.SSA
    
    override fun parse(inputStream: InputStream, encoding: String): List<SubtitleEntry> {
        val entries = mutableListOf<SubtitleEntry>()
        
        try {
            val content = inputStream.bufferedReader(charset(encoding)).readText()
            val lines = content.split("\n")
            
            var inEvents = false
            var formatLine: String? = null
            
            for (line in lines) {
                val trimmedLine = line.trim()
                
                when {
                    trimmedLine.startsWith("[Events]") -> {
                        inEvents = true
                        continue
                    }
                    trimmedLine.startsWith("[") -> {
                        inEvents = false
                        continue
                    }
                    inEvents && trimmedLine.startsWith("Format:") -> {
                        formatLine = trimmedLine
                    }
                    inEvents && trimmedLine.startsWith("Dialogue:") -> {
                        formatLine?.let { format ->
                            try {
                                val entry = parseAssDialogue(trimmedLine, format)
                                entry?.let { entries.add(it) }
                            } catch (e: Exception) {
                                // Skip malformed entries
                                continue
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            throw SubtitleParseException("Failed to parse ASS subtitle", e)
        }
        
        return entries.sortedBy { it.startTime }
    }
    
    private fun parseAssDialogue(dialogueLine: String, formatLine: String): SubtitleEntry? {
        val formatFields = formatLine.substringAfter("Format:").split(",").map { it.trim() }
        val dialogueFields = dialogueLine.substringAfter("Dialogue:").split(",", limit = formatFields.size)
        
        if (dialogueFields.size < formatFields.size) return null
        
        val fieldMap = formatFields.zip(dialogueFields).toMap()
        
        val startTime = parseAssTime(fieldMap["Start"] ?: return null)
        val endTime = parseAssTime(fieldMap["End"] ?: return null)
        val text = fieldMap["Text"]?.let { cleanAssText(it) } ?: return null
        
        return SubtitleEntry(startTime, endTime, text)
    }
    
    private fun parseAssTime(timeString: String): Long {
        // Format: H:MM:SS.mm
        val parts = timeString.split(":")
        if (parts.size != 3) throw IllegalArgumentException("Invalid ASS time format")
        
        val hours = parts[0].toLong()
        val minutes = parts[1].toLong()
        val secondsParts = parts[2].split(".")
        val seconds = secondsParts[0].toLong()
        val centiseconds = if (secondsParts.size > 1) {
            secondsParts[1].padEnd(2, '0').take(2).toLong() * 10
        } else 0
        
        return hours * 3600000 + minutes * 60000 + seconds * 1000 + centiseconds
    }
    
    private fun cleanAssText(text: String): String {
        // Remove ASS formatting tags
        return text.replace(Regex("\\{[^}]*\\}"), "")
            .replace("\\N", "\n")
            .replace("\\n", "\n")
            .trim()
    }
}

/**
 * Exception thrown when subtitle parsing fails
 */
class SubtitleParseException(message: String, cause: Throwable? = null) : Exception(message, cause)