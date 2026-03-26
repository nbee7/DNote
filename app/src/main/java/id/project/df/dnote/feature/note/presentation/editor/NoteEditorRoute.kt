package id.project.df.dnote.feature.note.presentation.editor

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.draw.blur
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerState
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import kotlinx.coroutines.CoroutineScope
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
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.blur(if (note.isPrivate) 8.dp else 0.dp)
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
        onNoteSelected = { note -> viewModel.onNoteSelected(note) },
        onNewTab = { viewModel.onNewTab() },
        onTabsClick = { viewModel.onTabsClick() },
        onSwitchTab = { index -> viewModel.onSwitchTab(index) },
        onCloseTab = { index -> viewModel.onCloseTab(index) },
        onDismissTabGrid = { viewModel.onDismissTabGrid() },
        onTogglePrivacy = { viewModel.onTogglePrivacy() }
    )
}

class EditorScreenState(
    initialContent: String,
    val drawerState: DrawerState,
    private val scope: CoroutineScope,
    val markdownFormatter: MarkdownFormatter
) {
    var textFieldValue by mutableStateOf(TextFieldValue(initialContent))
        private set

    fun onTextFieldValueChange(newValue: TextFieldValue) {
        textFieldValue = newValue
    }

    fun syncContent(newContent: String) {
        if (newContent != textFieldValue.text) {
            textFieldValue = textFieldValue.copy(text = newContent)
        }
    }

    fun closeDrawer() = scope.launch { drawerState.close() }

    fun toggleDrawer() = scope.launch {
        if (drawerState.isClosed) drawerState.open() else drawerState.close()
    }
}

@Composable
fun rememberEditorScreenState(initialContent: String): EditorScreenState {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val markdownFormatter = remember { MarkdownFormatter() }
    return remember { EditorScreenState(initialContent, drawerState, scope, markdownFormatter) }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    uiState: NoteEditorUiState,
    onTitleChange: (String) -> Unit,
    onContentChange: (TextFieldValue) -> Unit,
    onSaveNote: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onNoteSelected: (Note) -> Unit,
    onNewTab: () -> Unit,
    onTabsClick: () -> Unit,
    onSwitchTab: (Int) -> Unit,
    onCloseTab: (Int) -> Unit,
    onDismissTabGrid: () -> Unit,
    onTogglePrivacy: () -> Unit = {}
) {
    val state = rememberEditorScreenState(uiState.contentText)
    val isKeyboardVisible = WindowInsets.isImeVisible

    LaunchedEffect(uiState.contentText) {
        state.syncContent(uiState.contentText)
    }

    val markdownTransformation = remember(state.textFieldValue.selection) {
        MarkdownVisualTransformation(
            listOf(BoldRule(), ItalicRule(), HeaderRule()),
            cursorPosition = state.textFieldValue.selection.start
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            drawerContent = {
                EditorDrawerContent(
                    notes = uiState.notes,
                    currentNoteId = uiState.noteId,
                    onNoteSelected = { note ->
                        state.closeDrawer()
                        onNoteSelected(note)
                    },
                    onClose = { state.closeDrawer() }
                )
            },
            drawerState = state.drawerState
        ) {
            Scaffold(
                topBar = {
                    EditorTopBar(
                        title = uiState.title,
                        canUndo = uiState.canUndo,
                        canRedo = uiState.canRedo,
                        isSaving = uiState.isSaving,
                        isPrivate = uiState.isPrivate,
                        onTitleChange = onTitleChange,
                        onUndo = onUndo,
                        onRedo = onRedo,
                        onSaveNote = onSaveNote,
                        onTogglePrivacy = onTogglePrivacy,
                        onNavigationClick = { state.toggleDrawer() }
                    )
                },
                bottomBar = {
                    EditorBottomBar(
                        isKeyboardVisible = isKeyboardVisible,
                        tabCount = uiState.tabCount,
                        textFieldValue = state.textFieldValue,
                        markdownFormatter = state.markdownFormatter,
                        onContentChange = { newValue ->
                            state.onTextFieldValueChange(newValue)
                            onContentChange(newValue)
                        },
                        onNewTab = onNewTab,
                        onTabsClick = onTabsClick
                    )
                }
            ) { paddingValues ->
                EditorContent(
                    paddingValues = paddingValues,
                    isPrivate = uiState.isPrivate,
                    textFieldValue = state.textFieldValue,
                    markdownTransformation = markdownTransformation,
                    onValueChange = { newValue ->
                        val processed = state.markdownFormatter.processInput(newValue, state.textFieldValue)
                        state.onTextFieldValueChange(processed)
                        onContentChange(processed)
                    }
                )
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = uiState.showTabGrid,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut()
        ) {
            TabGridOverlay(
                tabs = uiState.tabs,
                activeTabIndex = uiState.activeTabIndex,
                onSwitchTab = onSwitchTab,
                onCloseTab = onCloseTab,
                onNewTab = onNewTab,
                onDone = onDismissTabGrid
            )
        }
    }
}

@Composable
private fun EditorContent(
    paddingValues: PaddingValues,
    isPrivate: Boolean,
    textFieldValue: TextFieldValue,
    markdownTransformation: MarkdownVisualTransformation,
    onValueChange: (TextFieldValue) -> Unit
) {
    val blurRadius by animateDpAsState(
        targetValue = if (isPrivate) 12.dp else 0.dp,
        animationSpec = tween(200),
        label = "privacyBlur"
    )
    Box(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
    ) {
        NoteContentTextField(
            value = textFieldValue,
            markdownTransformation = markdownTransformation,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .blur(blurRadius)
        )
        if (isPrivate) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(PointerEventPass.Initial)
                                    .changes.forEach { it.consume() }
                            }
                        }
                    }
            )
        }
    }
}

