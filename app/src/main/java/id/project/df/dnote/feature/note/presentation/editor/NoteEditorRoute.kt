package id.project.df.dnote.feature.note.presentation.editor

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.project.df.dnote.R
import id.project.df.dnote.core.ui.markdown.MarkdownFormatter
import id.project.df.dnote.core.ui.markdown.MarkdownVisualTransformation
import id.project.df.dnote.core.ui.markdown.rules.BoldRule
import id.project.df.dnote.core.ui.markdown.rules.HeaderRule
import id.project.df.dnote.core.ui.markdown.rules.ItalicRule
import id.project.df.dnote.core.ui.theme.DNoteTheme
import id.project.df.dnote.feature.note.domain.model.Note
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatNoteDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun DrawerNoteItem(
    note: Note,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val hasTitle = note.title.isNotBlank()
    val dateText = formatNoteDate(note.updatedAt)

    NavigationDrawerItem(
        label = {
            if (hasTitle) {
                Column {
                    Text(
                        text = note.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = dateText,
                        maxLines = 1,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = dateText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        selected = isSelected,
        onClick = onClick,
        modifier = Modifier.padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun NoteEditorRoute(
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
        onContentChange = { newValue -> viewModel.onContentChanged(newValue.text) },
        onSaveNote = { viewModel.onCloseRequested() },
        onUndo = { viewModel.onUndo() },
        onRedo = { viewModel.onRedo() },
        onNoteSelected = { note -> viewModel.onNoteSelected(note) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    uiState: NoteEditorUiState,
    onTitleChange: (String) -> Unit,
    onContentChange: (TextFieldValue) -> Unit,
    onSaveNote: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onNoteSelected: (Note) -> Unit
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val textFieldValueState = remember { mutableStateOf(TextFieldValue(uiState.contentText)) }

    LaunchedEffect(uiState.contentText) {
        if (uiState.contentText != textFieldValueState.value.text) {
             textFieldValueState.value = textFieldValueState.value.copy(text = uiState.contentText)
        }
    }

    val markdownTransformation = remember(textFieldValueState.value.selection) {
        MarkdownVisualTransformation(
            listOf(
                BoldRule(),
                ItalicRule(),
                HeaderRule()
            ),
            cursorPosition = textFieldValueState.value.selection.start
        )
    }

    val markdownFormatter = remember { MarkdownFormatter() }
    @OptIn(ExperimentalLayoutApi::class)
    val isKeyboardVisible = WindowInsets.isImeVisible

    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "All Notes",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.headlineSmall
                )
                HorizontalDivider()

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                ) {
                    items(uiState.notes, key = { it.id }) { note ->
                        DrawerNoteItem(
                            note = note,
                            isSelected = uiState.noteId == note.id,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                }
                                onNoteSelected(note)
                            }
                        )
                    }
                    item {
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        },
        drawerState = drawerState
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
                                autoCorrect = false,
                                imeAction = ImeAction.Next
                            )
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = onUndo,
                            enabled = uiState.canUndo
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo",
                                tint = if (uiState.canUndo) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f)
                            )
                        }
                        IconButton(
                            onClick = onRedo,
                            enabled = uiState.canRedo
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Redo,
                                contentDescription = "Redo",
                                tint = if (uiState.canRedo) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f)
                            )
                        }
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
                        IconButton(onClick = {
                            scope.launch {
                                if (drawerState.isClosed) {
                                    drawerState.open()
                                } else {
                                    drawerState.close()
                                }
                            }
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.outline_format_list_bulleted_24),
                                contentDescription = "Open List"
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (isKeyboardVisible) {
                    Column {
                        KeyboardAccessoryBar(
                            onBoldClick = {
                                val newValue =
                                    markdownFormatter.toggleStyle(textFieldValueState.value, BoldRule())
                                textFieldValueState.value = newValue
                                onContentChange(newValue)
                            },
                            onItalicClick = {
                                val newValue = markdownFormatter.toggleStyle(
                                    textFieldValueState.value,
                                    ItalicRule()
                                )
                                textFieldValueState.value = newValue
                                onContentChange(newValue)
                            },
                            onHeaderClick = {
                                val newValue = markdownFormatter.toggleCyclicHeading(
                                    textFieldValueState.value,
                                    HeaderRule()
                                )
                                textFieldValueState.value = newValue
                                onContentChange(newValue)
                            }
                        )
                        Spacer(modifier = Modifier.windowInsetsBottomHeight(
                            WindowInsets.ime.exclude(WindowInsets.navigationBars)
                        ))
                    }
                } else {
                    BrowserBottomBar(
                        onSearch = {},
                        onNewTab = {},
                        onTabsClick = {},
                        onMenuClick = {}
                    )
                }
            }
        ) { paddingValues ->
            TextField(
                value = textFieldValueState.value,
                onValueChange = { newValue ->
                    var processedValue = newValue
                    
                    val oldText = textFieldValueState.value.text
                    val newText = newValue.text
                    
                    if (newText.length > oldText.length) {
                        val addedCharIndex = newValue.selection.start - 1
                        if (addedCharIndex >= 0 && addedCharIndex < newText.length) {
                             
                             val rules = listOf(BoldRule(), ItalicRule(), HeaderRule())
                             var justCompletedMatch = false
                             
                             for (rule in rules) {
                                 val matches = rule.pattern.findAll(newText)
                                 for (match in matches) {
                                     if (match.range.last + 1 == newValue.selection.start) {
                                         val (_, endLen) = rule.getDelimiterLengths(match)
                                         if (endLen > 0) {
                                             justCompletedMatch = true
                                             break
                                         }
                                     }
                                 }
                                 if (justCompletedMatch) break
                             }
                             
                             if (justCompletedMatch) {
                                 processedValue = newValue.copy(
                                     text = "$newText ",
                                     selection = TextRange(newValue.selection.start + 1)
                                 )
                             }
                        }
                    }

                    textFieldValueState.value = processedValue
                    onContentChange(processedValue)
                },
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(16.dp),
                textStyle = MaterialTheme.typography.bodyMedium,
                placeholder = { Text("Start writing your note...") },
                visualTransformation = markdownTransformation,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                )
            )
        }

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
            onTitleChange = {},
            onUndo = {},
            onRedo = {},
            onNoteSelected = {}
        )
    }
}

