package com.tasknotes.android.ui.category

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.animateItem
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import com.tasknotes.android.ui.common.dragContainer
import com.tasknotes.android.ui.common.rememberDragDropState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tasknotes.android.model.Note
import com.tasknotes.android.model.Subtask
import com.tasknotes.android.model.Task
import com.tasknotes.android.ui.common.ErrorState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    viewModel: CategoryViewModel,
    onBack:    () -> Unit
) {
    val tasks             by viewModel.tasks.collectAsState()
    val notes             by viewModel.notes.collectAsState()
    val isLoading         by viewModel.isLoading.collectAsState()
    val loadError         by viewModel.loadError.collectAsState()
    val snackbarMessage   by viewModel.snackbarMessage.collectAsState()
    val showTaskForm      by viewModel.showTaskForm.collectAsState()
    val editingTask       by viewModel.editingTask.collectAsState()
    val deletingTask      by viewModel.deletingTask.collectAsState()
    val showNoteForm      by viewModel.showNoteForm.collectAsState()
    val deletingNote      by viewModel.deletingNote.collectAsState()
    val expandedTaskIds   by viewModel.expandedTaskIds.collectAsState()
    val subtasksByTask    by viewModel.subtasksByTask.collectAsState()
    val loadingSubtaskIds by viewModel.loadingSubtaskIds.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text     = viewModel.categoryName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = MaterialTheme.colorScheme.primary,
                    titleContentColor          = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (selectedTab == 0) viewModel.showTaskForm.value = true
                    else viewModel.showNoteForm.value = true
                },
                icon          = { Icon(Icons.Filled.Add, contentDescription = null) },
                text          = { Text(if (selectedTab == 0) "Nova Tarefa" else "Nova Anotação") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor  = MaterialTheme.colorScheme.onPrimary
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick  = { selectedTab = 0 },
                    text     = { Text("Tarefas") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick  = { selectedTab = 1 },
                    text     = { Text("Anotações") }
                )
            }

            when {
                loadError != null -> {
                    ErrorState(
                        message = loadError!!,
                        onRetry = viewModel::load
                    )
                }
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                selectedTab == 0 -> {
                    TasksTab(
                        tasks             = tasks,
                        expandedTaskIds   = expandedTaskIds,
                        subtasksByTask    = subtasksByTask,
                        loadingSubtaskIds = loadingSubtaskIds,
                        onToggleExpand    = viewModel::toggleTaskExpansion,
                        onAddSubtask      = viewModel::addSubtask,
                        onToggleSubtask   = viewModel::toggleSubtask,
                        onDeleteSubtask   = viewModel::deleteSubtask,
                        onStatusChange    = viewModel::updateTaskStatus,
                        onEdit            = { viewModel.editingTask.value = it; viewModel.showTaskForm.value = true },
                        onDelete          = { viewModel.deletingTask.value = it },
                        onReorder         = viewModel::reorderTasks,
                        onReorderFinished = viewModel::persistTaskOrder
                    )
                }
                else -> {
                    NotesTab(
                        notes             = notes,
                        onUpdate          = viewModel::updateNote,
                        onDelete          = { viewModel.deletingNote.value = it },
                        onReorder         = viewModel::reorderNotes,
                        onReorderFinished = viewModel::persistNoteOrder
                    )
                }
            }
        }
    }

    if (showTaskForm) {
        TaskFormDialog(
            task      = editingTask,
            onDismiss = { viewModel.showTaskForm.value = false; viewModel.editingTask.value = null },
            onConfirm = { title, desc, date, priority, status ->
                val t = editingTask
                if (t == null) viewModel.createTask(title, desc, date, priority)
                else viewModel.updateTask(t.id, title, desc, date, priority, status)
            }
        )
    }

    if (showNoteForm) {
        NoteFormDialog(
            onDismiss = { viewModel.showNoteForm.value = false },
            onConfirm = { title, content -> viewModel.createNote(title, content) }
        )
    }

    deletingTask?.let { task ->
        AlertDialog(
            onDismissRequest = { viewModel.deletingTask.value = null },
            title            = { Text("Excluir tarefa?") },
            text             = { Text("\"${task.title}\" será excluída permanentemente.") },
            confirmButton    = {
                TextButton(onClick = { viewModel.deleteTask(task.id) }) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.deletingTask.value = null }) { Text("Cancelar") }
            }
        )
    }

    deletingNote?.let { note ->
        AlertDialog(
            onDismissRequest = { viewModel.deletingNote.value = null },
            title            = { Text("Excluir anotação?") },
            text             = { Text("\"${note.title}\" será excluída permanentemente.") },
            confirmButton    = {
                TextButton(onClick = { viewModel.deleteNote(note.id) }) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.deletingNote.value = null }) { Text("Cancelar") }
            }
        )
    }

}

