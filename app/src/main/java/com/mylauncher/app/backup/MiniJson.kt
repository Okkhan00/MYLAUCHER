package com.mylauncher.app.backup

/**
 * A tiny strict JSON reader/writer so backups need no extra dependency and never rely on
 * Android's org.json (which cannot be unit-tested on the JVM). Reader output is Map / List /
 * String / Double / Boolean / null. Nesting depth and input size are limited so a hostile or
 * corrupted file cannot exhaust memory or the stack.
 */
object MiniJson {
    class JsonException(message: String) : Exception(message)

    const val MAX_DEPTH = 8

    fun write(value: Any?): String = StringBuilder().also { write(it, value) }.toString()

    private fun write(sb: StringBuilder, value: Any?) {
        when (value) {
            null -> sb.append("null")
            is Boolean -> sb.append(value)
            is Int, is Long -> sb.append(value.toString())
            is Number -> sb.append(value.toString())
            is String -> writeString(sb, value)
            is Map<*, *> -> {
                sb.append('{')
                var first = true
                for ((k, v) in value) {
                    if (!first) sb.append(',')
                    first = false
                    writeString(sb, k.toString())
                    sb.append(':')
                    write(sb, v)
                }
                sb.append('}')
            }
            is List<*> -> {
                sb.append('[')
                value.forEachIndexed { i, v ->
                    if (i > 0) sb.append(',')
                    write(sb, v)
                }
                sb.append(']')
            }
            else -> throw JsonException("Unsupported type ${value::class.simpleName}")
        }
    }

    private fun writeString(sb: StringBuilder, s: String) {
        sb.append('"')
        for (c in s) {
            when {
                c == '"' -> sb.append("\\\"")
                c == '\\' -> sb.append("\\\\")
                c == '\n' -> sb.append("\\n")
                c == '\r' -> sb.append("\\r")
                c == '\t' -> sb.append("\\t")
                c < ' ' -> sb.append("\\u%04x".format(c.code))
                else -> sb.append(c)
            }
        }
        sb.append('"')
    }

    /** Throws [JsonException] on any malformed input, including trailing garbage. */
    fun parse(text: String): Any? {
        val p = Parser(text)
        p.skipWs()
        val v = p.value(0)
        p.skipWs()
        if (!p.atEnd()) throw JsonException("Unexpected trailing data")
        return v
    }

    private class Parser(private val s: String) {
        private var i = 0

        fun atEnd() = i >= s.length

        fun skipWs() {
            while (i < s.length && (s[i] == ' ' || s[i] == '\n' || s[i] == '\r' || s[i] == '\t')) i++
        }

        private fun peek(): Char = if (i < s.length) s[i] else throw JsonException("Unexpected end")

        private fun expect(c: Char) {
            if (peek() != c) throw JsonException("Expected '$c' at $i")
            i++
        }

        fun value(depth: Int): Any? {
            if (depth > MAX_DEPTH) throw JsonException("Too deeply nested")
            skipWs()
            return when (val c = peek()) {
                '{' -> obj(depth)
                '[' -> arr(depth)
                '"' -> str()
                't' -> lit("true", true)
                'f' -> lit("false", false)
                'n' -> lit("null", null)
                else -> if (c == '-' || c in '0'..'9') num() else throw JsonException("Unexpected '$c' at $i")
            }
        }

        private fun lit(word: String, v: Any?): Any? {
            if (!s.startsWith(word, i)) throw JsonException("Bad literal at $i")
            i += word.length
            return v
        }

        private fun num(): Double {
            val start = i
            if (peek() == '-') i++
            while (i < s.length && (s[i] in '0'..'9' || s[i] == '.' || s[i] == 'e' || s[i] == 'E' || s[i] == '+' || s[i] == '-')) i++
            return s.substring(start, i).toDoubleOrNull()?.takeIf { it.isFinite() } ?: throw JsonException("Bad number at $start")
        }

        private fun str(): String {
            expect('"')
            val sb = StringBuilder()
            while (true) {
                if (i >= s.length) throw JsonException("Unterminated string")
                val c = s[i++]
                when {
                    c == '"' -> return sb.toString()
                    c == '\\' -> {
                        if (i >= s.length) throw JsonException("Bad escape")
                        when (val e = s[i++]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'u' -> {
                                if (i + 4 > s.length) throw JsonException("Bad unicode escape")
                                val code = s.substring(i, i + 4).toIntOrNull(16) ?: throw JsonException("Bad unicode escape")
                                sb.append(code.toChar())
                                i += 4
                            }
                            else -> throw JsonException("Bad escape '\\$e'")
                        }
                    }
                    c < ' ' -> throw JsonException("Control character in string")
                    else -> sb.append(c)
                }
            }
        }

        private fun arr(depth: Int): List<Any?> {
            expect('[')
            val out = ArrayList<Any?>()
            skipWs()
            if (peek() == ']') { i++; return out }
            while (true) {
                out.add(value(depth + 1))
                skipWs()
                when (peek()) {
                    ',' -> i++
                    ']' -> { i++; return out }
                    else -> throw JsonException("Expected ',' or ']' at $i")
                }
            }
        }

        private fun obj(depth: Int): Map<String, Any?> {
            expect('{')
            val out = LinkedHashMap<String, Any?>()
            skipWs()
            if (peek() == '}') { i++; return out }
            while (true) {
                skipWs()
                val key = str()
                skipWs()
                expect(':')
                out[key] = value(depth + 1)
                skipWs()
                when (peek()) {
                    ',' -> i++
                    '}' -> { i++; return out }
                    else -> throw JsonException("Expected ',' or '}' at $i")
                }
            }
        }
    }
}
