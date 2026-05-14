package com.tasknotes.android.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.animateItem
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import com.tasknotes.android.ui.common.dragContainer
import com.tasknotes.android.ui.common.rememberDragDropState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tasknotes.android.model.Category
import com.tasknotes.android.ui.common.ErrorState

private const val MAX_CATEGORIES = 5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToCategory: (Long, String) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val categories      by viewModel.categories.collectAsState()
    val isLoading       by viewModel.isLoading.collectAsState()
    val loadError       by viewModel.loadError.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val showCreate      by viewModel.showCreateDialog.collectAsState()
    val editing         by viewModel.editingCategory.collectAsState()
    val deleting        by viewModel.deletingCategory.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("TaskNotes") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            if (categories.size < MAX_CATEGORIES) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.showCreateDialog.value = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Nova Categoria") }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        when {
            loadError != null -> {
                ErrorState(
                    message  = loadError!!,
                    onRetry  = viewModel::load,
                    modifier = Modifier.padding(padding)
                )
            }

            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }

            categories.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Nenhuma categoria ainda.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.showCreateDialog.value = true }) {
                            Text("Criar primeira categoria")
                        }
                    }
                }
            }

            else -> {
                val lazyListState = rememberLazyListState()
                val dragState = rememberDragDropState(lazyListState, onSwap = viewModel::reorderCategories, onDragFinished = viewModel::persistCategoryOrder)
                LazyColumn(
                    state               = lazyListState,
                    modifier            = Modifier.fillMaxSize().padding(padding).dragContainer(dragState),
                    contentPadding      = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(categories, key = { _, it -> it.id }) { index, category ->
                        val itemModifier = if (index == dragState.draggingItemIndex) {
                            Modifier.zIndex(1f).graphicsLayer { translationY = dragState.draggingItemOffset }
                        } else {
                            animateItem()
                        }
                        CategoryCard(
                            category = category,
                            modifier = itemModifier,
                            onClick  = { onNavigateToCategory(category.id, category.name) },
                            onEdit   = { viewModel.editingCategory.value = category },
                            onDelete = { viewModel.deletingCategory.value = category }
                        )
                    }

                    if (categories.size >= MAX_CATEGORIES) {
                        item {
                            Text(
                                "Limite de $MAX_CATEGORIES categorias atingido.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Dialogo: nova categoria ───────────────────────────────────────────────
    if (showCreate) {
        CategoryFormDialog(
            title        = "Nova Categoria",
            initialName  = "",
            confirmLabel = "Criar",
            onConfirm    = { viewModel.createCategory(it) },
            onDismiss    = { viewModel.showCreateDialog.value = false }
        )
    }

    // ── Dialogo: editar categoria ─────────────────────────────────────────────
    editing?.let { cat ->
        CategoryFormDialog(
            title        = "Editar Categoria",
            initialName  = cat.name,
            confirmLabel = "Salvar",
            onConfirm    = { viewModel.updateCategory(cat.id, it) },
            onDismiss    = { viewModel.editingCategory.value = null }
        )
    }

    // ── Dialogo: confirmar exclusão ───────────────────────────────────────────
    deleting?.let { cat ->
        AlertDialog(
            onDismissRequest = { viewModel.deletingCategory.value = null },
            title = { Text("Excluir categoria?") },
            text  = {
                Text("\"${cat.name}\" e todas as suas tarefas e anotações serão removidas permanentemente.")
            },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteCategory(cat.id) }) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.deletingCategory.value = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun CategoryCard(
    category: Category,
    modifier: Modifier = Modifier,
    onClick:  () -> Unit,
    onEdit:   () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick   = onClick,
        modifier  = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text     = category.name,
                    style    = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                val pending = category.pendingTaskCount
                Text(
                    text  = if (pending > 0)
                        "$pending tarefa${if (pending > 1) "s" else ""} pendente${if (pending > 1) "s" else ""}"
                    else
                        "Sem tarefas pendentes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Editar",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Excluir",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun CategoryFormDialog(
    title:        String,
    initialName:  String,
    confirmLabel: String,
    onConfirm:    (String) -> Unit,
    onDismiss:    () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    val isValid = name.trim().isNotEmpty() && name.length <= 50

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text  = {
            OutlinedTextField(
                value         = name,
                onValueChange = { if (it.length <= 50) name = it },
                label         = { Text("Nome") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { if (isValid) onConfirm(name) }, enabled = isValid) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
