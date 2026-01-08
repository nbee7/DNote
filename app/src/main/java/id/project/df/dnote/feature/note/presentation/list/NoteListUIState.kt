package id.project.df.dnote.feature.note.presentation.list

data class NotesListUiState(
    val query: String = "",
    val items: List<NoteListItemUI> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