// ── Tasks tab ─────────────────────────────────────────────────────────────────

@Composable
private fun TasksTab(
    tasks:             List<Task>,
    expandedTaskIds:   Set<Long>,
    subtasksByTask:    Map<Long, List<Subtask>>,
    loadingSubtaskIds: Set<Long>,
    onToggleExpand:    (Long) -> Unit,
    onAddSubtask:      (Long, String) -> Unit,
    onToggleSubtask:   (Long, Long) -> Unit,
    onDeleteSubtask:   (Long, Long) -> Unit,
    onStatusChange:    (Long, String) -> Unit,
    onEdit:            (Task) -> Unit,
    onDelete:          (Task) -> Unit,
    onReorder:         (Int, Int) -> Unit,
    onReorderFinished: () -> Unit
) {
    if (tasks.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nenhuma tarefa ainda.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        val lazyListState = rememberLazyListState()
        val dragState = rememberDragDropState(lazyListState, onSwap = onReorder, onDragFinished = onReorderFinished)
        LazyColumn(
            state               = lazyListState,
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier            = Modifier.fillMaxSize().dragContainer(dragState)
        ) {
            itemsIndexed(tasks, key = { _, it -> it.id }) { index, task ->
                val itemModifier = if (index == dragState.draggingItemIndex) {
                    Modifier.zIndex(1f).graphicsLayer { translationY = dragState.draggingItemOffset }
                } else {
                    animateItem()
                }
                TaskCard(
                    task             = task,
                    modifier         = itemModifier,
                    isExpanded       = task.id in expandedTaskIds,
                    subtasks         = subtasksByTask[task.id],
                    loadingSubtasks  = task.id in loadingSubtaskIds,
                    onToggleExpand   = { onToggleExpand(task.id) },
                    onAddSubtask     = { text -> onAddSubtask(task.id, text) },
                    onToggleSubtask  = { subId -> onToggleSubtask(subId, task.id) },
                    onDeleteSubtask  = { subId -> onDeleteSubtask(subId, task.id) },
                    onStatusChange   = { status -> onStatusChange(task.id, status) },
                    onEdit           = { onEdit(task) },
                    onDelete         = { onDelete(task) }
                )
            }
        }
    }
}

