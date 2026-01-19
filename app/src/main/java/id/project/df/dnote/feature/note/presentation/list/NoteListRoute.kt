package id.project.df.dnote.feature.note.presentation.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.project.df.dnote.core.common.util.formatDate

@Composable
fun NoteListRoute(
    viewModel: NotesListViewModel,
    onNoteClick: (noteId: String) -> Unit,
    onCreateNew: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NotesListScreen(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChanged,
        onNoteClick = onNoteClick,
        onCreateNew = onCreateNew,
        onDeleteClick = viewModel::onDeleteClicked,
        onRetry = { viewModel.onQueryChanged(uiState.query) }
    )
}

@Composable
fun NotesListScreen(
    uiState: NotesListUiState,
    onQueryChange: (String) -> Unit,
    onNoteClick: (String) -> Unit,
    onCreateNew: () -> Unit,
    onDeleteClick: (String) -> Unit,
    onRetry: () -> Unit,
) {
    var pendingDeleteId by rememberSaveable { mutableStateOf<String?>(null) }

    val pendingDeleteTitle = remember(pendingDeleteId, uiState.items) {
        uiState.items
            .firstOrNull { it.id == pendingDeleteId }
            ?.title
            .orEmpty()
    }

    Scaffold(
        topBar = {
            Text(
                text = "Notes",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                placeholder = { Text("Search notes...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                }
            )

            NotesListContent(
                modifier = Modifier.weight(1f),
                isLoading = uiState.isLoading,
                errorMessage = uiState.errorMessage,
                items = uiState.items,
                onNoteClick = onNoteClick,
                onDeleteClick = { noteId ->
                    pendingDeleteId = noteId
                },
                onRetry = onRetry
            )

            FloatingActionButton(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.End),
                onClick = {
                    onCreateNew.invoke()
                },
                shape = MaterialTheme.shapes.medium,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "add new note"
                )
            }
        }

        if (pendingDeleteId != null) {
            DeleteConfirmationDialog(
                title = pendingDeleteTitle,
                onConfirm = {
                    onDeleteClick(pendingDeleteId!!)
                    pendingDeleteId = null
                },
                onDismiss = {
                    pendingDeleteId = null
                }
            )
        }
    }
}


@Composable
private fun NotesTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Notes",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Search") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
        )
    }
}

@Composable
private fun NotesListContent(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    errorMessage: String?,
    items: List<NoteListItemUI>,
    onNoteClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onRetry: () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            errorMessage != null -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = errorMessage, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onRetry) { Text("Retry") }
                }
            }

            items.isEmpty() -> {
                Text(
                    text = "No notes found",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                        items = items,
                        key = { it.id }
                    ) { note ->
                        NoteRow(
                            item = note,
                            onClick = { onNoteClick(note.id) },
                            onDeleteClick = { onDeleteClick(note.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteRow(
    item: NoteListItemUI,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val updatedText = remember(item.updatedAt) { item.updatedAt?.formatDate() }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title.ifBlank { "(Untitled)" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = item.preview,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Updated: $updatedText",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete"
                )
            }
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    title: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete note?") },
        text = {
            Text(
                text = if (title.isBlank()) {
                    "Delete this note? This action can’t be undone."
                } else {
                    "Delete “$title”? This action can’t be undone."
                }
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}



@Preview(showBackground = true)
@Composable
fun NotesListScreenPreview_Positive() {
    MaterialTheme {
        NotesListScreen(
            uiState = NotesListUiState(
                query = "",
                isLoading = false,
                errorMessage = null,
                items = listOf(
                    NoteListItemUI(
                        id = "1",
                        title = "Meeting Notes",
                        preview = "Discuss project timeline and milestones...",
                        updatedAt = System.currentTimeMillis() - 60_000
                    ),
                    NoteListItemUI(
                        id = "2",
                        title = "Shopping List",
                        preview = "Milk, Eggs, Bread, Coffee Beans",
                        updatedAt = System.currentTimeMillis() - 3_600_000
                    )
                )
            ),
            onQueryChange = {},
            onNoteClick = {},
            onCreateNew = {},
            onDeleteClick = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NotesListScreenPreview_Loading() {
    MaterialTheme {
        NotesListScreen(
            uiState = NotesListUiState(
                query = "",
                isLoading = true,
                errorMessage = null,
                items = emptyList()
            ),
            onQueryChange = {},
            onNoteClick = {},
            onCreateNew = {},
            onDeleteClick = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NotesListScreenPreview_Error() {
    MaterialTheme {
        NotesListScreen(
            uiState = NotesListUiState(
                query = "meeting",
                isLoading = false,
                errorMessage = "Something went wrong",
                items = emptyList()
            ),
            onQueryChange = {},
            onNoteClick = {},
            onCreateNew = {},
            onDeleteClick = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NotesListScreenPreview_Empty() {
    MaterialTheme {
        NotesListScreen(
            uiState = NotesListUiState(
                query = "abc",
                isLoading = false,
                errorMessage = null,
                items = emptyList()
            ),
            onQueryChange = {},
            onNoteClick = {},
            onCreateNew = {},
            onDeleteClick = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DeleteConfirmationDialogPreview_WithTitle() {
    MaterialTheme {
        DeleteConfirmationDialog(
            title = "Meeting Notes",
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DeleteConfirmationDialogPreview_EmptyTitle() {
    MaterialTheme {
        DeleteConfirmationDialog(
            title = "",
            onConfirm = {},
            onDismiss = {}
        )
    }
}