@Composable
private fun EditorDrawerContent(
    notes: List<Note>,
    currentNoteId: String?,
    onNoteSelected: (Note) -> Unit,
    onClose: () -> Unit
) {
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
            items(notes, key = { it.id }) { note ->
                DrawerNoteItem(
                    note = note,
                    isSelected = currentNoteId == note.id,
                    onClick = { onNoteSelected(note) }
                )
            }
            item {
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTopBar(
    title: String,
    canUndo: Boolean,
    canRedo: Boolean,
    isSaving: Boolean,
    isPrivate: Boolean,
    onTitleChange: (String) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSaveNote: () -> Unit,
    onTogglePrivacy: () -> Unit,
    onNavigationClick: () -> Unit
) {
    TopAppBar(
        title = { EditorTitleField(title = title, onTitleChange = onTitleChange) },
        actions = {
            EditorActionButtons(
                canUndo = canUndo,
                canRedo = canRedo,
                isSaving = isSaving,
                isPrivate = isPrivate,
                onUndo = onUndo,
                onRedo = onRedo,
                onSaveNote = onSaveNote,
                onTogglePrivacy = onTogglePrivacy
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigationClick) {
                Icon(
                    painter = painterResource(R.drawable.outline_format_list_bulleted_24),
                    contentDescription = "Open List"
                )
            }
        }
    )
}

@Composable
private fun EditorTitleField(
    title: String,
    onTitleChange: (String) -> Unit
) {
    TextField(
        value = title,
        onValueChange = onTitleChange,
        modifier = Modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.headlineSmall,
        placeholder = {
            Text("Untitled Note", style = MaterialTheme.typography.headlineSmall)
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
}

@Composable
private fun EditorActionButtons(
    canUndo: Boolean,
    canRedo: Boolean,
    isSaving: Boolean,
    isPrivate: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSaveNote: () -> Unit,
    onTogglePrivacy: () -> Unit
) {
    IconButton(onClick = onTogglePrivacy) {
        Icon(
            imageVector = if (isPrivate) Icons.Default.VisibilityOff else Icons.Default.Visibility,
            contentDescription = if (isPrivate) "Show note" else "Hide note"
        )
    }
    IconButton(onClick = onUndo, enabled = canUndo) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Undo,
            contentDescription = "Undo",
            tint = if (canUndo) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f)
        )
    }
    IconButton(onClick = onRedo, enabled = canRedo) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Redo,
            contentDescription = "Redo",
            tint = if (canRedo) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f)
        )
    }
    IconButton(onClick = onSaveNote, enabled = !isSaving) {
        if (isSaving) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        } else {
            Icon(imageVector = Icons.Default.Save, contentDescription = "Save note")
        }
    }
}

