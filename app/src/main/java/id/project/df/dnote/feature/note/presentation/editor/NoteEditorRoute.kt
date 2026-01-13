package id.project.df.dnote.feature.note.presentation.editor

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.project.df.dnote.core.ui.theme.DNoteTheme

@Composable
fun NoteEditorRoute(
    onCloseEditor: () -> Unit,
    viewModel: NoteEditorViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val events = viewModel.events.collectAsState(initial = null)

    LaunchedEffect(events.value) {
        when (val event = events.value) {
            is NoteEditorEvent.Close -> {
            }
            is NoteEditorEvent.ShowError -> {
                Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }

    EditorScreen(
        uiState = uiState,
        onTitleChange = { newText -> viewModel.onTitleChanged(newText) },
        onContentChange = { newText -> viewModel.onContentChanged(newText) },
        onSaveNote = { viewModel.onCloseRequested() },
        onCloseEditor = {
            viewModel.onCloseRequested()
            onCloseEditor.invoke()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    uiState: NoteEditorUiState,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onSaveNote: () -> Unit,
    onCloseEditor: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = uiState.title,
                        onValueChange = onTitleChange,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.headlineSmall,
                        placeholder = {
                            Text(
                                "Untitled Note",
                                style = MaterialTheme.typography.headlineSmall
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next
                        )
                    )
                },
                actions = {
                    IconButton(
                        onClick = onSaveNote,
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Save note"
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCloseEditor) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close editor"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        TextField(
            value = uiState.contentText,
            onValueChange = onContentChange,
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
            placeholder = { Text("Start writing your note...") },
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EditorScreenPreview() {
    DNoteTheme {
        EditorScreen(
            uiState = NoteEditorUiState(
                title = "",
                contentText = "",
                isSaving = false,
                errorMessage = null
            ),
            onContentChange = {},
            onSaveNote = {},
            onCloseEditor = {},
            onTitleChange = {}
        )
    }
}
