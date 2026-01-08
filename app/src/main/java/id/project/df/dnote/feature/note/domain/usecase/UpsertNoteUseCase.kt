package id.project.df.dnote.feature.note.domain.usecase

import id.project.df.dnote.feature.note.domain.repository.NoteRepositoryInterface
import javax.inject.Inject

class UpsertNoteUseCase @Inject constructor(
    private val repo: NoteRepositoryInterface
) {
    suspend operator fun invoke(noteId: String?, rawContent: String): String? {
        val content = rawContent.trim()

        if (noteId == null && content.isEmpty()) return null

        return if (noteId == null) {
            repo.create(content)
        } else {
            repo.update(noteId, content)
            noteId
        }
    }
}