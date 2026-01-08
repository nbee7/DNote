package id.project.df.dnote.feature.note.presentation.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.project.df.dnote.feature.note.domain.usecase.UpsertNoteUseCase
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
import javax.inject.Inject

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    private val upsertNote: UpsertNoteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteEditorUiState())
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    private val _events = Channel<NoteEditorEvent>(capacity = Channel.BUFFERED)
    val events: Flow<NoteEditorEvent> = _events.receiveAsFlow()

    private var autosaveJob: Job? = null

    fun loadExisting(noteId: String, initialText: String) {
        _uiState.value = NoteEditorUiState(noteId = noteId, text = initialText)
    }

    fun onTextChanged(newText: String) {
        _uiState.update { it.copy(text = newText, errorMessage = null) }
        scheduleAutosave()
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

    private suspend fun saveInternal(flush: Boolean): Boolean {
        val current = _uiState.value

        _uiState.update { it.copy(isSaving = flush) }

        return runCatching {
            upsertNote(current.noteId, current.text)
        }.onSuccess { newIdOrNull ->
            if (current.noteId == null && newIdOrNull != null) {
                _uiState.update { it.copy(noteId = newIdOrNull) }
            }
            _uiState.update { it.copy(isSaving = false) }
        }.onFailure { t ->
            _uiState.update { it.copy(isSaving = false, errorMessage = t.message ?: "Error") }
            _events.send(NoteEditorEvent.ShowError(t.message ?: "Error"))
        }.isSuccess
    }
}