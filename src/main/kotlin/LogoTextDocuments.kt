package logo
import org.eclipse.lsp4j.DefinitionParams
import org.eclipse.lsp4j.DidChangeTextDocumentParams
import org.eclipse.lsp4j.DidCloseTextDocumentParams
import org.eclipse.lsp4j.DidOpenTextDocumentParams
import org.eclipse.lsp4j.DidSaveTextDocumentParams
import org.eclipse.lsp4j.Hover
import org.eclipse.lsp4j.HoverParams
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.SemanticTokenTypes
import org.eclipse.lsp4j.SemanticTokens
import org.eclipse.lsp4j.SemanticTokensParams
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.TextDocumentService
import java.util.concurrent.CompletableFuture

class LogoTextDocuments : TextDocumentService {

    private val documents = mutableMapOf<String,String>()
    private val definitionResolver = LogoDefinitionResolver()
    private val hoverProvider = LogoHoverProvider()

    private val tokenTypes = listOf(
        SemanticTokenTypes.Keyword,   // 0
        SemanticTokenTypes.Function,  // 1
        SemanticTokenTypes.Parameter, // 2
        SemanticTokenTypes.Variable,  // 3
        SemanticTokenTypes.Number,    // 4
        SemanticTokenTypes.Comment    // 5
    )

    override fun didOpen(params: DidOpenTextDocumentParams) {
        val uri = params.textDocument.uri
        val text = params.textDocument.text
        documents[uri] = text
        log("didOpen: ${uri}")
    }

    override fun didChange(params: DidChangeTextDocumentParams) {
        val uri = params.textDocument.uri
        val newText = params.contentChanges.firstOrNull()?.text ?: return
        documents[uri] = newText
        log("didChange")
    }

    override fun didClose(params: DidCloseTextDocumentParams) {
        val uri = params.textDocument.uri
        documents.remove(uri)
        log("didClose: ${params.textDocument.uri}")
    }

    override fun didSave(params: DidSaveTextDocumentParams) {
        log("didSave")
    }

    override fun semanticTokensFull(params: SemanticTokensParams): CompletableFuture<SemanticTokens> {
        val uri = params.textDocument.uri
        log("semanticTokensFull: $uri")

        val text = documents[uri] ?: ""
        val tokenizer = LogoTokenizer()
        val rawTokens = tokenizer.tokenize(text)

        val typeToIndex = mapOf(
            "keyword" to 0,
            "function" to 1,
            "parameter" to 2,
            "variable" to 3,
            "number" to 4,
            "comment" to 5
        )

        val data = mutableListOf<Int>()

        var previousLine = 0
        var previousStart = 0

        for (token in rawTokens) {
            val deltaLine = token.line - previousLine
            val deltaStart = if (deltaLine == 0) token.start - previousStart else token.start
            val typeIndex = typeToIndex[token.type] ?: 0
            val modifierBits = if (token.declaration) 1 else 0

            data += deltaLine
            data += deltaStart
            data += token.length
            data += typeIndex
            data += modifierBits

            previousLine = token.line
            previousStart = token.start
        }

        return CompletableFuture.completedFuture(
            SemanticTokens(data)
        )
    }

    override fun definition(params: DefinitionParams): CompletableFuture<Either<List<Location>, List<org.eclipse.lsp4j.LocationLink>>> {
        val uri = params.textDocument.uri
        val text = documents[uri] ?: ""

        log("definition: $uri at ${params.position.line}:${params.position.character}")

        val location = definitionResolver.findDefinition(text, params.position, uri)

        return CompletableFuture.completedFuture(
            Either.forLeft(location?.let { listOf(it) } ?: emptyList())
        )
    }

    override fun hover(params: HoverParams): CompletableFuture<Hover?>? {
        val uri = params.textDocument.uri
        val text = documents[uri] ?: ""

        log("hover: $uri at ${params.position.line}:${params.position.character}")

        val hover = hoverProvider.hover(text, params.position)
        return CompletableFuture.completedFuture(hover)
    }
}
