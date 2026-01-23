package id.project.df.dnote.feature.note.domain.usecase

import id.project.df.dnote.feature.note.domain.repository.NoteRepositoryInterface
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DeleteNoteUseCaseTest {

    private val repository: NoteRepositoryInterface = mockk(relaxed = true)
    private val deleteNoteUseCase = DeleteNoteUseCase(repository)

    @Test
    fun `invoke_callsRepositoryDelete`() = runTest {
        val noteId = "123"

        deleteNoteUseCase(noteId)

        coVerify { repository.delete(noteId) }
    }
}
