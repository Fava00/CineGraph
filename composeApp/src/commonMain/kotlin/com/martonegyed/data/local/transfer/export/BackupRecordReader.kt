package com.martonegyed.data.local.transfer.export

import com.martonegyed.domain.repository.BackupInput
import kotlinx.serialization.json.Json

internal class BackupRecordReader(private val input: BackupInput, private val checkActive: () -> Unit = {}) {
    private val buffer = ByteArray(32 * 1024)
    private var position = 0
    private var available = 0
    private var next = -2

    private fun peek(): Int {
        if (next == -2) {
            if (position == available) {
                checkActive()
                available = input.read(buffer)
                position = 0
                check(available != 0) { "Backup stream returned no data" }
            }
            next = if (available < 0) -1 else buffer[position++].toInt() and 255
        }
        return next
    }

    private fun take(): Int = peek().also { next = -2 }
    private fun isWhitespace(byte: Int) = byte == 9 || byte == 10 || byte == 13 || byte == 32
    private fun whitespace() { while (isWhitespace(peek())) take() }
    private fun expect(char: Char) {
        whitespace()
        require(take() == char.code) { "Malformed backup: expected '$char'" }
    }

    fun read(selected: Set<String>, onEntry: (String) -> Unit = {}, record: (String, String) -> Unit): Int {
        val arrays = setOf("movies", "people", "logs", "customLists", "listEntries", "ignoredMovies")
        val seen = mutableSetOf<String>()
        var version = 1
        expect('{')
        whitespace()
        if (peek() != '}'.code) while (true) {
            val key = Json.decodeFromString<String>(value(true)!!)
            require(seen.add(key)) { "Duplicate backup field: $key" }
            expect(':')
            when (key) {
                "version" -> version = Json.decodeFromString<Int>(value(true)!!)
                "exportedAt" -> Json.decodeFromString<String>(value(true)!!)
                in arrays -> {
                    expect('[')
                    whitespace()
                    if (peek() != ']'.code) while (true) {
                        val text = value(key in selected)
                        onEntry(key)
                        if (text != null) record(key, text)
                        whitespace()
                        if (peek() != ','.code) break
                        take()
                    }
                    expect(']')
                }
                else -> error("Unknown backup field: $key")
            }
            whitespace()
            if (peek() != ','.code) break
            take()
        }
        expect('}')
        whitespace()
        require(peek() == -1) { "Unexpected content after backup" }
        require(seen.containsAll(arrays + "exportedAt")) { "Backup is missing required fields" }
        require(version in 1..3) { "Unsupported CineGraph backup version: $version" }
        return version
    }

    private fun value(capture: Boolean): String? {
        whitespace()
        var bytes = if (capture) ByteArray(1024) else null
        var size = 0
        var depth = 0
        var quoted = false
        var escaped = false
        while (true) {
            val byte = peek()
            if (!quoted && depth == 0 &&
                (byte < 0 || byte == ','.code || byte == ']'.code || byte == '}'.code ||
                    byte == ':'.code || isWhitespace(byte))) break
            require(byte >= 0) { "Truncated backup record" }
            take()
            if (capture) {
                require(size < 8 * 1024 * 1024) { "A single backup record exceeds the 8 MB limit" }
                if (size == bytes!!.size) bytes = bytes.copyOf(bytes.size * 2)
                bytes[size] = byte.toByte()
            }
            size++
            if (quoted) {
                if (escaped) escaped = false
                else if (byte == '\\'.code) escaped = true
                else if (byte == '"'.code) quoted = false
            } else when (byte) {
                '"'.code -> quoted = true
                '{'.code, '['.code -> depth++
                '}'.code, ']'.code -> depth--
            }
        }
        require(size > 0 && !quoted && depth == 0) { "Invalid or truncated backup value" }
        return bytes?.decodeToString(0, size, throwOnInvalidSequence = true)
    }
}