@Composable
private fun TaskCard(
    task:            Task,
    modifier:        Modifier = Modifier,
    isExpanded:      Boolean,
    subtasks:        List<Subtask>?,
    loadingSubtasks: Boolean,
    onToggleExpand:  () -> Unit,
    onAddSubtask:    (String) -> Unit,
    onToggleSubtask: (Long) -> Unit,
    onDeleteSubtask: (Long) -> Unit,
    onStatusChange:  (String) -> Unit,
    onEdit:          () -> Unit,
    onDelete:        () -> Unit
) {
    var statusMenuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier  = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PriorityBadge(task.priority)
                Spacer(Modifier.width(8.dp))
                Text(
                    text      = task.title,
                    style     = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier  = Modifier.weight(1f),
                    maxLines  = 2,
                    overflow  = TextOverflow.Ellipsis
                )
                Box {
                    TextButton(onClick = { statusMenuExpanded = true }) {
                        Text(statusLabel(task.status), style = MaterialTheme.typography.labelSmall)
                    }
                    DropdownMenu(
                        expanded          = statusMenuExpanded,
                        onDismissRequest  = { statusMenuExpanded = false }
                    ) {
                        listOf("PENDING", "IN_PROGRESS", "DONE").forEach { s ->
                            DropdownMenuItem(
                                text    = { Text(statusLabel(s)) },
                                onClick = { onStatusChange(s); statusMenuExpanded = false }
                            )
                        }
                    }
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = "Editar", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Excluir",
                        tint     = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (!task.description.isNullOrBlank()) {
                Text(
                    text     = task.description,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (!task.dueDate.isNullOrBlank()) {
                Text(
                    text     = "Vencimento: ${task.dueDate}",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            TextButton(
                onClick  = onToggleExpand,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    imageVector        = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    modifier           = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (isExpanded) "Ocultar subtarefas" else "Ver subtarefas",
                    style = MaterialTheme.typography.labelSmall
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                SubtaskSection(
                    subtasks = subtasks,
                    loading  = loadingSubtasks,
                    onAdd    = onAddSubtask,
                    onToggle = onToggleSubtask,
                    onDelete = onDeleteSubtask
                )
            }
        }
    }
}

@Composable
private fun SubtaskSection(
    subtasks: List<Subtask>?,
    loading:  Boolean,
    onAdd:    (String) -> Unit,
    onToggle: (Long) -> Unit,
    onDelete: (Long) -> Unit
) {
    var newText by remember { mutableStateOf("") }
    val limit   = 20
    val count   = subtasks?.size ?: 0

    Column(modifier = Modifier.padding(top = 4.dp)) {
        when {
            loading -> CircularProgressIndicator(
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.CenterHorizontally)
            )
            subtasks != null -> {
                subtasks.forEach { sub ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(checked = sub.done, onCheckedChange = { onToggle(sub.id) })
                        Text(
                            text            = sub.text,
                            style           = MaterialTheme.typography.bodySmall,
                            textDecoration  = if (sub.done) TextDecoration.LineThrough else TextDecoration.None,
                            modifier        = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onDelete(sub.id) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Remover", modifier = Modifier.size(14.dp))
                        }
                    }
                }

                if (count < limit) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value         = newText,
                            onValueChange = { newText = it },
                            placeholder   = { Text("Nova subtarefa", style = MaterialTheme.typography.bodySmall) },
                            modifier      = Modifier.weight(1f),
                            singleLine    = true,
                            textStyle     = MaterialTheme.typography.bodySmall
                        )
                        IconButton(
                            onClick  = {
                                if (newText.isNotBlank()) { onAdd(newText.trim()); newText = "" }
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Adicionar")
                        }
                    }
                } else {
                    Text(
                        "Limite de $limit subtarefas atingido.",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PriorityBadge(priority: String) {
    val (label, color) = when (priority) {
        "HIGH"   -> "Alta"  to Color(0xFFDC2626)
        "MEDIUM" -> "Média" to Color(0xFFF59E0B)
        else     -> "Baixa" to Color(0xFF16A34A)
    }
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text       = label,
            style      = MaterialTheme.typography.labelSmall,
            color      = color,
            fontWeight = FontWeight.Bold
        )
    }
}

// ── Notes tab ─────────────────────────────────────────────────────────────────

@Composable
private fun NotesTab(
    notes:             List<Note>,
    onUpdate:          (Long, String, String?) -> Unit,
    onDelete:          (Note) -> Unit,
    onReorder:         (Int, Int) -> Unit,
    onReorderFinished: () -> Unit
) {
    if (notes.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nenhuma anotação ainda.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        val lazyListState = rememberLazyListState()
        val dragState = rememberDragDropState(lazyListState, onSwap = onReorder, onDragFinished = onReorderFinished)
        LazyColumn(
            state               = lazyListState,
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier            = Modifier.fillMaxSize().dragContainer(dragState)
        ) {
            itemsIndexed(notes, key = { _, it -> it.id }) { index, note ->
                val itemModifier = if (index == dragState.draggingItemIndex) {
                    Modifier.zIndex(1f).graphicsLayer { translationY = dragState.draggingItemOffset }
                } else {
                    animateItem()
                }
                NoteCard(
                    note     = note,
                    modifier = itemModifier,
                    onUpdate = onUpdate,
                    onDelete = { onDelete(note) }
                )
            }
        }
    }
}

@Composable
private fun NoteCard(
    note:     Note,
    modifier: Modifier = Modifier,
    onUpdate: (Long, String, String?) -> Unit,
    onDelete: () -> Unit
) {
    var isEditing   by remember(note.id) { mutableStateOf(false) }
    var editTitle   by remember(note.id) { mutableStateOf(note.title) }
    var editContent by remember(note.id) { mutableStateOf(note.content ?: "") }

    Card(
        modifier  = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            if (isEditing) {
                OutlinedTextField(
                    value         = editTitle,
                    onValueChange = { editTitle = it },
                    label         = { Text("Título") },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value         = editContent,
                    onValueChange = { editContent = it },
                    label         = { Text("Conteúdo") },
                    modifier      = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp),
                    maxLines      = 6
                )
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        isEditing   = false
                        editTitle   = note.title
                        editContent = note.content ?: ""
                    }) { Text("Cancelar") }
                    Spacer(Modifier.width(4.dp))
                    Button(
                        onClick = {
                            if (editTitle.isNotBlank()) {
                                onUpdate(note.id, editTitle.trim(), editContent.ifBlank { null })
                                isEditing = false
                            }
                        },
                        enabled = editTitle.isNotBlank()
                    ) { Text("Salvar") }
                }
            } else {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text       = note.title,
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (!note.content.isNullOrBlank()) {
                            Text(
                                text     = note.content,
                                style    = MaterialTheme.typography.bodySmall,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick  = { isEditing = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = "Editar", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Excluir",
                            tint     = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Dialogs ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskFormDialog(
    task:      Task?,
    onDismiss: () -> Unit,
    onConfirm: (title: String, desc: String?, date: String?, priority: String, status: String) -> Unit
) {
    val isEdit = task != null
    var title               by remember(task?.id) { mutableStateOf(task?.title ?: "") }
    var desc                by remember(task?.id) { mutableStateOf(task?.description ?: "") }
    var date                by remember(task?.id) { mutableStateOf(task?.dueDate ?: "") }
    var priority            by remember(task?.id) { mutableStateOf(task?.priority ?: "MEDIUM") }
    var status              by remember(task?.id) { mutableStateOf(task?.status ?: "PENDING") }
    var priorityExpanded    by remember { mutableStateOf(false) }
    var statusExpanded      by remember { mutableStateOf(false) }
    var titleError          by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title            = { Text(if (isEdit) "Editar Tarefa" else "Nova Tarefa") },
        text             = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value         = title,
                    onValueChange = { title = it; titleError = false },
                    label         = { Text("Título *") },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    isError       = titleError,
                    supportingText = if (titleError) { { Text("Campo obrigatório") } } else null
                )
                OutlinedTextField(
                    value         = desc,
                    onValueChange = { desc = it },
                    label         = { Text("Descrição") },
                    modifier      = Modifier.fillMaxWidth(),
                    maxLines      = 3
                )
                OutlinedTextField(
                    value         = date,
                    onValueChange = { date = it },
                    label         = { Text("Vencimento (AAAA-MM-DD)") },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true
                )
                ExposedDropdownMenuBox(
                    expanded          = priorityExpanded,
                    onExpandedChange  = { priorityExpanded = it }
                ) {
                    OutlinedTextField(
                        value         = priorityLabel(priority),
                        onValueChange = {},
                        readOnly      = true,
                        label         = { Text("Prioridade") },
                        trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(priorityExpanded) },
                        modifier      = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded         = priorityExpanded,
                        onDismissRequest = { priorityExpanded = false }
                    ) {
                        listOf("HIGH", "MEDIUM", "LOW").forEach { p ->
                            DropdownMenuItem(
                                text    = { Text(priorityLabel(p)) },
                                onClick = { priority = p; priorityExpanded = false }
                            )
                        }
                    }
                }
                if (isEdit) {
                    ExposedDropdownMenuBox(
                        expanded         = statusExpanded,
                        onExpandedChange = { statusExpanded = it }
                    ) {
                        OutlinedTextField(
                            value         = statusLabel(status),
                            onValueChange = {},
                            readOnly      = true,
                            label         = { Text("Status") },
                            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(statusExpanded) },
                            modifier      = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded         = statusExpanded,
                            onDismissRequest = { statusExpanded = false }
                        ) {
                            listOf("PENDING", "IN_PROGRESS", "DONE").forEach { s ->
                                DropdownMenuItem(
                                    text    = { Text(statusLabel(s)) },
                                    onClick = { status = s; statusExpanded = false }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isBlank()) { titleError = true; return@TextButton }
                onConfirm(title.trim(), desc.ifBlank { null }, date.ifBlank { null }, priority, status)
            }) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun NoteFormDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, content: String?) -> Unit
) {
    var title      by remember { mutableStateOf("") }
    var content    by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title            = { Text("Nova Anotação") },
        text             = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value         = title,
                    onValueChange = { title = it; titleError = false },
                    label         = { Text("Título *") },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    isError       = titleError,
                    supportingText = if (titleError) { { Text("Campo obrigatório") } } else null
                )
                OutlinedTextField(
                    value         = content,
                    onValueChange = { content = it },
                    label         = { Text("Conteúdo") },
                    modifier      = Modifier.fillMaxWidth(),
                    maxLines      = 5
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isBlank()) { titleError = true; return@TextButton }
                onConfirm(title.trim(), content.ifBlank { null })
            }) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun priorityLabel(p: String) = when (p) {
    "HIGH"   -> "Alta"
    "MEDIUM" -> "Média"
    else     -> "Baixa"
}

private fun statusLabel(s: String) = when (s) {
    "IN_PROGRESS" -> "Em andamento"
    "DONE"        -> "Concluída"
    else          -> "Pendente"
}
