package id.project.df.dnote.feature.note.presentation.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import id.project.df.dnote.feature.note.data.repository.Result
import id.project.df.dnote.feature.note.di.NoteEditor
import id.project.df.dnote.feature.note.domain.repository.NoteRepositoryInterface
import id.project.df.dnote.feature.note.domain.usecase.UpsertNoteUseCase
import id.project.df.dnote.feature.note.domain.usecase.DeleteNoteUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.ArrayDeque
import id.project.df.dnote.feature.note.domain.model.Note

@HiltViewModel(assistedFactory = NoteEditorViewModel.Factory::class)
class NoteEditorViewModel @AssistedInject constructor(
    @Assisted private val navKey: NoteEditor,
    @Assisted private val upsertNote: UpsertNoteUseCase,
    @Assisted private val deleteNote: DeleteNoteUseCase,
    @Assisted private val repo: NoteRepositoryInterface
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
                        is Result.Success -> loadExisting(navKey.id, result.data.title, result.data.content, null)
                        is Result.Error -> loadExisting(navKey.id,  errorMessage = result.exception.message.toString())
                    }
                }
            }
        }
    }

    fun loadExisting(noteId: String, title: String = "", initialText: String = "", errorMessage: String? = null) {
        _uiState.update {
            it.copy(
                noteId = noteId,
                title = title,
                contentText = initialText,
                isSaving = false,
                errorMessage = errorMessage,
                canUndo = false,
                canRedo = false
            )
        }
        undoStack.clear()
        redoStack.clear()
        lastStableState = EditorSnapshot(title, initialText)
        updateUndoRedoState()
    }

    fun onNoteSelected(note: Note) {
        viewModelScope.launch {
            saveInternal(flush = true)
            loadExisting(note.id, note.title, note.content)
        }
    }

    fun onContentChanged(newText: String) {
        if (redoStack.isNotEmpty()) {
            redoStack.clear()
        }
        _uiState.update { 
            val updated = it.copy(contentText = newText, errorMessage = null)
            val currentState = EditorSnapshot(updated.title, updated.contentText)
            val hasPending = currentState != lastStableState
            updated.copy(
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
        _uiState.update { 
            val updated = it.copy(title = newText, errorMessage = null)
            val currentState = EditorSnapshot(updated.title, updated.contentText)
            val hasPending = currentState != lastStableState
            updated.copy(
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
        _uiState.update { 
            it.copy(title = snapshot.title, contentText = snapshot.content) 
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

    private fun scheduleAutosave() {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(400)
            saveInternal(flush = false)
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
                if (latest.title.isBlank() && latest.contentText.isBlank()) {
                     if (latest.noteId != null) {
                         deleteNote(latest.noteId)
                     }
                     null
                } else {
                    upsertNote(
                        latest.noteId,
                        latest.title,
                        latest.contentText
                    )
                }
            }.onSuccess { newIdOrNull ->
                if (_uiState.value.noteId == null && newIdOrNull != null) {
                    _uiState.update { it.copy(noteId = newIdOrNull) }
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
    interface Factory {
        fun create(
            navKey: NoteEditor,
            upsertNote: UpsertNoteUseCase,
            deleteNote: DeleteNoteUseCase,
            repo: NoteRepositoryInterface
        ) : NoteEditorViewModel
    }
}