package logo

class LogoTokenizer {

    private val keywords = setOf(
        "to", "end", "repeat", "if", "make",
        "fd", "forward",
        "bk", "back",
        "rt", "right",
        "lt", "left",
        "print",
        "pu", "penup",
        "pd", "pendown",
        "cs", "clearscreen",
        "home"
    )

    fun tokenize(text: String): List<RawToken> {
        val procedureNames = collectProcedureNames(text)
        val tokens = mutableListOf<RawToken>()
        val lines = text.lines()

        for ((lineNumber, line) in lines.withIndex()) {
            var i = 0
            var expectProcedureName = false
            var onToLine = false
            var expectMakeName = false

            while (i < line.length) {
                val ch = line[i]

                if (ch.isWhitespace()) {
                    i++
                    continue
                }

                if (ch == ';') {
                    tokens += RawToken(
                        line = lineNumber,
                        start = i,
                        length = line.length - i,
                        type = "comment"
                    )
                    break
                }

                if (ch == '[' || ch == ']') {
                    tokens += RawToken(
                        line = lineNumber,
                        start = i,
                        length = 1,
                        type = "keyword"
                    )
                    i++
                    continue
                }

                val start = i
                while (i < line.length &&
                    !line[i].isWhitespace() &&
                    line[i] != ';' &&
                    line[i] != '[' &&
                    line[i] != ']'
                ) {
                    i++
                }

                val word = line.substring(start, i)
                val lower = word.lowercase()

                when {
                    lower == "to" -> {
                        tokens += RawToken(
                            line = lineNumber,
                            start = start,
                            length = word.length,
                            type = "keyword"
                        )
                        expectProcedureName = true
                        onToLine = true
                        expectMakeName = false
                    }

                    expectProcedureName -> {
                        tokens += RawToken(
                            line = lineNumber,
                            start = start,
                            length = word.length,
                            type = "function",
                            declaration = true
                        )
                        expectProcedureName = false
                    }

                    onToLine && word.startsWith(":") && word.length > 1 -> {
                        tokens += RawToken(
                            line = lineNumber,
                            start = start,
                            length = word.length,
                            type = "parameter",
                            declaration = true
                        )
                    }

                    lower == "make" -> {
                        tokens += RawToken(
                            line = lineNumber,
                            start = start,
                            length = word.length,
                            type = "keyword"
                        )
                        expectMakeName = true
                    }

                    expectMakeName && word.startsWith("\"") && word.length > 1 -> {
                        tokens += RawToken(
                            line = lineNumber,
                            start = start,
                            length = word.length,
                            type = "variable",
                            declaration = true
                        )
                        expectMakeName = false
                    }

                    lower in keywords -> {
                        tokens += RawToken(
                            line = lineNumber,
                            start = start,
                            length = word.length,
                            type = "keyword"
                        )
                        expectMakeName = false
                    }

                    word.startsWith(":") && word.length > 1 -> {
                        tokens += RawToken(
                            line = lineNumber,
                            start = start,
                            length = word.length,
                            type = "variable"
                        )
                        expectMakeName = false
                    }

                    word.matches(NUMBER_REGEX) -> {
                        tokens += RawToken(
                            line = lineNumber,
                            start = start,
                            length = word.length,
                            type = "number"
                        )
                        expectMakeName = false
                    }

                    lower in procedureNames -> {
                        tokens += RawToken(
                            line = lineNumber,
                            start = start,
                            length = word.length,
                            type = "function"
                        )
                        expectMakeName = false
                    }

                    else -> {
                        expectMakeName = false
                    }
                }
            }
        }

        return tokens
    }

    private fun collectProcedureNames(text: String): Set<String> {
        val names = mutableSetOf<String>()

        for (line in text.lines()) {
            val words = Regex("""\S+""").findAll(line).map { it.value }.toList()
            if (words.isEmpty()) continue

            if (words[0].lowercase() == "to" && words.size >= 2) {
                names += words[1].lowercase()
            }
        }

        return names
    }

    companion object {
        private val NUMBER_REGEX = Regex("""-?\d+(\.\d+)?""")
    }
}