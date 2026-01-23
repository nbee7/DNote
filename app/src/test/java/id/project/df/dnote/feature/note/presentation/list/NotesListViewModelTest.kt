package id.project.df.dnote.feature.note.presentation.list

import app.cash.turbine.test
import id.project.df.dnote.core.testing.MainDispatcherRule
import id.project.df.dnote.feature.note.data.repository.Result
import id.project.df.dnote.feature.note.domain.model.Note
import id.project.df.dnote.feature.note.domain.usecase.DeleteNoteUseCase
import id.project.df.dnote.feature.note.domain.usecase.ObserveNotesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class NotesListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observeNotes: ObserveNotesUseCase = mockk()
    private val deleteNote: DeleteNoteUseCase = mockk(relaxed = true)

    private lateinit var viewModel: NotesListViewModel

    @Test
    fun `init_loadsNotes`() = runTest {
        val notes = listOf(Note("1", "Title", "Content", 0L, 0L))
        coEvery { observeNotes("") } returns flowOf(Result.Success(notes))

        viewModel = NotesListViewModel(observeNotes, deleteNote)

        viewModel.uiState.test {
            skipItems(1)
            val item = awaitItem()
            assertEquals("1", item.items.first().id)
        }
    }

    @Test
    fun `onQueryChanged_updatesQuery_and_reloads`() = runTest {
        coEvery { observeNotes("") } returns flowOf(Result.Success(emptyList()))
        val searchResult = listOf(Note("2", "Found", "Content", 0L, 0L))
        coEvery { observeNotes("search") } returns flowOf(Result.Success(searchResult))
        
        viewModel = NotesListViewModel(observeNotes, deleteNote)
        
        viewModel.uiState.test {
            skipItems(1)
            
            viewModel.onQueryChanged("search")
            
            val queryState = awaitItem()
            assertEquals("search", queryState.query)
            
            val resultState = awaitItem()
            assertEquals("search", resultState.query)
            assertEquals("2", resultState.items.first().id)
        }
    }

    @Test
    fun `onDeleteClicked_callsDelete`() = runTest {
        coEvery { observeNotes(any()) } returns flowOf(Result.Success(emptyList()))
        viewModel = NotesListViewModel(observeNotes, deleteNote)

        viewModel.onDeleteClicked("123")
        advanceTimeBy(10) 

        coVerify { deleteNote("123") }
    }

    @Test
    fun `init_observesError_updatesState`() = runTest {
        val errorMsg = "Network Error"
        coEvery { observeNotes("") } returns flowOf(Result.Error(Exception(errorMsg)))
        
        viewModel = NotesListViewModel(observeNotes, deleteNote)

        viewModel.uiState.test {
            skipItems(1)
            
            val item = awaitItem()
            assertEquals(errorMsg, item.errorMessage)
            assertEquals(false, item.isLoading)
        }
    }

    @Test
    fun `init_streamException_updatesState`() = runTest {
        val errorMsg = "Crash"
        coEvery { observeNotes("") } throws RuntimeException(errorMsg)
        
        viewModel = NotesListViewModel(observeNotes, deleteNote)

        viewModel.uiState.test {
             skipItems(1)
             
             val item = awaitItem()
             assertEquals(errorMsg, item.errorMessage)
        }
    }
}
