package id.project.df.dnote.feature.note.domain.model

data class Note(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toPreview(): String {
        val maxChars = 80
        val oneLine = content.trim().replace("\n", " ")
        return if (oneLine.length <= maxChars) oneLine else oneLine.take(maxChars).trimEnd() + "…"
    }
}