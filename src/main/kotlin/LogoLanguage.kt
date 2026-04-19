package logo

data class LogoLexeme(
    val text: String,
    val start: Int,
    val endExclusive: Int
)

data class LogoScannedLine(
    val lexemes: List<LogoLexeme>,
    val commentStart: Int? = null
)

data class LogoLexemeAt(
    val index: Int,
    val lexeme: LogoLexeme,
    val lexemes: List<LogoLexeme>
)

enum class QuotedWordRole {
    PLAIN_STRING,
    VARIABLE_DECLARATION,
    VARIABLE_REFERENCE,
    PROCEDURE_DECLARATION,
    PROCEDURE_REFERENCE
}

private data class LogoBuiltin(
    val names: Set<String>,
    val description: String
)

object LogoLanguage {
    val variableDeclarationKeywords = setOf("make", "localmake")
    val variableReferenceKeywords = setOf("thing")
    val procedureDeclarationKeywords = setOf("define")
    val procedureReferenceKeywords = setOf("def")
    val loopKeywords = setOf("for", "dotimes")

    private val operatorPairs = setOf("<=", ">=", "<>")
    val operatorChars = setOf('=', '<', '>', '+', '-', '*', '/', '%')

    private val builtins = listOf(
        builtin("Draws an arc using the given angle and radius.", "arc"),
        builtin("Creates an array with the requested size.", "array"),
        builtin("Returns whether a value is an array.", "array?", "arrayp"),
        builtin("Moves the turtle backward.", "back", "bk"),
        builtin("Tests lexical ordering between two words.", "before?", "beforep"),
        builtin("Returns all items except the first.", "butfirst"),
        builtin("Returns all items except the last.", "butlast"),
        builtin("Terminates the current program.", "bye"),
        builtin("Changes the turtle shape.", "changeshape", "csh"),
        builtin("Clears the drawing without moving the turtle home.", "clean"),
        builtin("Clears the screen and returns the turtle home.", "clearscreen", "cs"),
        builtin("Returns the stored definition of a procedure.", "def"),
        builtin("Defines a procedure from a quoted name and body.", "define"),
        builtin("Runs a block at least once and repeats until the expression becomes true.", "do.until"),
        builtin("Runs a block at least once and repeats while the expression stays true.", "do.while"),
        builtin("Runs a block a fixed number of times with a loop variable.", "dotimes"),
        builtin("Draws an ellipse with the given width and height.", "ellipse"),
        builtin("Tests whether a value is empty.", "empty?", "emptyp"),
        builtin("Ends a `to` procedure declaration.", "end"),
        builtin("Tests whether two values are equal.", "equal?", "equalp"),
        builtin("Moves the turtle forward.", "fd", "forward"),
        builtin("Prevents the turtle from moving beyond the screen bounds.", "fence"),
        builtin("Flood-fills the current region.", "fill"),
        builtin("Executes a block and fills the traced region.", "filled"),
        builtin("Returns the first item of a list.", "first"),
        builtin("Iterates using a control list and a loop variable.", "for"),
        builtin("Returns the current turtle heading.", "heading"),
        builtin("Hides the turtle.", "hideturtle", "ht"),
        builtin("Moves the turtle to the home position.", "home"),
        builtin("Executes a block when the expression is true.", "if"),
        builtin("Chooses between two blocks based on an expression.", "ifelse"),
        builtin("Executes the false branch after `test`.", "iffalse"),
        builtin("Executes the true branch after `test`.", "iftrue"),
        builtin("Returns the item at a given index from a list or array.", "item"),
        builtin("Draws text at the turtle position.", "label"),
        builtin("Returns the current label size.", "labelsize"),
        builtin("Returns the last item of a list.", "last"),
        builtin("Turns the turtle left.", "left", "lt"),
        builtin("Creates a list from the given values.", "list"),
        builtin("Returns whether a value is a list.", "list?", "listp"),
        builtin("Declares local variables in the current procedure scope.", "local"),
        builtin("Creates or updates a local variable.", "localmake"),
        builtin("Creates or updates a variable using a quoted name.", "make"),
        builtin("Subtracts the second value from the first.", "minus"),
        builtin("Returns the modulo of two values.", "modulo"),
        builtin("Creates or updates a variable with the inputs reversed.", "name"),
        builtin("Tests whether two values are not equal.", "notequal?", "notequalp"),
        builtin("Returns whether a value is numeric.", "number?", "numberp"),
        builtin("Returns the current pen color.", "pc", "pencolor"),
        builtin("Lowers the pen so movement draws.", "pd", "pendown"),
        builtin("Returns whether the pen is down.", "pendown?", "pendownp"),
        builtin("Lifts the pen so movement does not draw.", "penup", "pu"),
        builtin("Returns the current pen size.", "pensize"),
        builtin("Returns a random item from a list.", "pick"),
        builtin("Returns the current turtle position.", "pos"),
        builtin("Raises a value to a power.", "power"),
        builtin("Prints or shows a value in the text output.", "print", "show"),
        builtin("Returns a random number in a range.", "random"),
        builtin("Prompts the user and returns a list.", "readlist"),
        builtin("Prompts the user and returns a word.", "readword"),
        builtin("Repeats a block a fixed number of times.", "repeat"),
        builtin("Returns the current repeat or loop iteration count.", "repcount"),
        builtin("Turns the turtle right.", "right", "rt"),
        builtin("Part of `set pos`, which moves the turtle to a specific position.", "set"),
        builtin("Sets the current pen color.", "setcolor", "setpencolor"),
        builtin("Sets the turtle heading.", "setheading", "seth", "sh"),
        builtin("Sets the size used by `label`.", "setlabelheight"),
        builtin("Sets the pen width.", "setpensize", "setwidth"),
        builtin("Moves the turtle to a specific X coordinate.", "setx"),
        builtin("Moves the turtle to a specific position.", "setxy"),
        builtin("Moves the turtle to a specific Y coordinate.", "sety"),
        builtin("Returns whether the turtle is visible.", "shown?", "shownp"),
        builtin("Shows the turtle.", "showturtle", "st"),
        builtin("Tests whether one word is a substring of another.", "substring?", "substringp"),
        builtin("Adds two values.", "sum"),
        builtin("Stores a test value for `iftrue` and `iffalse`.", "test"),
        builtin("Looks up a variable value from a quoted name.", "thing"),
        builtin("Starts a `to` procedure declaration.", "to"),
        builtin("Returns the heading towards a target position.", "towards"),
        builtin("Runs a block until the expression becomes true.", "until"),
        builtin("Pauses execution for a number of ticks.", "wait"),
        builtin("Runs a block while the expression stays true.", "while"),
        builtin("Lets the turtle move beyond the screen without wrapping.", "window"),
        builtin("Returns or constructs a word value.", "word", "word?"),
        builtin("Wraps turtle movement across screen edges.", "wrap"),
        builtin("Returns the turtle X coordinate.", "xcor"),
        builtin("Returns the turtle Y coordinate.", "ycor")
    )

