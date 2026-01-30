package id.project.df.dnote.feature.note.presentation.editor

import app.cash.turbine.test
import id.project.df.dnote.core.testing.MainDispatcherRule
import id.project.df.dnote.feature.note.data.repository.Result
import id.project.df.dnote.feature.note.di.NoteEditor
import id.project.df.dnote.feature.note.domain.model.Note
import id.project.df.dnote.feature.note.domain.repository.NoteRepositoryInterface
import id.project.df.dnote.feature.note.domain.usecase.DeleteNoteUseCase
import id.project.df.dnote.feature.note.domain.usecase.UpsertNoteUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class NoteEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val upsertNote: UpsertNoteUseCase = mockk(relaxed = true)
    private val deleteNote: DeleteNoteUseCase = mockk(relaxed = true)
    private val repo: NoteRepositoryInterface = mockk(relaxed = true)

    private lateinit var viewModel: NoteEditorViewModel

    @Test
    fun `init_withNavKey_loadsNote`() = runTest {
        val noteId = "123"
        val note = Note(noteId, "Title", "Content", 0L, 0L)
        coEvery { repo.getNote(noteId) } returns flowOf(Result.Success(note))

        viewModel = NoteEditorViewModel(NoteEditor(noteId), upsertNote, deleteNote, repo)
        runCurrent()

        assertEquals(noteId, viewModel.uiState.value.noteId)
        assertEquals("Title", viewModel.uiState.value.title)
        assertEquals("Content", viewModel.uiState.value.contentText)
    }

    @Test
    fun `onContentChanged_updatesState_and_schedulesAutosave`() = runTest {
        viewModel = NoteEditorViewModel(NoteEditor(null), upsertNote, deleteNote, repo)

        viewModel.onContentChanged("New Content")
        assertEquals("New Content", viewModel.uiState.value.contentText)

        advanceTimeBy(401)
        runCurrent()
        coVerify { upsertNote(null, "", "New Content") }
    }

    @Test
    fun `undo_restoresPreviousState`() = runTest {
        viewModel = NoteEditorViewModel(NoteEditor(null), upsertNote, deleteNote, repo)

        viewModel.onContentChanged("A")
        advanceTimeBy(601)
        runCurrent()
        
        viewModel.onContentChanged("AB")
        assertEquals("AB", viewModel.uiState.value.contentText)
        
        viewModel.onUndo()
        assertEquals("A", viewModel.uiState.value.contentText)
    }

    @Test
    fun `redo_restoresUndoneState`() = runTest {
        viewModel = NoteEditorViewModel(NoteEditor(null), upsertNote, deleteNote, repo)

        viewModel.onContentChanged("A")
        advanceTimeBy(702)
        runCurrent()

        viewModel.onContentChanged("B")
        advanceTimeBy(701)
        runCurrent()

        viewModel.onUndo()
        assertEquals("A", viewModel.uiState.value.contentText)
        assertTrue(viewModel.uiState.value.canRedo)

        viewModel.onRedo()
        assertEquals("B", viewModel.uiState.value.contentText)
    }

    @Test
    fun `saveInternal_emptyContent_deletesExistingNote`() = runTest {
        val noteId = "123"
        coEvery { repo.getNote(noteId) } returns flowOf(Result.Success(Note(noteId, "T", "C", 0, 0)))
        
        viewModel = NoteEditorViewModel(NoteEditor(noteId), upsertNote, deleteNote, repo)
        runCurrent()
        
        viewModel.onTitleChanged("")
        viewModel.onContentChanged("")
        
        viewModel.onCloseRequested()
        advanceTimeBy(10)
        runCurrent()

        coVerify { deleteNote(noteId) }
        coVerify(exactly = 0) { upsertNote(any(), any(), any()) }
    }

    @Test
    fun `saveInternal_emptyContent_skipsUpsertForNewNote`() = runTest {
        viewModel = NoteEditorViewModel(NoteEditor(null), upsertNote, deleteNote, repo)
        
        viewModel.onTitleChanged("")
        viewModel.onContentChanged("")
        
        viewModel.onCloseRequested()
        advanceTimeBy(10)
        runCurrent()

        coVerify(exactly = 0) { deleteNote(any()) }
        coVerify(exactly = 0) { upsertNote(any(), any(), any()) }
    }

    @Test
    fun `onCloseRequested_emitsCloseEventOnSuccess`() = runTest {
        viewModel = NoteEditorViewModel(NoteEditor(null), upsertNote, deleteNote, repo)
        viewModel.onContentChanged("Save Me")

        viewModel.events.test {
            viewModel.onCloseRequested()
            assertEquals(NoteEditorEvent.Close, awaitItem())
        }
    }

    @Test
    fun `saveInternal_failure_updatesState`() = runTest {
        coEvery { upsertNote(any(), any(), any()) } throws RuntimeException("Fail")
        
        viewModel = NoteEditorViewModel(NoteEditor(null), upsertNote, deleteNote, repo)
        viewModel.onContentChanged("Content")

        viewModel.onCloseRequested()
        advanceTimeBy(10)
        runCurrent()
        
        assertEquals("Fail", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isSaving)
    }
}
