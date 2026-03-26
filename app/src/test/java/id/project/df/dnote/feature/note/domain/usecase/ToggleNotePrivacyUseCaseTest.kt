package id.project.df.dnote.feature.note.domain.usecase

import id.project.df.dnote.feature.note.domain.repository.NoteRepositoryInterface
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ToggleNotePrivacyUseCaseTest {

    private val repository: NoteRepositoryInterface = mockk(relaxed = true)
    private val togglePrivacy = ToggleNotePrivacyUseCase(repository)

    @Test
    fun `invoke_withTrue_callsRepositoryTogglePrivacy`() = runTest {
        togglePrivacy("123", true)

        coVerify { repository.togglePrivacy("123", true) }
    }

    @Test
    fun `invoke_withFalse_callsRepositoryWithFalse`() = runTest {
        togglePrivacy("123", false)

        coVerify { repository.togglePrivacy("123", false) }
    }
}