@Composable
private fun EditorBottomBar(
    isKeyboardVisible: Boolean,
    tabCount: Int,
    textFieldValue: TextFieldValue,
    markdownFormatter: MarkdownFormatter,
    onContentChange: (TextFieldValue) -> Unit,
    onNewTab: () -> Unit,
    onTabsClick: () -> Unit
) {
    if (isKeyboardVisible) {
        Column {
            KeyboardAccessoryBar(
                onBoldClick = {
                    val newValue = markdownFormatter.toggleStyle(textFieldValue, BoldRule())
                    onContentChange(newValue)
                },
                onItalicClick = {
                    val newValue = markdownFormatter.toggleStyle(textFieldValue, ItalicRule())
                    onContentChange(newValue)
                },
                onHeaderClick = {
                    val newValue = markdownFormatter.toggleCyclicHeading(textFieldValue, HeaderRule())
                    onContentChange(newValue)
                }
            )
            Spacer(
                modifier = Modifier.windowInsetsBottomHeight(
                    WindowInsets.ime.exclude(WindowInsets.navigationBars)
                )
            )
        }
    } else {
        BrowserBottomBar(
            tabCount = tabCount,
            onSearch = {},
            onNewTab = onNewTab,
            onTabsClick = onTabsClick,
            onMenuClick = {}
        )
    }
}

@Composable
private fun NoteContentTextField(
    value: TextFieldValue,
    markdownTransformation: MarkdownVisualTransformation,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = MaterialTheme.typography.bodyMedium,
        placeholder = { Text("Start writing your note...") },
        visualTransformation = markdownTransformation,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        )
    )
}

@Preview(showBackground = true)
@Composable
fun EditorScreenPreview() {
    DNoteTheme {
        EditorScreen(
            uiState = NoteEditorUiState(),
            onContentChange = {},
            onSaveNote = {},
            onTitleChange = {},
            onUndo = {},
            onRedo = {},
            onNoteSelected = {},
            onNewTab = {},
            onTabsClick = {},
            onSwitchTab = {},
            onCloseTab = {},
            onDismissTabGrid = {}
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
fun TabGridOverlay(
    tabs: List<TabState>,
    activeTabIndex: Int,
    onSwitchTab: (Int) -> Unit,
    onCloseTab: (Int) -> Unit,
    onNewTab: () -> Unit,
    onDone: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNewTab) {
                        Icon(Icons.Default.Add, contentDescription = "New Tab")
                    }
                    Text(
                        text = "${tabs.size} tabs",
                        style = MaterialTheme.typography.titleSmall
                    )
                    androidx.compose.material3.TextButton(onClick = onDone) {
                        Text("Done")
                    }
                }
            }
        }
    ) { paddingValues ->
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                count = tabs.size,
                key = { it }
            ) { index ->
                val tab = tabs[index]
                val isActive = index == activeTabIndex
                TabGridCard(
                    tab = tab,
                    isActive = isActive,
                    onSelect = { onSwitchTab(index) },
                    onClose = { onCloseTab(index) }
                )
            }
        }
    }
}

@Composable
private fun TabGridCard(
    tab: TabState,
    isActive: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit
) {
    val borderColor = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent
    val borderWidth = if (isActive) 3.dp else 0.dp

    androidx.compose.material3.Card(
        onClick = onSelect,
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Text(
                    text = tab.title.ifBlank { "Untitled Note" },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = tab.contentText.ifBlank { "No content" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close tab",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun BrowserBottomBar(
    tabCount: Int,
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
                        Text(tabCount.toString(), style = MaterialTheme.typography.labelSmall)
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
            tabCount = 1,
            onSearch = {},
            onNewTab = {},
            onTabsClick = {},
            onMenuClick = {}
        )
    }
}
