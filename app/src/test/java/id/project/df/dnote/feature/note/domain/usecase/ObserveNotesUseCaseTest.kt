package id.project.df.dnote.feature.note.domain.usecase

import id.project.df.dnote.feature.note.data.repository.Result
import id.project.df.dnote.feature.note.domain.model.Note
import id.project.df.dnote.feature.note.domain.repository.NoteRepositoryInterface
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveNotesUseCaseTest {

    private val repository: NoteRepositoryInterface = mockk()
    private val observeNotesUseCase = ObserveNotesUseCase(repository)

    @Test
    fun `invoke_callsRepositoryObserve`() = runTest {
        val query = "test"
        val notes = listOf(Note("1", "Title", "Content", 0L, 0L))
        coEvery { repository.observeNotes(query) } returns flowOf(Result.Success(notes))

        val result = observeNotesUseCase(query).first()

        assertEquals(Result.Success(notes), result)
        coVerify { repository.observeNotes(query) }
    }
}
