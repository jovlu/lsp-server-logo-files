package logo
import org.eclipse.lsp4j.launch.*
import java.io.File

val logFile = File("server-log.txt")

fun log(message: String) {
    logFile.appendText(message + "\n")
    System.err.println(message)
}

fun main() {
    log("server process started")

    val server = LogoServer()
    val launcher = LSPLauncher.createServerLauncher(server, System.`in`, System.out)
    launcher.startListening().get()
}