    val builtinDocs: Map<String, String> = buildMap {
        for (builtin in builtins) {
            for (name in builtin.names) {
                put(name, builtin.description)
            }
        }
    }

    val keywordNames: Set<String> = builtinDocs.keys

    fun scanLine(line: String): LogoScannedLine {
        val lexemes = mutableListOf<LogoLexeme>()
        var i = 0

        while (i < line.length) {
            val ch = line[i]

            if (ch.isWhitespace()) {
                i++
                continue
            }

            if (ch == ';') {
                return LogoScannedLine(lexemes, commentStart = i)
            }

            if (ch == '[' || ch == ']' || ch == '(' || ch == ')' || ch == ',') {
                lexemes += LogoLexeme(ch.toString(), i, i + 1)
                i++
                continue
            }

            if (startsNumber(line, i)) {
                val start = i
                i = consumeNumber(line, i)
                lexemes += LogoLexeme(line.substring(start, i), start, i)
                continue
            }

            if (isOperatorStart(line, i)) {
                val length = operatorLength(line, i)
                lexemes += LogoLexeme(line.substring(i, i + length), i, i + length)
                i += length
                continue
            }

            val start = i
            while (i < line.length && !isWordBoundary(line[i])) {
                i++
            }

            lexemes += LogoLexeme(line.substring(start, i), start, i)
        }

        return LogoScannedLine(lexemes)
    }

