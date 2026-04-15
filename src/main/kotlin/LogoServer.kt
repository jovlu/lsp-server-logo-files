package logo

import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.SemanticTokenModifiers
import org.eclipse.lsp4j.SemanticTokenTypes
import org.eclipse.lsp4j.SemanticTokensLegend
import org.eclipse.lsp4j.SemanticTokensWithRegistrationOptions
import org.eclipse.lsp4j.ServerCapabilities
import org.eclipse.lsp4j.TextDocumentSyncKind
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.LanguageServer
import org.eclipse.lsp4j.services.TextDocumentService
import org.eclipse.lsp4j.services.WorkspaceService
import java.util.concurrent.CompletableFuture

class LogoServer : LanguageServer {
    private val textDocuments = LogoTextDocuments()
    private val workspace = LogoWorkspace()

    val legend = SemanticTokensLegend(
        listOf(
            SemanticTokenTypes.Keyword,
            SemanticTokenTypes.Function,
            SemanticTokenTypes.Parameter,
            SemanticTokenTypes.Variable,
            SemanticTokenTypes.Number,
            SemanticTokenTypes.Comment
        ),
        listOf(SemanticTokenModifiers.Declaration)
    )

    override fun initialize(params: InitializeParams): CompletableFuture<InitializeResult> {
        log("initialize received")

        val capabilities = ServerCapabilities().apply {
            textDocumentSync = Either.forLeft(TextDocumentSyncKind.Full)
            semanticTokensProvider = SemanticTokensWithRegistrationOptions(legend, true, false)
            definitionProvider = Either.forLeft(true)
            hoverProvider = Either.forLeft(true)
        }

        return CompletableFuture.completedFuture(InitializeResult(capabilities))
    }

    override fun shutdown(): CompletableFuture<Any> =
        CompletableFuture.completedFuture(null)

    override fun exit() {
        log("exit")
        System.exit(0)
    }

    override fun getTextDocumentService(): TextDocumentService = textDocuments
    override fun getWorkspaceService(): WorkspaceService = workspace
}