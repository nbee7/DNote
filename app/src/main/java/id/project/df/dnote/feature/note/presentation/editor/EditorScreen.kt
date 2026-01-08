package id.project.df.dnote.feature.note.presentation.editor

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.project.df.dnote.ui.theme.DNoteTheme

@Composable
fun NoteEditorScreen(viewModel: NoteEditorViewModel) {
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
        onTextChange = { newText -> viewModel.onTextChanged(newText) },
        onSaveNote = { viewModel.onCloseRequested() },
        onCloseEditor = { viewModel.onCloseRequested() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    uiState: NoteEditorUiState,
    onTextChange: (String) -> Unit,
    onSaveNote: () -> Unit,
    onCloseEditor: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Note Editor") },
                actions = {
                    IconButton(onClick = onSaveNote) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        } else {
                            Icon(imageVector = Icons.Default.Save, contentDescription = "Save")
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCloseEditor) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                BasicTextField(
                    value = uiState.text,
                    onValueChange = onTextChange,
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, Color.Gray, shape = RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.Black),
                    cursorBrush = SolidColor(Color.Black)
                )
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun EditorScreenPreview() {
    DNoteTheme {
        EditorScreen(
            uiState = NoteEditorUiState(
                text = "This is the note content.",
                isSaving = false,
                errorMessage = null
            ),
            onTextChange = {},
            onSaveNote = {},
            onCloseEditor = {}
        )
    }
}