    fun findLexemeAt(line: String, character: Int): LogoLexemeAt? {
        if (line.isEmpty()) return null

        val safeChar = character.coerceIn(0, line.length)
        val lexemes = scanLine(line).lexemes

        for ((index, lexeme) in lexemes.withIndex()) {
            if (safeChar in lexeme.start..lexeme.endExclusive) {
                return LogoLexemeAt(index, lexeme, lexemes)
            }
        }

        return null
    }

    fun quotedWordRole(lexemes: List<LogoLexeme>, index: Int): QuotedWordRole {
        val word = lexemes.getOrNull(index)?.text ?: return QuotedWordRole.PLAIN_STRING
        if (!word.startsWith("\"") || word.length <= 1) return QuotedWordRole.PLAIN_STRING

        for (candidate in index - 1 downTo 0) {
            val lower = lexemes[candidate].text.lowercase()
            when {
                lower in variableDeclarationKeywords -> return QuotedWordRole.VARIABLE_DECLARATION
                lower == "local" -> return QuotedWordRole.VARIABLE_DECLARATION
                lower == "name" -> {
                    return if (candidate == index - 1) {
                        QuotedWordRole.PLAIN_STRING
                    } else {
                        QuotedWordRole.VARIABLE_DECLARATION
                    }
                }
                lower in variableReferenceKeywords -> return QuotedWordRole.VARIABLE_REFERENCE
                lower in procedureDeclarationKeywords -> return QuotedWordRole.PROCEDURE_DECLARATION
                lower in procedureReferenceKeywords -> return QuotedWordRole.PROCEDURE_REFERENCE
                lower in keywordNames -> continue
            }
        }

        return QuotedWordRole.PLAIN_STRING
    }

    fun normalizeSymbol(word: String): String {
        return when {
            word.startsWith(":") && word.length > 1 -> word.substring(1).lowercase()
            word.startsWith("\"") && word.length > 1 -> word.substring(1).lowercase()
            else -> word.lowercase()
        }
    }

    fun startsNumber(line: String, index: Int): Boolean {
        val ch = line[index]
        return ch.isDigit() || (ch == '-' && line.getOrNull(index + 1)?.isDigit() == true)
    }

    fun consumeNumber(line: String, start: Int): Int {
        var i = start

        if (line[i] == '-') {
            i++
        }

        while (i < line.length && line[i].isDigit()) {
            i++
        }

        if (i < line.length && line[i] == '.' && line.getOrNull(i + 1)?.isDigit() == true) {
            i++
            while (i < line.length && line[i].isDigit()) {
                i++
            }
        }

        return i
    }

    fun isOperatorStart(line: String, index: Int): Boolean {
        val ch = line[index]
        if (ch !in operatorChars) return false
        if (ch != '-') return true

        val next = line.getOrNull(index + 1)
        val previous = line.getOrNull(index - 1)
        return !(next?.isDigit() == true && (previous == null || isWordBoundary(previous)))
    }

    fun operatorLength(line: String, index: Int): Int {
        val pair = line.substring(index, minOf(index + 2, line.length))
        return if (pair in operatorPairs) 2 else 1
    }

    fun isWordBoundary(ch: Char): Boolean {
        return ch.isWhitespace() ||
            ch == ';' ||
            ch == '[' ||
            ch == ']' ||
            ch == '(' ||
            ch == ')' ||
            ch == ',' ||
            ch in operatorChars
    }

    fun isIdentifier(word: String): Boolean {
        return word.isNotEmpty() && word.all { it.isLetterOrDigit() || it == '_' || it == '?' }
    }

    private fun builtin(description: String, vararg names: String): LogoBuiltin {
        return LogoBuiltin(names.toSet(), description)
    }
}
