package id.project.df.dnote.feature.note.domain.usecase

import id.project.df.dnote.feature.note.domain.repository.NoteRepositoryInterface
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpsertNoteUseCaseTest {

    private val repository: NoteRepositoryInterface = mockk(relaxed = true)
    private val upsertNoteUseCase = UpsertNoteUseCase(repository)

    @Test
    fun `invoke_nullIdAndEmptyContent_returnsNull`() = runTest {
        val result = upsertNoteUseCase(null, "Title", "   ")

        assertNull(result)
        coVerify(exactly = 0) { repository.create(any(), any()) }
    }

    @Test
    fun `invoke_nullId_callsCreate`() = runTest {
        val newId = "newId"
        coEvery { repository.create("Title", "Content") } returns newId

        val result = upsertNoteUseCase(null, "Title", "Content")

        assertEquals(newId, result)
        coVerify { repository.create("Title", "Content") }
    }

    @Test
    fun `invoke_existingId_callsUpdate`() = runTest {
        val id = "123"

        val result = upsertNoteUseCase(id, "Title", "Content")

        assertEquals(id, result)
        coVerify { repository.update(id, "Title", "Content") }
    }

    @Test
    fun `invoke_trimsContent`() = runTest {
        val id = "123"
        
        upsertNoteUseCase(id, "Title", "  Content  ")

        coVerify { repository.update(id, "Title", "Content") }
    }
}
