package id.project.df.dnote.feature.note.presentation.editor

import app.cash.turbine.test
import id.project.df.dnote.core.data.SessionRepository
import id.project.df.dnote.core.testing.MainDispatcherRule
import id.project.df.dnote.feature.note.data.repository.Result
import id.project.df.dnote.feature.note.di.NoteEditor
import id.project.df.dnote.feature.note.domain.model.Note
import id.project.df.dnote.feature.note.domain.repository.NoteRepositoryInterface
import id.project.df.dnote.feature.note.domain.usecase.DeleteNoteUseCase
import id.project.df.dnote.feature.note.domain.usecase.ToggleNotePrivacyUseCase
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
    private val sessionRepo: SessionRepository = mockk(relaxed = true)
    private val toggleNotePrivacy: ToggleNotePrivacyUseCase = mockk(relaxed = true)

    private lateinit var viewModel: NoteEditorViewModel

    @Test
    fun `init_withNavKey_loadsNote`() = runTest {
        val noteId = "123"
        val note = Note(noteId, "Title", "Content", 0L, 0L)
        coEvery { repo.getNote(noteId) } returns flowOf(Result.Success(note))

        viewModel = NoteEditorViewModel(NoteEditor(noteId), upsertNote, deleteNote, repo, sessionRepo, toggleNotePrivacy)
        runCurrent()

        assertEquals(noteId, viewModel.uiState.value.noteId)
        assertEquals("Title", viewModel.uiState.value.title)
        assertEquals("Content", viewModel.uiState.value.contentText)
    }

    @Test
    fun `onContentChanged_updatesState_and_schedulesAutosave`() = runTest {
        viewModel = NoteEditorViewModel(NoteEditor(null), upsertNote, deleteNote, repo, sessionRepo, toggleNotePrivacy)

        viewModel.onContentChanged("New Content")
        assertEquals("New Content", viewModel.uiState.value.contentText)

        advanceTimeBy(401)
        runCurrent()
        coVerify { upsertNote(null, "", "New Content") }
    }

    @Test
    fun `undo_restoresPreviousState`() = runTest {
        viewModel = NoteEditorViewModel(NoteEditor(null), upsertNote, deleteNote, repo, sessionRepo, toggleNotePrivacy)

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
        viewModel = NoteEditorViewModel(NoteEditor(null), upsertNote, deleteNote, repo, sessionRepo, toggleNotePrivacy)

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
        
        viewModel = NoteEditorViewModel(NoteEditor(noteId), upsertNote, deleteNote, repo, sessionRepo, toggleNotePrivacy)
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
        viewModel = NoteEditorViewModel(NoteEditor(null), upsertNote, deleteNote, repo, sessionRepo, toggleNotePrivacy)
        
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
        viewModel = NoteEditorViewModel(NoteEditor(null), upsertNote, deleteNote, repo, sessionRepo, toggleNotePrivacy)
        viewModel.onContentChanged("Save Me")

        viewModel.events.test {
            viewModel.onCloseRequested()
            assertEquals(NoteEditorEvent.Close, awaitItem())
        }
    }

    @Test
    fun `saveInternal_failure_updatesState`() = runTest {
        coEvery { upsertNote(any(), any(), any()) } throws RuntimeException("Fail")

        viewModel = NoteEditorViewModel(NoteEditor(null), upsertNote, deleteNote, repo, sessionRepo, toggleNotePrivacy)
        viewModel.onContentChanged("Content")

        viewModel.onCloseRequested()
        advanceTimeBy(10)
        runCurrent()

        assertEquals("Fail", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `onNewTab_addsTabAndSwitchesToIt`() = runTest {
        viewModel = NoteEditorViewModel(NoteEditor(null), upsertNote, deleteNote, repo, sessionRepo, toggleNotePrivacy)

        viewModel.onNewTab()
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(2, state.tabCount)
        assertEquals(1, state.activeTabIndex)
        assertEquals("", state.title)
        assertEquals("", state.contentText)
    }

    @Test
    fun `onNewTab_savesCurrentTabBeforeSwitching`() = runTest {
        viewModel = NoteEditorViewModel(NoteEditor(null), upsertNote, deleteNote, repo, sessionRepo, toggleNotePrivacy)

        viewModel.onContentChanged("Draft")
        viewModel.onNewTab()
        runCurrent()

        coVerify { upsertNote(null, "", "Draft") }
    }

    @Test
    fun `onNewTab_preservesPreviousTabContent`() = runTest {
        viewModel = NoteEditorViewModel(NoteEditor(null), upsertNote, deleteNote, repo, sessionRepo, toggleNotePrivacy)

        viewModel.onContentChanged("Tab0 Content")
        viewModel.onNewTab()
        runCurrent()

        viewModel.onSwitchTab(0)
        runCurrent()

        assertEquals("Tab0 Content", viewModel.uiState.value.contentText)
    }

    @Test
    fun `onSwitchTab_changesToTargetTab`() = runTest {
        viewModel = NoteEditorViewModel(NoteEditor(null), upsertNote, deleteNote, repo, sessionRepo, toggleNotePrivacy)

        viewModel.onContentChanged("First")
        viewModel.onNewTab()
        runCurrent()

        viewModel.onContentChanged("Second")

        viewModel.onSwitchTab(0)
        runCurrent()
        assertEquals("First", viewModel.uiState.value.contentText)

        viewModel.onSwitchTab(1)
        runCurrent()
        assertEquals("Second", viewModel.uiState.value.contentText)
    }

    @Test
    fun `onSwitchTab_invalidIndex_noOp`() = runTest {
        viewModel = NoteEditorViewModel(NoteEditor(null), upsertNote, deleteNote, repo, sessionRepo, toggleNotePrivacy)

        viewModel.onSwitchTab(5)
        runCurrent()

        assertEquals(0, viewModel.uiState.value.activeTabIndex)
        assertEquals(1, viewModel.uiState.value.tabCount)
    }

    @Test
    fun `onSwitchTab_sameIndex_noOp`() = runTest {
        viewModel = NoteEditorViewModel(NoteEditor(null), upsertNote, deleteNote, repo, sessionRepo, toggleNotePrivacy)

        viewModel.onContentChanged("Content")
        viewModel.onSwitchTab(0)
        runCurrent()

        // saveInternal is not triggered for same-index switch (only autosave from onContentChanged)
        assertEquals(0, viewModel.uiState.value.activeTabIndex)
    }

    @Test
    fun `onNoteSelected_newNote_opensInNewTab`() = runTest {
        viewModel = NoteEditorViewModel(NoteEditor(null), upsertNote, deleteNote, repo, sessionRepo, toggleNotePrivacy)

        val note = Note("note1", "Selected", "Body", 0L, 0L)
        viewModel.onNoteSelected(note)
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(2, state.tabCount)
        assertEquals(1, state.activeTabIndex)
        assertEquals("note1", state.noteId)
        assertEquals("Selected", state.title)
        assertEquals("Body", state.contentText)
    }

    @Test
    fun `onNoteSelected_existingTab_switchesToIt`() = runTest {
        viewModel = NoteEditorViewModel(NoteEditor(null), upsertNote, deleteNote, repo, sessionRepo, toggleNotePrivacy)

        val note = Note("note1", "Title", "Content", 0L, 0L)
        viewModel.onNoteSelected(note)
        runCurrent()
        assertEquals(2, viewModel.uiState.value.tabCount)

        // Switch back to tab 0
        viewModel.onSwitchTab(0)
        runCurrent()

        // Select same note again — should switch, not create a 3rd tab
        viewModel.onNoteSelected(note)
        runCurrent()

        assertEquals(2, viewModel.uiState.value.tabCount)
        assertEquals(1, viewModel.uiState.value.activeTabIndex)
        assertEquals("note1", viewModel.uiState.value.noteId)
    }

    @Test
    fun `tabCount_reflectsNumberOfOpenTabs`() = runTest {
        viewModel = NoteEditorViewModel(NoteEditor(null), upsertNote, deleteNote, repo, sessionRepo, toggleNotePrivacy)

        assertEquals(1, viewModel.uiState.value.tabCount)

        viewModel.onNewTab()
        runCurrent()
        assertEquals(2, viewModel.uiState.value.tabCount)

        viewModel.onNewTab()
        runCurrent()
        assertEquals(3, viewModel.uiState.value.tabCount)
    }

    // --- Close tab tests ---

    @Test
    fun `onCloseTab_removesTabAndAdjustsIndex`() = runTest {
        viewModel = NoteEditorViewModel(NoteEditor(null), upsertNote, deleteNote, repo, sessionRepo, toggleNotePrivacy)

        // Create 3 tabs: [0, 1, 2], active = 2
        viewModel.onNewTab()
        runCurrent()
        viewModel.onNewTab()
        runCurrent()
        assertEquals(3, viewModel.uiState.value.tabCount)
        assertEquals(2, viewModel.uiState.value.activeTabIndex)

        // Close tab 0 (non-active, before active)
        viewModel.onCloseTab(0)
        runCurrent()

        assertEquals(2, viewModel.uiState.value.tabCount)
        assertEquals(1, viewModel.uiState.value.activeTabIndex) // decremented
    }

    @Test
    fun `onCloseTab_activeTab_switchesToNearest`() = runTest {
        viewModel = NoteEditorViewModel(NoteEditor(null), upsertNote, deleteNote, repo, sessionRepo, toggleNotePrivacy)

        viewModel.onContentChanged("Tab0")
        viewModel.onNewTab()
        runCurrent()
        viewModel.onContentChanged("Tab1")

        // Active is tab 1, close it
        viewModel.onCloseTab(1)
        runCurrent()

        assertEquals(1, viewModel.uiState.value.tabCount)
        assertEquals(0, viewModel.uiState.value.activeTabIndex)
        assertEquals("Tab0", viewModel.uiState.value.contentText)
    }

    @Test
    fun `onCloseTab_lastTab_createsNewBlank`() = runTest {
        viewModel = NoteEditorViewModel(NoteEditor(null), upsertNote, deleteNote, repo, sessionRepo, toggleNotePrivacy)

        viewModel.onContentChanged("Something")
        viewModel.onCloseTab(0)
        runCurrent()

        assertEquals(1, viewModel.uiState.value.tabCount)
        assertEquals(0, viewModel.uiState.value.activeTabIndex)
        assertEquals("", viewModel.uiState.value.title)
        assertEquals("", viewModel.uiState.value.contentText)
    }

    @Test
    fun `onCloseTab_savesTabBeforeRemoving`() = runTest {
        viewModel = NoteEditorViewModel(NoteEditor(null), upsertNote, deleteNote, repo, sessionRepo, toggleNotePrivacy)

        // Open a note in a tab
        val note = Note("n1", "Title", "Body", 0L, 0L)
        viewModel.onNoteSelected(note)
        runCurrent()

        // Close that tab (index 1)
        viewModel.onCloseTab(1)
        runCurrent()

        coVerify { upsertNote("n1", "Title", "Body") }
    }

    // --- Tab grid toggle tests ---

    @Test
    fun `onTabsClick_togglesShowTabGrid`() = runTest {
        viewModel = NoteEditorViewModel(NoteEditor(null), upsertNote, deleteNote, repo, sessionRepo, toggleNotePrivacy)

        assertFalse(viewModel.uiState.value.showTabGrid)

        viewModel.onTabsClick()
        assertTrue(viewModel.uiState.value.showTabGrid)

        viewModel.onTabsClick()
        assertFalse(viewModel.uiState.value.showTabGrid)
    }

    @Test
    fun `onDismissTabGrid_hidesGrid`() = runTest {
        viewModel = NoteEditorViewModel(NoteEditor(null), upsertNote, deleteNote, repo, sessionRepo, toggleNotePrivacy)

        viewModel.onTabsClick()
        assertTrue(viewModel.uiState.value.showTabGrid)

        viewModel.onDismissTabGrid()
        assertFalse(viewModel.uiState.value.showTabGrid)
    }

    @Test
    fun `onNewTab_dismissesTabGrid`() = runTest {
        viewModel = NoteEditorViewModel(NoteEditor(null), upsertNote, deleteNote, repo, sessionRepo, toggleNotePrivacy)

        viewModel.onTabsClick()
        assertTrue(viewModel.uiState.value.showTabGrid)

        viewModel.onNewTab()
        runCurrent()
        assertFalse(viewModel.uiState.value.showTabGrid)
    }

    @Test
    fun `onSwitchTab_dismissesTabGrid`() = runTest {
        viewModel = NoteEditorViewModel(NoteEditor(null), upsertNote, deleteNote, repo, sessionRepo, toggleNotePrivacy)

        viewModel.onNewTab()
        runCurrent()

        viewModel.onTabsClick()
        assertTrue(viewModel.uiState.value.showTabGrid)

        viewModel.onSwitchTab(0)
        runCurrent()
        assertFalse(viewModel.uiState.value.showTabGrid)
    }

    // --- Session persistence tests ---

    @Test
    fun `init_withSession_restoresTabs`() = runTest {
        val note1 = Note("id1", "Title1", "Content1", 0L, 0L)
        val note2 = Note("id2", "Title2", "Content2", 0L, 0L)
        coEvery { sessionRepo.getSession() } returns id.project.df.dnote.core.data.SessionData(
            noteIds = listOf("id1", "id2"),
            activeTabIndex = 1
        )
        coEvery { repo.getNote("id1") } returns flowOf(Result.Success(note1))
        coEvery { repo.getNote("id2") } returns flowOf(Result.Success(note2))

        viewModel = NoteEditorViewModel(NoteEditor(null), upsertNote, deleteNote, repo, sessionRepo, toggleNotePrivacy)
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(2, state.tabCount)
        assertEquals(1, state.activeTabIndex)
        assertEquals("Title2", state.title)
        assertEquals("Content2", state.contentText)
    }

    @Test
    fun `init_withSession_restoresActiveIndex`() = runTest {
        val note1 = Note("id1", "T1", "C1", 0L, 0L)
        val note2 = Note("id2", "T2", "C2", 0L, 0L)
        val note3 = Note("id3", "T3", "C3", 0L, 0L)
        coEvery { sessionRepo.getSession() } returns id.project.df.dnote.core.data.SessionData(
            noteIds = listOf("id1", "id2", "id3"),
            activeTabIndex = 2
        )
        coEvery { repo.getNote("id1") } returns flowOf(Result.Success(note1))
        coEvery { repo.getNote("id2") } returns flowOf(Result.Success(note2))
        coEvery { repo.getNote("id3") } returns flowOf(Result.Success(note3))

        viewModel = NoteEditorViewModel(NoteEditor(null), upsertNote, deleteNote, repo, sessionRepo, toggleNotePrivacy)
        runCurrent()

        assertEquals(2, viewModel.uiState.value.activeTabIndex)
        assertEquals("T3", viewModel.uiState.value.title)
    }

    @Test
    fun `init_noSession_startsBlank`() = runTest {
        coEvery { sessionRepo.getSession() } returns null

        viewModel = NoteEditorViewModel(NoteEditor(null), upsertNote, deleteNote, repo, sessionRepo, toggleNotePrivacy)
        runCurrent()

        assertEquals(1, viewModel.uiState.value.tabCount)
        assertEquals("", viewModel.uiState.value.title)
        assertEquals("", viewModel.uiState.value.contentText)
    }

    @Test
    fun `onNewTab_savesSession`() = runTest {
        viewModel = NoteEditorViewModel(NoteEditor(null), upsertNote, deleteNote, repo, sessionRepo, toggleNotePrivacy)

        // First select a note so there's a noteId to persist
        val note = Note("n1", "T", "C", 0L, 0L)
        viewModel.onNoteSelected(note)
        runCurrent()

        viewModel.onNewTab()
        runCurrent()

        coVerify(atLeast = 1) { sessionRepo.saveSession(any(), any()) }
    }

    @Test
    fun `onCloseTab_savesSession`() = runTest {
        viewModel = NoteEditorViewModel(NoteEditor(null), upsertNote, deleteNote, repo, sessionRepo, toggleNotePrivacy)

        val note = Note("n1", "T", "C", 0L, 0L)
        viewModel.onNoteSelected(note)
        runCurrent()

        viewModel.onCloseTab(1)
        runCurrent()

        coVerify(atLeast = 1) { sessionRepo.saveSession(any(), any()) }
    }

    // --- Privacy tests ---

    @Test
    fun `onTogglePrivacy_whenNoteLoaded_setsIsPrivateTrueAndCallsUseCase`() = runTest {
        val noteId = "123"
        coEvery { repo.getNote(noteId) } returns flowOf(Result.Success(Note(noteId, "T", "C", 0L, 0L, isPrivate = false)))

        viewModel = NoteEditorViewModel(NoteEditor(noteId), upsertNote, deleteNote, repo, sessionRepo, toggleNotePrivacy)
        runCurrent()

        viewModel.onTogglePrivacy()
        runCurrent()

        assertTrue(viewModel.uiState.value.isPrivate)
        coVerify { toggleNotePrivacy(noteId, true) }
    }

    @Test
    fun `onTogglePrivacy_whenAlreadyPrivate_setsIsPrivateFalseAndCallsUseCase`() = runTest {
        val noteId = "123"
        coEvery { repo.getNote(noteId) } returns flowOf(Result.Success(Note(noteId, "T", "C", 0L, 0L, isPrivate = true)))

        viewModel = NoteEditorViewModel(NoteEditor(noteId), upsertNote, deleteNote, repo, sessionRepo, toggleNotePrivacy)
        runCurrent()

        viewModel.onTogglePrivacy()
        runCurrent()

        assertFalse(viewModel.uiState.value.isPrivate)
        coVerify { toggleNotePrivacy(noteId, false) }
    }

    @Test
    fun `onTogglePrivacy_whenNoNoteId_doesNothing`() = runTest {
        viewModel = NoteEditorViewModel(NoteEditor(null), upsertNote, deleteNote, repo, sessionRepo, toggleNotePrivacy)

        viewModel.onTogglePrivacy()
        runCurrent()

        assertFalse(viewModel.uiState.value.isPrivate)
        coVerify(exactly = 0) { toggleNotePrivacy(any(), any()) }
    }

    @Test
    fun `init_withNavKey_loadsNoteIsPrivate`() = runTest {
        val noteId = "123"
        coEvery { repo.getNote(noteId) } returns flowOf(Result.Success(Note(noteId, "T", "C", 0L, 0L, isPrivate = true)))

        viewModel = NoteEditorViewModel(NoteEditor(noteId), upsertNote, deleteNote, repo, sessionRepo, toggleNotePrivacy)
        runCurrent()

        assertTrue(viewModel.uiState.value.isPrivate)
    }

    @Test
    fun `onNoteSelected_opensTabWithIsPrivate`() = runTest {
        viewModel = NoteEditorViewModel(NoteEditor(null), upsertNote, deleteNote, repo, sessionRepo, toggleNotePrivacy)

        val note = Note("n1", "Title", "Body", 0L, 0L, isPrivate = true)
        viewModel.onNoteSelected(note)
        runCurrent()

        assertTrue(viewModel.uiState.value.isPrivate)
    }

    @Test
    fun `init_withSession_restoresIsPrivate`() = runTest {
        coEvery { sessionRepo.getSession() } returns id.project.df.dnote.core.data.SessionData(
            noteIds = listOf("id1"),
            activeTabIndex = 0
        )
        coEvery { repo.getNote("id1") } returns flowOf(Result.Success(Note("id1", "T", "C", 0L, 0L, isPrivate = true)))

        viewModel = NoteEditorViewModel(NoteEditor(null), upsertNote, deleteNote, repo, sessionRepo, toggleNotePrivacy)
        runCurrent()

        assertTrue(viewModel.uiState.value.isPrivate)
    }
}
