package logo

import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range

class LogoDefinitionResolver {

    fun findDefinition(text: String, cursor: Position, uri: String): Location? {
        val lines = text.lines()
        if (cursor.line !in lines.indices) return null

        val lexemeAt = LogoLanguage.findLexemeAt(lines[cursor.line], cursor.character) ?: return null
        val symbol = classifySymbol(lexemeAt) ?: return null
        val definitions = collectDefinitions(text, uri)

        return when (symbol.kind) {
            SymbolKind.PROCEDURE -> resolveProcedure(symbol.name, definitions.procedures, cursor)
            SymbolKind.VARIABLE -> resolveVariable(
                symbol.name,
                definitions.variables,
                definitions.procedureAt(cursor.line),
                cursor
            )
        }
    }

    private fun classifySymbol(lexemeAt: LogoLexemeAt): Symbol? {
        val word = lexemeAt.lexeme.text
        val name = LogoLanguage.normalizeSymbol(word)

        return when {
            isVariableWord(word) -> Symbol(name, SymbolKind.VARIABLE)
            isQuotedWord(word) -> when (LogoLanguage.quotedWordRole(lexemeAt.lexemes, lexemeAt.index)) {
                QuotedWordRole.VARIABLE_DECLARATION,
                QuotedWordRole.VARIABLE_REFERENCE -> Symbol(name, SymbolKind.VARIABLE)
                QuotedWordRole.PROCEDURE_DECLARATION,
                QuotedWordRole.PROCEDURE_REFERENCE -> Symbol(name, SymbolKind.PROCEDURE)
                QuotedWordRole.PLAIN_STRING -> null
            }
            else -> Symbol(name, SymbolKind.PROCEDURE)
        }
    }

    private fun collectDefinitions(text: String, uri: String): DefinitionMaps {
        val lines = text.lines()
        val procedures = mutableMapOf<String, MutableList<Location>>()
        val variables = mutableMapOf<String, MutableList<VariableDefinition>>()
        val procedureScopes = mutableListOf<ProcedureScope>()

        var currentProcedure: ProcedureScope? = null

        for ((lineNumber, line) in lines.withIndex()) {
            val lexemes = LogoLanguage.scanLine(line).lexemes
            if (lexemes.isEmpty()) continue

            var expectProcedureName = false
            var onToLine = false
            var expectQuotedProcedureName = false
            var expectQuotedVariableName = false
            var quotedVariableScope: String? = null
            var collectLocalDeclarations = false
            var expectLoopControlList = false
            var expectLoopVariable = false
            var expectNameValue = false
            var expectNameVariable = false

            for (lexeme in lexemes) {
                val word = lexeme.text
                val lower = word.lowercase()

                if (collectLocalDeclarations && !isQuotedWord(word)) {
                    collectLocalDeclarations = false
                }

                when {
                    expectNameValue -> {
                        expectNameValue = false
                        expectNameVariable = true
                    }

                    lower == "to" -> {
                        expectProcedureName = true
                        onToLine = true
                    }

                    expectProcedureName && LogoLanguage.isIdentifier(word) -> {
                        val procedureName = lower
                        addProcedure(procedures, procedureName, uri, lineNumber, lexeme)
                        currentProcedure = ProcedureScope(procedureName, lineNumber, lines.lastIndex)
                        procedureScopes += currentProcedure
                        expectProcedureName = false
                    }

                    lower == "end" -> {
                        currentProcedure?.endLine = lineNumber
                        currentProcedure = null
                    }

                    onToLine && isVariableWord(word) -> {
                        addVariable(
                            variables,
                            word.substring(1),
                            currentProcedure?.name,
                            uri,
                            lineNumber,
                            lexeme
                        )
                    }

                    lower in LogoLanguage.procedureDeclarationKeywords -> {
                        expectQuotedProcedureName = true
                    }

                    expectQuotedProcedureName && isQuotedWord(word) -> {
                        addProcedure(procedures, word.substring(1), uri, lineNumber, lexeme)
                        expectQuotedProcedureName = false
                    }

                    lower == "make" -> {
                        expectQuotedVariableName = true
                        quotedVariableScope = null
                    }

                    lower == "localmake" -> {
                        expectQuotedVariableName = true
                        quotedVariableScope = currentProcedure?.name
                    }

                    expectQuotedVariableName && isQuotedWord(word) -> {
                        addVariable(
                            variables,
                            word.substring(1),
                            quotedVariableScope,
                            uri,
                            lineNumber,
                            lexeme
                        )
                        expectQuotedVariableName = false
                        quotedVariableScope = null
                    }

                    lower == "name" -> {
                        expectNameValue = true
                        expectNameVariable = false
                    }

                    expectNameVariable && isQuotedWord(word) -> {
                        addVariable(variables, word.substring(1), null, uri, lineNumber, lexeme)
                        expectNameVariable = false
                    }

                    lower == "local" -> {
                        collectLocalDeclarations = true
                    }

                    collectLocalDeclarations && isQuotedWord(word) -> {
                        addVariable(
                            variables,
                            word.substring(1),
                            currentProcedure?.name,
                            uri,
                            lineNumber,
                            lexeme
                        )
                    }

                    lower in LogoLanguage.loopKeywords -> {
                        expectLoopControlList = true
                    }

                    expectLoopControlList && word == "[" -> {
                        expectLoopControlList = false
                        expectLoopVariable = true
                    }

                    expectLoopVariable && LogoLanguage.isIdentifier(word) -> {
                        addVariable(variables, word, currentProcedure?.name, uri, lineNumber, lexeme)
                        expectLoopVariable = false
                    }
                }
            }
        }

        return DefinitionMaps(procedures, variables, procedureScopes)
    }

