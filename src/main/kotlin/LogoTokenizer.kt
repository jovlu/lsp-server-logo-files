package logo

class LogoTokenizer {

    fun tokenize(text: String): List<RawToken> {
        // ovo skupljam usput da kasniji pozivi budu function a ne obican text
        val knownProcedures = mutableSetOf<String>()
        val tokens = mutableListOf<RawToken>()

        for ((lineNumber, line) in text.lines().withIndex()) {
            val scannedLine = LogoLanguage.scanLine(line)

            var expectProcedureName = false
            var onToLine = false
            var expectedQuotedWord = QuotedWordKind.NONE
            var expectLoopControlList = false
            var expectLoopVariable = false
            var expectNameValue = false
            var expectNameVariable = false
            var collectLocalDeclarations = false

            fun addToken(lexeme: LogoLexeme, type: String, declaration: Boolean = false) {
                tokens += RawToken(
                    line = lineNumber,
                    start = lexeme.start,
                    length = lexeme.endExclusive - lexeme.start,
                    type = type,
                    declaration = declaration
                )
            }

            for (lexeme in scannedLine.lexemes) {
                val word = lexeme.text
                val lower = word.lowercase()

                // local moze vise quoted imena zaredom
                if (collectLocalDeclarations && !isQuotedWord(word)) {
                    collectLocalDeclarations = false
                }

                when {
                    expectNameValue -> {
                        // name prvo vrednost, pa onda cekam "ime
                        when {
                            word in structuralOperators -> addToken(lexeme, "operator")
                            LogoLanguage.startsNumber(word, 0) -> addToken(lexeme, "number")
                            LogoLanguage.isOperatorStart(word, 0) -> addToken(lexeme, "operator")
                            isVariableWord(word) -> addToken(lexeme, "variable")
                            isQuotedWord(word) -> addToken(lexeme, "string")
                            lower in knownProcedures -> addToken(lexeme, "function")
                            lower in LogoLanguage.keywordNames -> addToken(lexeme, "keyword")
                        }

                        expectNameValue = false
                        expectNameVariable = true
                        expectedQuotedWord = QuotedWordKind.NONE
                        expectLoopVariable = false
                    }

                    word in structuralOperators -> {
                        addToken(lexeme, "operator")
                        if (word == "[" && expectLoopControlList) {
                            // tek kad udje u [] krecem da cekam loop promenljivu
                            expectLoopControlList = false
                            expectLoopVariable = true
                        }
                    }

                    LogoLanguage.startsNumber(word, 0) -> {
                        addToken(lexeme, "number")
                        expectedQuotedWord = QuotedWordKind.NONE
                        expectLoopVariable = false
                    }

                    LogoLanguage.isOperatorStart(word, 0) -> {
                        addToken(lexeme, "operator")
                    }

                    lower == "to" -> {
                        addToken(lexeme, "keyword")
                        expectProcedureName = true
                        onToLine = true
                        expectedQuotedWord = QuotedWordKind.NONE
                    }

                    expectProcedureName -> {
                        addToken(lexeme, "function", declaration = true)
                        knownProcedures += lower
                        expectProcedureName = false
                        expectedQuotedWord = QuotedWordKind.NONE
                    }

                    onToLine && isVariableWord(word) -> {
                        addToken(lexeme, "parameter", declaration = true)
                        expectedQuotedWord = QuotedWordKind.NONE
                    }

                    lower in LogoLanguage.variableDeclarationKeywords -> {
                        addToken(lexeme, "keyword")
                        expectedQuotedWord = QuotedWordKind.VARIABLE_DECLARATION
                    }

                    lower == "name" -> {
                        addToken(lexeme, "keyword")
                        expectNameValue = true
                        expectNameVariable = false
                        expectedQuotedWord = QuotedWordKind.NONE
                    }

                    lower == "local" -> {
                        addToken(lexeme, "keyword")
                        collectLocalDeclarations = true
                        expectedQuotedWord = QuotedWordKind.NONE
                    }

                    lower in LogoLanguage.variableReferenceKeywords -> {
                        addToken(lexeme, "keyword")
                        expectedQuotedWord = QuotedWordKind.VARIABLE_REFERENCE
                    }

                    lower in LogoLanguage.procedureDeclarationKeywords -> {
                        addToken(lexeme, "keyword")
                        expectedQuotedWord = QuotedWordKind.PROCEDURE_DECLARATION
                    }

                    lower in LogoLanguage.procedureReferenceKeywords -> {
                        addToken(lexeme, "keyword")
                        expectedQuotedWord = QuotedWordKind.PROCEDURE_REFERENCE
                    }

                    lower in LogoLanguage.loopKeywords -> {
                        addToken(lexeme, "keyword")
                        expectedQuotedWord = QuotedWordKind.NONE
                        expectLoopControlList = true
                    }

                    lower in LogoLanguage.keywordNames -> {
                        addToken(lexeme, "keyword")
                        expectedQuotedWord = QuotedWordKind.NONE
                    }

                    expectLoopVariable && LogoLanguage.isIdentifier(word) -> {
                        addToken(lexeme, "variable", declaration = true)
                        expectLoopVariable = false
                        expectedQuotedWord = QuotedWordKind.NONE
                    }

                    expectNameVariable && isQuotedWord(word) -> {
                        addToken(lexeme, "variable", declaration = true)
                        expectNameVariable = false
                    }

                    collectLocalDeclarations && isQuotedWord(word) -> {
                        addToken(lexeme, "variable", declaration = true)
                    }

                    isVariableWord(word) -> {
                        addToken(lexeme, "variable")
                        expectedQuotedWord = QuotedWordKind.NONE
                    }

                    isQuotedWord(word) -> {
                        if (expectedQuotedWord == QuotedWordKind.PROCEDURE_DECLARATION) {
                            // ako je define "foo, posle foo prepoznam kao proceduru
                            knownProcedures += word.substring(1).lowercase()
                        }

                        addToken(
                            lexeme,
                            expectedQuotedWord.tokenType,
                            declaration = expectedQuotedWord.isDeclaration
                        )
                        expectedQuotedWord = QuotedWordKind.NONE
                    }

                    lower in knownProcedures -> {
                        addToken(lexeme, "function")
                        expectedQuotedWord = QuotedWordKind.NONE
                    }

                    else -> {
                        expectedQuotedWord = QuotedWordKind.NONE
                        expectLoopVariable = false
                    }
                }
            }

            scannedLine.commentStart?.let { commentStart ->
                // komentar uzimam u komadu
                tokens += RawToken(
                    line = lineNumber,
                    start = commentStart,
                    length = line.length - commentStart,
                    type = "comment"
                )
            }
        }

        return tokens
    }

    private fun isVariableWord(word: String): Boolean {
        return word.startsWith(":") && word.length > 1
    }

    private fun isQuotedWord(word: String): Boolean {
        return word.startsWith("\"") && word.length > 1
    }

    private enum class QuotedWordKind(val tokenType: String, val isDeclaration: Boolean) {
        NONE("string", false),
        VARIABLE_DECLARATION("variable", true),
        VARIABLE_REFERENCE("variable", false),
        PROCEDURE_DECLARATION("function", true),
        PROCEDURE_REFERENCE("function", false)
    }

    private companion object {
        private val structuralOperators = setOf("[", "]", "(", ")", ",")
    }
}
