package logo

import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range

class LogoDefinitionResolver {

    fun findDefinition(text: String, cursor: Position, uri: String): Location? {
        val lines = text.lines()
        if (cursor.line < 0 || cursor.line >= lines.size) return null

        val wordInfo = findWordAt(lines[cursor.line], cursor.character) ?: return null
        val currentWord = wordInfo.text
        val normalized = normalizeSymbol(currentWord)

        val procedureDeclarations = mutableMapOf<String, Location>()
        val variableDeclarations = mutableMapOf<String, MutableList<ScopedLocation>>()

        var currentProcedure: String? = null

        for ((lineNumber, line) in lines.withIndex()) {
            val words = Regex("""\S+""").findAll(line).toList()
            if (words.isEmpty()) continue

            var expectProcedureName = false
            var onToLine = false
            var expectMakeName = false

            for (match in words) {
                val word = match.value
                val start = match.range.first
                val end = match.range.last + 1
                val lower = word.lowercase()

                when {
                    lower == "to" -> {
                        expectProcedureName = true
                        onToLine = true
                    }

                    expectProcedureName -> {
                        val name = word.lowercase()
                        val location = Location(
                            uri,
                            Range(
                                Position(lineNumber, start),
                                Position(lineNumber, end)
                            )
                        )
                        procedureDeclarations[name] = location
                        currentProcedure = name
                        expectProcedureName = false
                    }

                    lower == "end" -> {
                        currentProcedure = null
                    }

                    onToLine && word.startsWith(":") && word.length > 1 -> {
                        val name = word.substring(1).lowercase()
                        val location = Location(
                            uri,
                            Range(
                                Position(lineNumber, start),
                                Position(lineNumber, end)
                            )
                        )
                        variableDeclarations
                            .getOrPut(name) { mutableListOf() }
                            .add(ScopedLocation(currentProcedure, location))
                    }

                    lower == "make" -> {
                        expectMakeName = true
                    }

                    expectMakeName && word.startsWith("\"") && word.length > 1 -> {
                        val name = word.substring(1).lowercase()
                        val location = Location(
                            uri,
                            Range(
                                Position(lineNumber, start),
                                Position(lineNumber, end)
                            )
                        )
                        variableDeclarations
                            .getOrPut(name) { mutableListOf() }
                            .add(ScopedLocation(currentProcedure, location))
                        expectMakeName = false
                    }

                    else -> {
                        if (word != "to") {
                            expectMakeName = false
                        }
                    }
                }
            }
        }

        return when {
            currentWord.startsWith(":") -> resolveVariable(normalized, variableDeclarations, cursor, lines)
            else -> procedureDeclarations[normalized]
        }
    }

    private fun resolveVariable(
        name: String,
        declarations: Map<String, MutableList<ScopedLocation>>,
        cursor: Position,
        lines: List<String>
    ): Location? {
        val visibleProcedure = findEnclosingProcedure(lines, cursor.line)
        val matches = declarations[name] ?: return null

        val local = matches.lastOrNull { it.scope == visibleProcedure }
        if (local != null) return local.location

        val global = matches.lastOrNull { it.scope == null }
        return global?.location
    }

    private fun findEnclosingProcedure(lines: List<String>, targetLine: Int): String? {
        var currentProcedure: String? = null

        for ((lineNumber, line) in lines.withIndex()) {
            if (lineNumber > targetLine) break

            val words = Regex("""\S+""").findAll(line).map { it.value }.toList()
            if (words.isEmpty()) continue

            if (words[0].lowercase() == "to" && words.size >= 2) {
                currentProcedure = words[1].lowercase()
            } else if (words[0].lowercase() == "end") {
                currentProcedure = null
            }
        }

        return currentProcedure
    }

    private fun findWordAt(line: String, character: Int): WordAtPosition? {
        if (line.isEmpty()) return null

        val safeChar = character.coerceIn(0, line.length)
        val regex = Regex("""[^\s\[\];]+""")

        for (match in regex.findAll(line)) {
            val start = match.range.first
            val endExclusive = match.range.last + 1

            if (safeChar in start..endExclusive || (safeChar > 0 && safeChar - 1 in start until endExclusive)) {
                return WordAtPosition(
                    text = match.value,
                    start = start,
                    endExclusive = endExclusive
                )
            }
        }

        return null
    }

    private fun normalizeSymbol(word: String): String {
        return when {
            word.startsWith(":") && word.length > 1 -> word.substring(1).lowercase()
            word.startsWith("\"") && word.length > 1 -> word.substring(1).lowercase()
            else -> word.lowercase()
        }
    }

    private data class WordAtPosition(
        val text: String,
        val start: Int,
        val endExclusive: Int
    )

    private data class ScopedLocation(
        val scope: String?,
        val location: Location
    )
}