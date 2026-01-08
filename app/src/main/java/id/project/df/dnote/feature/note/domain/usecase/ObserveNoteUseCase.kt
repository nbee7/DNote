package id.project.df.dnote.feature.note.domain.usecase

import id.project.df.dnote.feature.note.domain.model.Note
import id.project.df.dnote.feature.note.domain.repository.NoteRepositoryInterface
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveNotesUseCase @Inject constructor(
    private val repo: NoteRepositoryInterface
) {
    operator fun invoke(query: String): Flow<List<Note>> = repo.observeNotes(query)
}