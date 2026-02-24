package id.project.df.dnote.feature.note.presentation.editor

import id.project.df.dnote.feature.note.domain.model.Note

data class NoteEditorUiState(
    val noteId: String? = null,
    val title: String = "",
    val contentText: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val notes: List<Note> = emptyList()
)

sealed interface NoteEditorEvent {
    object Close : NoteEditorEvent
    data class ShowError(val message: String) : NoteEditorEvent
}
