package logo

import kotlin.test.Test
import kotlin.test.assertEquals

class LogoTokenizerTest {

    private val tokenizer = LogoTokenizer()

    @Test
    fun tokenizesProceduresParametersAndCalls() {
        val text = """
            to square :n
            square :n
            end
        """.trimIndent()

        assertEquals(
            listOf(
                RawToken(0, 0, 2, "keyword"),
                RawToken(0, 3, 6, "function", declaration = true),
                RawToken(0, 10, 2, "parameter", declaration = true),
                RawToken(1, 0, 6, "function"),
                RawToken(1, 7, 2, "variable"),
                RawToken(2, 0, 3, "keyword")
            ),
            tokenizer.tokenize(text)
        )
    }

    @Test
    fun tokenizesQuotedWordsByContext() {
        val text = """
            make "x 10
            thing "x
            define "proc [show "hello]
            def "proc
        """.trimIndent()

        assertEquals(
            listOf(
                RawToken(0, 0, 4, "keyword"),
                RawToken(0, 5, 2, "variable", declaration = true),
                RawToken(0, 8, 2, "number"),
                RawToken(1, 0, 5, "keyword"),
                RawToken(1, 6, 2, "variable"),
                RawToken(2, 0, 6, "keyword"),
                RawToken(2, 7, 5, "function", declaration = true),
                RawToken(2, 13, 1, "operator"),
                RawToken(2, 14, 4, "keyword"),
                RawToken(2, 19, 6, "string"),
                RawToken(2, 25, 1, "operator"),
                RawToken(3, 0, 3, "keyword"),
                RawToken(3, 4, 5, "function")
            ),
            tokenizer.tokenize(text)
        )
    }

    @Test
    fun tokenizesLoopDeclarationsLocalDeclarationsAndComments() {
        val text = """
            name 5 "x
            local "a "b value
            for [i 1 3] [print :i]
            fd 10 ; move
        """.trimIndent()

        assertEquals(
            listOf(
                RawToken(0, 0, 4, "keyword"),
                RawToken(0, 5, 1, "number"),
                RawToken(0, 7, 2, "variable", declaration = true),
                RawToken(1, 0, 5, "keyword"),
                RawToken(1, 6, 2, "variable", declaration = true),
                RawToken(1, 9, 2, "variable", declaration = true),
                RawToken(2, 0, 3, "keyword"),
                RawToken(2, 4, 1, "operator"),
                RawToken(2, 5, 1, "variable", declaration = true),
                RawToken(2, 7, 1, "number"),
                RawToken(2, 9, 1, "number"),
                RawToken(2, 10, 1, "operator"),
                RawToken(2, 12, 1, "operator"),
                RawToken(2, 13, 5, "keyword"),
                RawToken(2, 19, 2, "variable"),
                RawToken(2, 21, 1, "operator"),
                RawToken(3, 0, 2, "keyword"),
                RawToken(3, 3, 2, "number"),
                RawToken(3, 6, 6, "comment")
            ),
            tokenizer.tokenize(text)
        )
    }

    @Test
    fun tokenizesQuotedNameValueBeforeVariableDeclaration() {
        val text = """
            name "hello "x
            thing "x
        """.trimIndent()

        assertEquals(
            listOf(
                RawToken(0, 0, 4, "keyword"),
                RawToken(0, 5, 6, "string"),
                RawToken(0, 12, 2, "variable", declaration = true),
                RawToken(1, 0, 5, "keyword"),
                RawToken(1, 6, 2, "variable")
            ),
            tokenizer.tokenize(text)
        )
    }

    @Test
    fun doesNotHighlightProcedureBeforeItIsDeclared() {
        val text = """
            square :n
            to square :n
            end
            square :n
        """.trimIndent()

        assertEquals(
            listOf(
                RawToken(0, 7, 2, "variable"),
                RawToken(1, 0, 2, "keyword"),
                RawToken(1, 3, 6, "function", declaration = true),
                RawToken(1, 10, 2, "parameter", declaration = true),
                RawToken(2, 0, 3, "keyword"),
                RawToken(3, 0, 6, "function"),
                RawToken(3, 7, 2, "variable")
            ),
            tokenizer.tokenize(text)
        )
    }
}
