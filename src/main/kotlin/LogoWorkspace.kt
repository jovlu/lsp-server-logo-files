package logo
import org.eclipse.lsp4j.DidChangeConfigurationParams
import org.eclipse.lsp4j.DidChangeWatchedFilesParams
import org.eclipse.lsp4j.services.WorkspaceService

class LogoWorkspace : WorkspaceService {
    override fun didChangeConfiguration(params: DidChangeConfigurationParams) {
        log("didChangeConfiguration")
    }

    override fun didChangeWatchedFiles(params: DidChangeWatchedFilesParams) {
        log("didChangeWatchedFiles")
    }
}
