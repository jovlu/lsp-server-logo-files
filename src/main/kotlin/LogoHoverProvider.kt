package logo

import org.eclipse.lsp4j.Hover
import org.eclipse.lsp4j.MarkupContent
import org.eclipse.lsp4j.Position

class LogoHoverProvider {

    private val definitionResolver = LogoDefinitionResolver()

    fun hover(text: String, position: Position): Hover? {
        val lines = text.lines()
        if (position.line < 0 || position.line >= lines.size) return null

        val line = lines[position.line]
        val lexemeAt = LogoLanguage.findLexemeAt(line, position.character) ?: return null
        val word = lexemeAt.lexeme.text
        val lower = word.lowercase()

        if (word.startsWith(":") && word.length > 1) {
            val name = word.substring(1)
            return markdownHover("Variable `$name`")
        }

        if (word.startsWith("\"") && word.length > 1) {
            val name = word.substring(1)
            return when (LogoLanguage.quotedWordRole(lexemeAt.lexemes, lexemeAt.index)) {
                QuotedWordRole.VARIABLE_DECLARATION -> markdownHover("Variable declaration `$name`")
                QuotedWordRole.VARIABLE_REFERENCE -> markdownHover("Variable name `$name`")
                QuotedWordRole.PROCEDURE_DECLARATION -> markdownHover("Procedure declaration `$name`")
                QuotedWordRole.PROCEDURE_REFERENCE -> markdownHover("Procedure name `$name`")
                QuotedWordRole.PLAIN_STRING -> markdownHover("Quoted word `$name`")
            }
        }

        LogoLanguage.builtinDocs[lower]?.let { doc ->
            return markdownHover("`$word`\n\n$doc")
        }

        if (definitionResolver.findDefinition(text, position, "") != null) {
            return markdownHover("Procedure `$word`")
        }

        return null
    }

    private fun markdownHover(value: String): Hover {
        return Hover(
            MarkupContent("markdown", value)
        )
    }
}
