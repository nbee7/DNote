package id.project.df.dnote.feature.note.presentation.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import id.project.df.dnote.core.data.SessionRepository
import id.project.df.dnote.feature.note.data.repository.Result
import id.project.df.dnote.feature.note.di.NoteEditor
import id.project.df.dnote.feature.note.domain.model.Note
import id.project.df.dnote.feature.note.domain.repository.NoteRepositoryInterface
import id.project.df.dnote.feature.note.domain.usecase.DeleteNoteUseCase
import id.project.df.dnote.feature.note.domain.usecase.ToggleNotePrivacyUseCase
import id.project.df.dnote.feature.note.domain.usecase.UpsertNoteUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.ArrayDeque

@HiltViewModel(assistedFactory = NoteEditorViewModel.Factory::class)
class NoteEditorViewModel @AssistedInject constructor(
    @Assisted private val navKey: NoteEditor,
    @Assisted private val upsertNote: UpsertNoteUseCase,
    @Assisted private val deleteNote: DeleteNoteUseCase,
    @Assisted private val repo: NoteRepositoryInterface,
    private val sessionRepository: SessionRepository,
    private val toggleNotePrivacy: ToggleNotePrivacyUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteEditorUiState())
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    private val _events = Channel<NoteEditorEvent>(capacity = Channel.BUFFERED)
    val events: Flow<NoteEditorEvent> = _events.receiveAsFlow()

    private var autosaveJob: Job? = null
    private var undoDebounceJob: Job? = null

    private val saveMutex = Mutex()

    private data class EditorSnapshot(val title: String, val content: String)
    private val undoStack = ArrayDeque<EditorSnapshot>()
    private val redoStack = ArrayDeque<EditorSnapshot>()
    private var lastStableState = EditorSnapshot("", "")

    init {
        viewModelScope.launch {
            repo.observeNotes("").collect { result ->
                if (result is Result.Success) {
                    _uiState.update { it.copy(notes = result.data) }
                }
            }
        }

        if (navKey.id != null) {
            viewModelScope.launch {
                repo.getNote(navKey.id).collect { result ->
                    when (result) {
                        is Result.Success -> loadExisting(navKey.id, result.data.title, result.data.content, isPrivate = result.data.isPrivate)
                        is Result.Error -> loadExisting(navKey.id, errorMessage = result.exception.message.toString())
                    }
                }
            }
        } else {
            viewModelScope.launch {
                restoreSession()
            }
        }
    }

    private suspend fun restoreSession() {
        val session = sessionRepository.getSession() ?: return
        val tabs = mutableListOf<TabState>()
        for (noteId in session.noteIds) {
            val result = repo.getNote(noteId).first()
            if (result is Result.Success) {
                tabs.add(TabState(noteId = noteId, title = result.data.title, contentText = result.data.content, isPrivate = result.data.isPrivate))
            }
        }
        if (tabs.isEmpty()) return
        val activeIndex = session.activeTabIndex.coerceIn(0, tabs.lastIndex)
        _uiState.update { state ->
            state.copy(tabs = tabs, activeTabIndex = activeIndex, canUndo = false, canRedo = false)
        }
        resetUndoRedo()
    }
    fun onNewTab() {
        viewModelScope.launch {
            saveInternal(flush = true)
            resetUndoRedo()
            _uiState.update { state ->
                val newTabs = state.tabs + TabState()
                state.copy(
                    tabs = newTabs,
                    activeTabIndex = newTabs.lastIndex,
                    showTabGrid = false,
                    canUndo = false,
                    canRedo = false
                )
            }
            persistSession()
        }
    }

    fun onSwitchTab(index: Int) {
        val state = _uiState.value
        if (index == state.activeTabIndex || index !in state.tabs.indices) return
        viewModelScope.launch {
            saveInternal(flush = true)
            resetUndoRedo()
            _uiState.update {
                it.copy(activeTabIndex = index, showTabGrid = false, canUndo = false, canRedo = false)
            }
            persistSession()
        }
    }

    fun onCloseTab(index: Int) {
        val state = _uiState.value
        if (index !in state.tabs.indices) return
        viewModelScope.launch {
            val tab = state.tabs[index]
            if (tab.noteId != null && (tab.title.isNotBlank() || tab.contentText.isNotBlank())) {
                runCatching { upsertNote(tab.noteId, tab.title, tab.contentText) }
            }

            _uiState.update { s ->
                val newTabs = s.tabs.toMutableList().apply { removeAt(index) }
                if (newTabs.isEmpty()) {
                    s.copy(tabs = listOf(TabState()), activeTabIndex = 0, canUndo = false, canRedo = false)
                } else {
                    val newActiveIndex = when {
                        index < s.activeTabIndex -> s.activeTabIndex - 1
                        index == s.activeTabIndex -> index.coerceAtMost(newTabs.lastIndex)
                        else -> s.activeTabIndex
                    }
                    s.copy(tabs = newTabs, activeTabIndex = newActiveIndex, canUndo = false, canRedo = false)
                }
            }
            resetUndoRedo()
            persistSession()
        }
    }

    fun onTabsClick() {
        _uiState.update { it.copy(showTabGrid = !it.showTabGrid) }
    }

    fun onDismissTabGrid() {
        _uiState.update { it.copy(showTabGrid = false) }
    }

    fun onNoteSelected(note: Note) {
        val state = _uiState.value
        val existingIndex = state.tabs.indexOfFirst { it.noteId == note.id }
        if (existingIndex != -1) {
            onSwitchTab(existingIndex)
            return
        }
        viewModelScope.launch {
            saveInternal(flush = true)
            resetUndoRedo()
            _uiState.update { s ->
                val newTab = TabState(noteId = note.id, title = note.title, contentText = note.content, isPrivate = note.isPrivate)
                val newTabs = s.tabs + newTab
                s.copy(
                    tabs = newTabs,
                    activeTabIndex = newTabs.lastIndex,
                    showTabGrid = false,
                    canUndo = false,
                    canRedo = false
                )
            }
            persistSession()
        }
    }

    // --- Editor operations (operate on active tab) ---

    fun loadExisting(noteId: String, title: String = "", initialText: String = "", errorMessage: String? = null, isPrivate: Boolean = false) {
        _uiState.update { state ->
            val updatedTab = state.activeTab.copy(noteId = noteId, title = title, contentText = initialText, isPrivate = isPrivate)
            val updatedTabs = state.tabs.toMutableList().apply { set(state.activeTabIndex, updatedTab) }
            state.copy(
                tabs = updatedTabs,
                isSaving = false,
                errorMessage = errorMessage,
                canUndo = false,
                canRedo = false
            )
        }
        resetUndoRedo()
        lastStableState = EditorSnapshot(title, initialText)
    }

    fun onContentChanged(newText: String) {
        if (redoStack.isNotEmpty()) {
            redoStack.clear()
        }
        _uiState.update { state ->
            val updatedTab = state.activeTab.copy(contentText = newText)
            val updatedTabs = state.tabs.toMutableList().apply { set(state.activeTabIndex, updatedTab) }
            val currentSnapshot = EditorSnapshot(updatedTab.title, updatedTab.contentText)
            val hasPending = currentSnapshot != lastStableState
            state.copy(
                tabs = updatedTabs,
                errorMessage = null,
                canUndo = undoStack.isNotEmpty() || hasPending,
                canRedo = redoStack.isNotEmpty()
            )
        }
        scheduleAutosave()
        scheduleUndoSnapshot()
    }

    fun onTitleChanged(newText: String) {
        if (redoStack.isNotEmpty()) {
            redoStack.clear()
        }
        _uiState.update { state ->
            val updatedTab = state.activeTab.copy(title = newText)
            val updatedTabs = state.tabs.toMutableList().apply { set(state.activeTabIndex, updatedTab) }
            val currentSnapshot = EditorSnapshot(updatedTab.title, updatedTab.contentText)
            val hasPending = currentSnapshot != lastStableState
            state.copy(
                tabs = updatedTabs,
                errorMessage = null,
                canUndo = undoStack.isNotEmpty() || hasPending,
                canRedo = redoStack.isNotEmpty()
            )
        }
        scheduleAutosave()
        scheduleUndoSnapshot()
    }

    fun onUndo() {
        undoDebounceJob?.cancel()
        val currentUi = _uiState.value
        val currentState = EditorSnapshot(currentUi.title, currentUi.contentText)

        if (currentState != lastStableState) {
            redoStack.addLast(currentState)
            applySnapshot(lastStableState)
            return
        }

        if (undoStack.isNotEmpty()) {
            val prev = undoStack.removeLast()
            redoStack.addLast(lastStableState)
            lastStableState = prev
            applySnapshot(prev)
        }
    }

    fun onRedo() {
        undoDebounceJob?.cancel()
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeLast()
            undoStack.addLast(lastStableState)
            lastStableState = next
            applySnapshot(next)
        }
    }

    private fun applySnapshot(snapshot: EditorSnapshot) {
        _uiState.update { state ->
            val updatedTab = state.activeTab.copy(title = snapshot.title, contentText = snapshot.content)
            val updatedTabs = state.tabs.toMutableList().apply { set(state.activeTabIndex, updatedTab) }
            state.copy(tabs = updatedTabs)
        }
        updateUndoRedoState()
        scheduleAutosave()
    }

    private fun scheduleUndoSnapshot() {
        undoDebounceJob?.cancel()
        undoDebounceJob = viewModelScope.launch {
            delay(600)
            commitToUndoStack()
        }
    }

    private fun commitToUndoStack() {
        val currentUi = _uiState.value
        val currentState = EditorSnapshot(currentUi.title, currentUi.contentText)
        if (currentState != lastStableState) {
            undoStack.addLast(lastStableState)
            lastStableState = currentState
            updateUndoRedoState()
        }
    }

    private fun updateUndoRedoState() {
        val currentUi = _uiState.value
        val currentState = EditorSnapshot(currentUi.title, currentUi.contentText)
        val hasPending = currentState != lastStableState

        _uiState.update {
            it.copy(
                canUndo = undoStack.isNotEmpty() || hasPending,
                canRedo = redoStack.isNotEmpty()
            )
        }
    }

    private fun resetUndoRedo() {
        undoDebounceJob?.cancel()
        autosaveJob?.cancel()
        undoStack.clear()
        redoStack.clear()
        val current = _uiState.value
        lastStableState = EditorSnapshot(current.title, current.contentText)
    }

    private fun persistSession() {
        viewModelScope.launch {
            val state = _uiState.value
            val noteIds = state.tabs.mapNotNull { it.noteId }
            if (noteIds.isEmpty()) {
                sessionRepository.clearSession()
            } else {
                sessionRepository.saveSession(noteIds, state.activeTabIndex)
            }
        }
    }

    private fun scheduleAutosave() {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(400)
            saveInternal(flush = false)
        }
    }

    fun onTogglePrivacy() {
        val tab = _uiState.value.activeTab
        val noteId = tab.noteId ?: return
        val newValue = !tab.isPrivate
        _uiState.update { s ->
            val tabs = s.tabs.toMutableList()
            tabs[s.activeTabIndex] = tabs[s.activeTabIndex].copy(isPrivate = newValue)
            s.copy(tabs = tabs)
        }
        viewModelScope.launch {
            toggleNotePrivacy(noteId, newValue)
        }
    }

    fun onCloseRequested() {
        viewModelScope.launch {
            autosaveJob?.cancel()
            val ok = saveInternal(flush = true)
            if (ok) _events.send(NoteEditorEvent.Close)
        }
    }

    private suspend fun saveInternal(flush: Boolean): Boolean =
        saveMutex.withLock {
            _uiState.update { it.copy(isSaving = flush) }

            runCatching {
                val latest = _uiState.value
                val currentNoteId = latest.noteId
                if (latest.title.isBlank() && latest.contentText.isBlank()) {
                    if (currentNoteId != null) {
                        deleteNote(currentNoteId)
                    }
                    null
                } else {
                    upsertNote(
                        currentNoteId,
                        latest.title,
                        latest.contentText
                    )
                }
            }.onSuccess { newIdOrNull ->
                if (_uiState.value.noteId == null && newIdOrNull != null) {
                    _uiState.update { state ->
                        val updatedTab = state.activeTab.copy(noteId = newIdOrNull)
                        val updatedTabs = state.tabs.toMutableList().apply { set(state.activeTabIndex, updatedTab) }
                        state.copy(tabs = updatedTabs)
                    }
                    persistSession()
                }
                _uiState.update { it.copy(isSaving = false) }
            }.onFailure { t ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = t.message ?: "Error"
                    )
                }
                _events.send(NoteEditorEvent.ShowError(t.message ?: "Error"))
            }.isSuccess
        }

    @AssistedFactory
    fun interface Factory {
        fun create(
            navKey: NoteEditor,
            upsertNote: UpsertNoteUseCase,
            deleteNote: DeleteNoteUseCase,
            repo: NoteRepositoryInterface
        ): NoteEditorViewModel
    }
}
