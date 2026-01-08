package id.project.df.dnote.feature.note.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.project.df.dnote.feature.note.domain.usecase.DeleteNoteUseCase
import id.project.df.dnote.feature.note.domain.usecase.ObserveNotesUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class NotesListViewModel @Inject constructor(
    private val observeNotes: ObserveNotesUseCase,
    private val deleteNote: DeleteNoteUseCase
) : ViewModel() {

    private val queryFlow = MutableStateFlow("")

    private val _uiState = MutableStateFlow(NotesListUiState(isLoading = true))
    val uiState: StateFlow<NotesListUiState> = _uiState.asStateFlow()

    init {
        queryFlow
            .debounce(150)
            .distinctUntilChanged()
            .flatMapLatest { q ->
                flow {
                    emit(NotesListPartial.Loading(q))

                    emitAll(
                        observeNotes(q).map { notes ->
                            NotesListPartial.Data(
                                q = q,
                                items = notes.map {
                                    NoteListItemUI(
                                        id = it.id,
                                        preview = it.toPreview(),
                                        updatedAt = it.updatedAt
                                    )
                                }
                            )
                        }
                    )
                }
                    .catch { t ->
                        emit(NotesListPartial.Error(q, t.message ?: "Error"))
                    }
            }
            .onEach { partial ->
                _uiState.update { current ->
                    when (partial) {
                        is NotesListPartial.Loading -> current.copy(
                            query = partial.q,
                            isLoading = true,
                            errorMessage = null
                        )
                        is NotesListPartial.Data -> current.copy(
                            query = partial.q,
                            items = partial.items,
                            isLoading = false,
                            errorMessage = null
                        )
                        is NotesListPartial.Error -> current.copy(
                            query = partial.q,
                            isLoading = false,
                            errorMessage = partial.message
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun onQueryChanged(q: String) {
        queryFlow.value = q
    }

    fun onDeleteClicked(noteId: String) {
        viewModelScope.launch { deleteNote(noteId) }
    }

    private sealed interface NotesListPartial {
        data class Loading(val q: String) : NotesListPartial
        data class Data(val q: String, val items: List<NoteListItemUI>) : NotesListPartial
        data class Error(val q: String, val message: String) : NotesListPartial
    }
}