@Composable
fun KeyboardAccessoryBar(
    onBoldClick: () -> Unit,
    onItalicClick: () -> Unit,
    onHeaderClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBoldClick) {
                Icon(Icons.Default.FormatBold, contentDescription = "Bold")
            }
            IconButton(onClick = onItalicClick) {
                Icon(Icons.Default.FormatItalic, contentDescription = "Italic")
            }
            IconButton(onClick = onHeaderClick) {
                Icon(Icons.Default.Title, contentDescription = "Heading")
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun KeyboardAccessoryBarPreview() {
    DNoteTheme {
        KeyboardAccessoryBar(
            onBoldClick = {},
            onItalicClick = {},
            onHeaderClick = {}
        )
    }
}

@Composable
fun BrowserBottomBar(
    onSearch: () -> Unit,
    onNewTab: () -> Unit,
    onTabsClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(4.dp, RoundedCornerShape(32.dp)),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 3.dp
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onSearch) { Icon(Icons.Default.Search, "Search") }
                IconButton(onClick = onNewTab) { Icon(Icons.Default.Add, "New Tab") }
                IconButton(onClick = onTabsClick) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .border(
                                2.dp,
                                MaterialTheme.colorScheme.onSurface,
                                RoundedCornerShape(4.dp)
                            )
                            .size(24.dp)
                    ) {
                        Text("1", style = MaterialTheme.typography.labelSmall)
                    }
                }
                IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, "Menu") }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BrowserBottomBarPreview() {
    DNoteTheme {
        BrowserBottomBar(
            onSearch = {},
            onNewTab = {},
            onTabsClick = {},
            onMenuClick = {}
        )
    }
}
