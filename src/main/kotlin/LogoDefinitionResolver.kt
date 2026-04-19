package logo

import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range

class LogoDefinitionResolver {

    fun buildIndex(text: String, uri: String): DefinitionIndex {
        val procedures = mutableMapOf<String, MutableList<Location>>()
        val variables = mutableMapOf<String, MutableList<VariableDef>>()
        val scopes = mutableListOf<ProcedureScope>()
        val lines = text.lines()

        // ovde pamtim u kom smo to..end bloku
        var currentScope: ProcedureScope? = null

        for ((lineNumber, line) in lines.withIndex()) {
            val lexemes = LogoLanguage.scanLine(line).lexemes
            var expectProcedureName = false
            var onHeader = false
            var expectQuotedProcedure = false
            var expectQuotedVariable = false
            var quotedVariableScopeId: Int? = null
            var collectLocals = false
            var expectLoopList = false
            var expectLoopVariable = false
            var expectNameValue = false
            var expectNameVariable = false

            for (lexeme in lexemes) {
                val word = lexeme.text
                val lower = word.lowercase()

                // local moze vise "ime" jedno za drugim, cim to prestane gasim flag
                if (collectLocals && !isQuotedWord(word)) collectLocals = false

                when {
                    expectNameValue -> {
                        expectNameValue = false
                        expectNameVariable = true
                    }
                    lower == "to" -> {
                        // zatvori stari scope
                        currentScope?.endLine = lineNumber - 1
                        expectProcedureName = true
                        onHeader = true
                    }
                    expectProcedureName -> {
                        addProcedure(procedures, lower, uri, lineNumber, lexeme)
                        currentScope = ProcedureScope(scopes.size, lineNumber, lines.lastIndex)
                        scopes += currentScope
                        expectProcedureName = false
                    }
                    lower == "end" -> {
                        currentScope?.endLine = lineNumber
                        currentScope = null
                    }
                    onHeader && isVariableWord(word) -> {
                        addVariable(variables, LogoLanguage.normalizeSymbol(word), currentScope, uri, lineNumber, lexeme)
                    }
                    lower in LogoLanguage.procedureDeclarationKeywords -> {
                        expectQuotedProcedure = true
                    }
                    expectQuotedProcedure && isQuotedWord(word) -> {
                        addProcedure(procedures, LogoLanguage.normalizeSymbol(word), uri, lineNumber, lexeme)
                        expectQuotedProcedure = false
                    }
                    lower == "make" -> {
                        expectQuotedVariable = true
                        quotedVariableScopeId = null
                    }
                    lower == "localmake" -> {
                        expectQuotedVariable = true
                        quotedVariableScopeId = currentScope?.id
                    }
                    expectQuotedVariable && isQuotedWord(word) -> {
                        addVariable(
                            variables,
                            LogoLanguage.normalizeSymbol(word),
                            scopes.lastOrNull { it.id == quotedVariableScopeId },
                            uri,
                            lineNumber,
                            lexeme
                        )
                        expectQuotedVariable = false
                        quotedVariableScopeId = null
                    }
                    lower == "name" -> {
                        // name je naopako, prvo ide vrednost pa tek onda ime prom
                        expectNameValue = true
                        expectNameVariable = false
                    }
                    expectNameVariable && isQuotedWord(word) -> {
                        addVariable(variables, LogoLanguage.normalizeSymbol(word), null, uri, lineNumber, lexeme)
                        expectNameVariable = false
                    }
                    lower == "local" -> {
                        collectLocals = true
                    }
                    collectLocals && isQuotedWord(word) -> {
                        addVariable(variables, LogoLanguage.normalizeSymbol(word), currentScope, uri, lineNumber, lexeme)
                    }
                    lower in LogoLanguage.loopKeywords -> {
                        expectLoopList = true
                    }
                    expectLoopList && word == "[" -> {
                        // loop var za for/dotimes iskace tek iz [] liste
                        expectLoopList = false
                        expectLoopVariable = true
                    }
                    expectLoopVariable && LogoLanguage.isIdentifier(word) -> {
                        addVariable(variables, lower, currentScope, uri, lineNumber, lexeme)
                        expectLoopVariable = false
                    }
                }
            }
        }

        return DefinitionIndex(lines, procedures, variables, scopes)
    }

