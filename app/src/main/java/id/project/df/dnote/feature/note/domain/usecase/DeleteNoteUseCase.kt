package id.project.df.dnote.feature.note.domain.usecase

import id.project.df.dnote.feature.note.domain.repository.NoteRepositoryInterface
import javax.inject.Inject

class DeleteNoteUseCase @Inject constructor(
    private val repo: NoteRepositoryInterface
) {
    suspend operator fun invoke(id: String) = repo.delete(id)
}