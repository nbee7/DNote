package id.project.df.dnote.feature.note.presentation.editor

import id.project.df.dnote.feature.note.domain.model.Note

data class TabState(
    val noteId: String? = null,
    val title: String = "",
    val contentText: String = ""
)

data class NoteEditorUiState(
    val tabs: List<TabState> = listOf(TabState()),
    val activeTabIndex: Int = 0,
    val showTabGrid: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val notes: List<Note> = emptyList()
) {
    val activeTab: TabState get() = tabs[activeTabIndex]
    val noteId: String? get() = activeTab.noteId
    val title: String get() = activeTab.title
    val contentText: String get() = activeTab.contentText
    val tabCount: Int get() = tabs.size
}

sealed interface NoteEditorEvent {
    object Close : NoteEditorEvent
    data class ShowError(val message: String) : NoteEditorEvent
}