    fun findDefinition(text: String, cursor: Position, uri: String): Location? {
        return findDefinition(buildIndex(text, uri), cursor)
    }

    fun findDefinition(index: DefinitionIndex, cursor: Position): Location? {
        val line = index.lines.getOrNull(cursor.line) ?: return null
        val lexemeAt = LogoLanguage.findLexemeAt(line, cursor.character) ?: return null
        val symbol = classify(lexemeAt) ?: return null

        return if (symbol.kind == SymbolKind.PROCEDURE) {
            index.procedures[symbol.name]?.lastOrNull { beforeOrAt(it.range.start, cursor) }
        } else {
            val defs = index.variables[symbol.name] ?: return null
            val scopeId = index.scopeAt(cursor.line)?.id
            defs.lastOrNull { it.scopeId == scopeId && beforeOrAt(it.location.range.start, cursor) }?.location
                ?: defs.lastOrNull { it.scopeId == null && beforeOrAt(it.location.range.start, cursor) }?.location
        }
    }

    private fun classify(lexemeAt: LogoLexemeAt): Symbol? {
        val word = lexemeAt.lexeme.text
        if (word in STRUCTURAL) return null
        if (LogoLanguage.startsNumber(word, 0)) return null
        if (LogoLanguage.isOperatorStart(word, 0)) return null

        return when {
            isVariableWord(word) -> Symbol(LogoLanguage.normalizeSymbol(word), SymbolKind.VARIABLE)
            isQuotedWord(word) -> when (LogoLanguage.quotedWordRole(lexemeAt.lexemes, lexemeAt.index)) {
                QuotedWordRole.VARIABLE_DECLARATION, QuotedWordRole.VARIABLE_REFERENCE ->
                    Symbol(LogoLanguage.normalizeSymbol(word), SymbolKind.VARIABLE)
                QuotedWordRole.PROCEDURE_DECLARATION, QuotedWordRole.PROCEDURE_REFERENCE ->
                    Symbol(LogoLanguage.normalizeSymbol(word), SymbolKind.PROCEDURE)
                QuotedWordRole.PLAIN_STRING -> null
            }
            else -> Symbol(word.lowercase(), SymbolKind.PROCEDURE)
        }
    }

    private fun addProcedure(
        procedures: MutableMap<String, MutableList<Location>>,
        name: String,
        uri: String,
        lineNumber: Int,
        lexeme: LogoLexeme
    ) {
        procedures.getOrPut(name) { mutableListOf() }.add(location(uri, lineNumber, lexeme))
    }

    private fun addVariable(
        variables: MutableMap<String, MutableList<VariableDef>>,
        name: String,
        scope: ProcedureScope?,
        uri: String,
        lineNumber: Int,
        lexeme: LogoLexeme
    ) {
        variables.getOrPut(name) { mutableListOf() }.add(
            VariableDef(location(uri, lineNumber, lexeme), scope?.id)
        )
    }

    private fun location(uri: String, lineNumber: Int, lexeme: LogoLexeme) = Location(
        uri,
        Range(Position(lineNumber, lexeme.start), Position(lineNumber, lexeme.endExclusive))
    )

    private fun beforeOrAt(definition: Position, cursor: Position): Boolean {
        return definition.line < cursor.line ||
            (definition.line == cursor.line && definition.character <= cursor.character)
    }

    private fun isVariableWord(word: String) = word.startsWith(":") && word.length > 1
    private fun isQuotedWord(word: String) = word.startsWith("\"") && word.length > 1

    data class DefinitionIndex(
        val lines: List<String>,
        val procedures: Map<String, List<Location>>,
        val variables: Map<String, List<VariableDef>>,
        val scopes: List<ProcedureScope>
    ) {
        // uzmi zadnji scope koji pokriva line
        fun scopeAt(line: Int): ProcedureScope? = scopes.lastOrNull { line in it.startLine..it.endLine }
    }

    data class VariableDef(val location: Location, val scopeId: Int?)
    data class ProcedureScope(val id: Int, val startLine: Int, var endLine: Int)

    private data class Symbol(val name: String, val kind: SymbolKind)
    private enum class SymbolKind { PROCEDURE, VARIABLE }

    private companion object {
        private val STRUCTURAL = setOf("[", "]", "(", ")", ",")
    }
}
