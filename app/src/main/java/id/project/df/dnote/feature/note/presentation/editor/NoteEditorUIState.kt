package id.project.df.dnote.feature.note.presentation.editor

data class NoteEditorUiState(
    val noteId: String? = null,
    val title: String = "",
    val contentText: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

sealed interface NoteEditorEvent {
    object Close : NoteEditorEvent
    data class ShowError(val message: String) : NoteEditorEvent
}
