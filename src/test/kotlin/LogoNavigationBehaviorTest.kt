package logo

import org.eclipse.lsp4j.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class LogoNavigationBehaviorTest {

    private val definitionResolver = LogoDefinitionResolver()
    private val hoverProvider = LogoHoverProvider()

    private val text = """
        square 10
        to square :n
        end
        square 20
    """.trimIndent()

    @Test
    fun resolvesProcedureDeclarationEvenWhenItAppearsLater() {
        val forwardReferenceDefinition =
            definitionResolver.findDefinition(text, Position(0, 1), "file:///test.logo")
        assertNotNull(forwardReferenceDefinition)
        assertEquals(1, forwardReferenceDefinition.range.start.line)
        assertEquals(3, forwardReferenceDefinition.range.start.character)

        val definition = definitionResolver.findDefinition(text, Position(3, 1), "file:///test.logo")
        assertNotNull(definition)
    }

    @Test
    fun showsProcedureHoverBeforeDeclaration() {
        assertNotNull(hoverProvider.hover(text, Position(0, 1)))
        assertNotNull(hoverProvider.hover(text, Position(3, 1)))
    }

    @Test
    fun doesNotTreatQuotedNameValueAsVariableDeclaration() {
        val text = """
            name "hello "x
            thing "x
        """.trimIndent()

        assertEquals(
            null,
            definitionResolver.findDefinition(text, Position(0, 6), "file:///test.logo")
        )

        val definition = definitionResolver.findDefinition(text, Position(1, 7), "file:///test.logo")
        assertNotNull(definition)
    }

    @Test
    fun resolvesMostSpecificVariableDefinitionInScope() {
        val text = """
            make "x 1
            to demo :x
            local "x
            print :x
            end
        """.trimIndent()

        val definition = definitionResolver.findDefinition(text, Position(3, 7), "file:///test.logo")
        assertNotNull(definition)
        assertEquals(2, definition.range.start.line)
        assertEquals(6, definition.range.start.character)
    }

    @Test
    fun prefersEarlierVisibleDefinitionOverLaterOne() {
        val text = """
            to demo :x
            print :x
            local "x
            end
        """.trimIndent()

        val definition = definitionResolver.findDefinition(text, Position(1, 7), "file:///test.logo")
        assertNotNull(definition)
        assertEquals(0, definition.range.start.line)
        assertEquals(8, definition.range.start.character)
    }
}
