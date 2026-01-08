package id.project.df.dnote.feature.note.presentation.editor

data class NoteEditorUiState(
    val noteId: String? = null,
    val text: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

sealed interface NoteEditorEvent {
    object Close : NoteEditorEvent
    data class ShowError(val message: String) : NoteEditorEvent
}
