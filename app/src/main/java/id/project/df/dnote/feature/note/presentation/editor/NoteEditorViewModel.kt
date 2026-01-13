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
import kotlinx.coroutines.Dispatchers
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

@HiltViewModel(assistedFactory = NoteEditorViewModel.Factory::class)
class NoteEditorViewModel @AssistedInject constructor(
    @Assisted private val navKey: NoteEditor,
    @Assisted private val upsertNote: UpsertNoteUseCase,
    @Assisted private val repo: NoteRepositoryInterface
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteEditorUiState())
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    private val _events = Channel<NoteEditorEvent>(capacity = Channel.BUFFERED)
    val events: Flow<NoteEditorEvent> = _events.receiveAsFlow()

    private var autosaveJob: Job? = null

    init {
        if (navKey.id != null) {
            viewModelScope.launch(Dispatchers.IO) {
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
        _uiState.value = NoteEditorUiState(noteId = noteId, title = title, contentText = initialText, errorMessage = errorMessage)
    }

    fun onContentChanged(newText: String) {
        _uiState.update { it.copy(contentText = newText, errorMessage = null) }
        scheduleAutosave()
    }

    fun onTitleChanged(newText: String) {
        _uiState.update { it.copy(title = newText, errorMessage = null) }
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
            upsertNote(current.noteId, current.title, current.contentText)
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

    @AssistedFactory
    interface Factory {
        fun create(
            navKey: NoteEditor,
            upsertNote: UpsertNoteUseCase,
            repo: NoteRepositoryInterface
        ) : NoteEditorViewModel
    }
}