    private fun addProcedure(
        procedures: MutableMap<String, MutableList<Location>>,
        name: String,
        uri: String,
        lineNumber: Int,
        lexeme: LogoLexeme
    ) {
        procedures
            .getOrPut(name.lowercase()) { mutableListOf() }
            .add(location(uri, lineNumber, lexeme))
    }

    private fun addVariable(
        variables: MutableMap<String, MutableList<VariableDefinition>>,
        name: String,
        scope: String?,
        uri: String,
        lineNumber: Int,
        lexeme: LogoLexeme
    ) {
        variables
            .getOrPut(name.lowercase()) { mutableListOf() }
            .add(VariableDefinition(scope, location(uri, lineNumber, lexeme)))
    }

    private fun resolveProcedure(
        name: String,
        procedures: Map<String, List<Location>>,
        cursor: Position
    ): Location? {
        return procedures[name]?.lastOrNull { isBeforeOrAt(it.range.start, cursor) }
    }

    private fun resolveVariable(
        name: String,
        variables: Map<String, List<VariableDefinition>>,
        currentProcedure: String?,
        cursor: Position
    ): Location? {
        val matches = variables[name] ?: return null

        if (currentProcedure != null) {
            matches.lastOrNull {
                it.scope == currentProcedure && isBeforeOrAt(it.location.range.start, cursor)
            }?.let { return it.location }
        }

        return matches.lastOrNull {
            it.scope == null && isBeforeOrAt(it.location.range.start, cursor)
        }?.location
    }

    private fun location(uri: String, lineNumber: Int, lexeme: LogoLexeme): Location {
        return Location(
            uri,
            Range(
                Position(lineNumber, lexeme.start),
                Position(lineNumber, lexeme.endExclusive)
            )
        )
    }

    private fun isBeforeOrAt(declaration: Position, cursor: Position): Boolean {
        return declaration.line < cursor.line ||
            (declaration.line == cursor.line && declaration.character <= cursor.character)
    }

    private fun isVariableWord(word: String): Boolean {
        return word.startsWith(":") && word.length > 1
    }

    private fun isQuotedWord(word: String): Boolean {
        return word.startsWith("\"") && word.length > 1
    }

    private data class DefinitionMaps(
        val procedures: Map<String, List<Location>>,
        val variables: Map<String, List<VariableDefinition>>,
        val procedureScopes: List<ProcedureScope>
    ) {
        fun procedureAt(lineNumber: Int): String? {
            return procedureScopes.lastOrNull { lineNumber in it.startLine..it.endLine }?.name
        }
    }

    private data class VariableDefinition(
        val scope: String?,
        val location: Location
    )

    private data class ProcedureScope(
        val name: String,
        val startLine: Int,
        var endLine: Int
    )

    private data class Symbol(
        val name: String,
        val kind: SymbolKind
    )

    private enum class SymbolKind {
        PROCEDURE,
        VARIABLE
    }
}
