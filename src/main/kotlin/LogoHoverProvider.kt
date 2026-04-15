package logo

import org.eclipse.lsp4j.Hover
import org.eclipse.lsp4j.MarkupContent
import org.eclipse.lsp4j.Position

class LogoHoverProvider {

    private val builtinDocs = mapOf(
        "to" to "Starts a procedure declaration.",
        "end" to "Ends a procedure declaration.",
        "repeat" to "Repeats a block of commands a given number of times.",
        "if" to "Conditionally executes commands.",
        "make" to "Assigns a value to a variable name.",
        "print" to "Prints a value.",
        "fd" to "Moves the turtle forward.",
        "forward" to "Moves the turtle forward.",
        "bk" to "Moves the turtle backward.",
        "back" to "Moves the turtle backward.",
        "rt" to "Turns the turtle right.",
        "right" to "Turns the turtle right.",
        "lt" to "Turns the turtle left.",
        "left" to "Turns the turtle left.",
        "pu" to "Lifts the pen up.",
        "penup" to "Lifts the pen up.",
        "pd" to "Puts the pen down.",
        "pendown" to "Puts the pen down.",
        "cs" to "Clears the screen.",
        "clearscreen" to "Clears the screen.",
        "home" to "Moves the turtle to the home position."
    )

    fun hover(text: String, position: Position): Hover? {
        val lines = text.lines()
        if (position.line < 0 || position.line >= lines.size) return null

        val line = lines[position.line]
        val wordInfo = findWordAt(line, position.character) ?: return null
        val word = wordInfo.text
        val lower = word.lowercase()

        if (word.startsWith(":") && word.length > 1) {
            val name = word.substring(1)
            return markdownHover("Variable `$name`")
        }

        if (word.startsWith("\"") && word.length > 1) {
            val name = word.substring(1)
            return markdownHover("Variable name declaration `$name`")
        }

        builtinDocs[lower]?.let { doc ->
            return markdownHover("`$word`\n\n$doc")
        }

        val procedures = collectProcedureNames(text)
        if (lower in procedures) {
            return markdownHover("Procedure `$word`")
        }

        return null
    }

    private fun markdownHover(value: String): Hover {
        return Hover(
            MarkupContent("markdown", value)
        )
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

    private data class WordAtPosition(
        val text: String,
        val start: Int,
        val endExclusive: Int
    )
}