package logo

data class RawToken(
    val line: Int,
    val start: Int,
    val length: Int,
    val type: String,
    val declaration: Boolean = false
)