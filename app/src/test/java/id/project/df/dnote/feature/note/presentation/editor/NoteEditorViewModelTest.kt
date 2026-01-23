package id.project.df.dnote.feature.note.presentation.editor

import app.cash.turbine.test
import id.project.df.dnote.core.testing.MainDispatcherRule
import id.project.df.dnote.feature.note.data.repository.Result
import id.project.df.dnote.feature.note.di.NoteEditor
import id.project.df.dnote.feature.note.domain.model.Note
import id.project.df.dnote.feature.note.domain.repository.NoteRepositoryInterface
import id.project.df.dnote.feature.note.domain.usecase.UpsertNoteUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class NoteEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val upsertNote: UpsertNoteUseCase = mockk(relaxed = true)
    private val repo: NoteRepositoryInterface = mockk(relaxed = true)

    private lateinit var viewModel: NoteEditorViewModel

    @Test
    fun `init_withNavKey_loadsNote`() = runTest {
        val noteId = "123"
        val note = Note(noteId, "Title", "Content", 0L, 0L)
        coEvery { repo.getNote(noteId) } returns flowOf(Result.Success(note))

        viewModel = NoteEditorViewModel(NoteEditor(noteId), upsertNote, repo)

        viewModel.uiState.test {
            var item = awaitItem()
            if (item.noteId != noteId) {
                item = awaitItem()
            }
            
            assertEquals(noteId, item.noteId)
            assertEquals("Title", item.title)
            assertEquals("Content", item.contentText)
        }
    }

    @Test
    fun `onContentChanged_updatesState_and_schedulesAutosave`() = runTest {
        viewModel = NoteEditorViewModel(NoteEditor(null), upsertNote, repo)

        viewModel.uiState.test {
            skipItems(1)

            viewModel.onContentChanged("New Content")
            
            val item = awaitItem()
            assertEquals("New Content", item.contentText)
        }

        advanceTimeBy(401) 
        coVerify { upsertNote(null, "", "New Content") }
    }

    @Test
    fun `onTitleChanged_updatesState_and_schedulesAutosave`() = runTest {
        viewModel = NoteEditorViewModel(NoteEditor(null), upsertNote, repo)

        viewModel.uiState.test {
            skipItems(1)

            viewModel.onTitleChanged("New Title")
            
            val item = awaitItem()
            assertEquals("New Title", item.title)
        }

        advanceTimeBy(401)
        coVerify { upsertNote(null, "New Title", "") }
    }
    
    @Test
    fun `onCloseRequested_savesAndEmitsClose`() = runTest {
        viewModel = NoteEditorViewModel(NoteEditor(null), upsertNote, repo)
        viewModel.onContentChanged("Save Me")
        
        viewModel.events.test {
            viewModel.onCloseRequested()
            assertEquals(NoteEditorEvent.Close, awaitItem())
        }
        
        coVerify { upsertNote(null, "", "Save Me") }
    }

    @Test
    fun `saveInternal_failure_updatesStateAndEmitsError`() = runTest {
        val errorMsg = "Save Failed"
        coEvery { upsertNote(any(), any(), any()) } coAnswers {
            kotlinx.coroutines.delay(1)
            throw RuntimeException(errorMsg)
        }
        
        viewModel = NoteEditorViewModel(NoteEditor(null), upsertNote, repo)
        viewModel.onContentChanged("Content")

        val eventJob = launch {
            viewModel.events.test {
                val event = awaitItem()
                assertTrue(event is NoteEditorEvent.ShowError)
                assertEquals(errorMsg, (event as NoteEditorEvent.ShowError).message)
            }
        }

        viewModel.uiState.test {
            val contentItem = awaitItem()
            assertEquals("Content", contentItem.contentText)
            
            viewModel.onCloseRequested()
            
            val savingItem = awaitItem()
            assertTrue(savingItem.isSaving)
            
            advanceTimeBy(1)
            
            val errorItem = awaitItem()
            assertEquals(errorMsg, errorItem.errorMessage)
            assertFalse(errorItem.isSaving)
        }
        
        eventJob.join()
    }
    
    @Test
    fun `init_errorLoadingNote_setsErrorMessage`() = runTest {
        val noteId = "123"
        val errorMsg = "Load Error"
        coEvery { repo.getNote(noteId) } returns flowOf(Result.Error(Exception(errorMsg)))

        viewModel = NoteEditorViewModel(NoteEditor(noteId), upsertNote, repo)

        viewModel.uiState.test {

            var item = awaitItem()
            if (item.errorMessage == null) {
                item = awaitItem()
            }
            
            assertEquals(errorMsg, item.errorMessage)
        }
    }
}
