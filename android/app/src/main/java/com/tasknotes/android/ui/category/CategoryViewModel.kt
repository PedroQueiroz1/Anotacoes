package com.tasknotes.android.ui.category

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasknotes.android.model.Note
import com.tasknotes.android.model.Subtask
import com.tasknotes.android.model.Task
import com.tasknotes.android.model.TaskRequest
import com.tasknotes.android.repository.NoteRepository
import com.tasknotes.android.repository.SubtaskRepository
import com.tasknotes.android.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CategoryViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val categoryId: Long = checkNotNull(savedStateHandle["categoryId"])
    val categoryName: String = savedStateHandle["categoryName"] ?: ""

    private val taskRepo    = TaskRepository()
    private val subtaskRepo = SubtaskRepository()
    private val noteRepo    = NoteRepository()

    // ── Tarefas ───────────────────────────────────────────────────────────────
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    val showTaskForm = MutableStateFlow(false)
    val editingTask  = MutableStateFlow<Task?>(null)
    val deletingTask = MutableStateFlow<Task?>(null)

    // ── Subtarefas ────────────────────────────────────────────────────────────
    private val _expandedTaskIds    = MutableStateFlow<Set<Long>>(emptySet())
    val expandedTaskIds: StateFlow<Set<Long>> = _expandedTaskIds.asStateFlow()

    private val _subtasksByTask     = MutableStateFlow<Map<Long, List<Subtask>>>(emptyMap())
    val subtasksByTask: StateFlow<Map<Long, List<Subtask>>> = _subtasksByTask.asStateFlow()

    private val _loadingSubtaskIds  = MutableStateFlow<Set<Long>>(emptySet())
    val loadingSubtaskIds: StateFlow<Set<Long>> = _loadingSubtaskIds.asStateFlow()

    // ── Anotações ─────────────────────────────────────────────────────────────
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    val showNoteForm = MutableStateFlow(false)
    val deletingNote = MutableStateFlow<Note?>(null)

    // ── Sincronização / conflito de anotações ─────────────────────────────────
    private val _noteSyncState = MutableStateFlow<Map<Long, SyncStateHolder>>(emptyMap())
    val noteSyncState: StateFlow<Map<Long, SyncStateHolder>> = _noteSyncState.asStateFlow()

    private val _conflictNote = MutableStateFlow<NoteConflictState?>(null)
    val conflictNote: StateFlow<NoteConflictState?> = _conflictNote.asStateFlow()

    // ── Compartilhado ─────────────────────────────────────────────────────────
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = null
            var hasError = false
            taskRepo.getByCategory(categoryId).fold(
                onSuccess = { list -> _tasks.value = list.sortedWith(taskComparator) },
                onFailure = { hasError = true }
            )
            noteRepo.getByCategory(categoryId).fold(
                onSuccess = { _notes.value = it },
                onFailure = { hasError = true }
            )
            if (hasError) {
                _loadError.value = "Não foi possível carregar o conteúdo.\nVerifique a conexão e tente novamente."
            }
            _isLoading.value = false
        }
    }

    // ── Operações de tarefa ───────────────────────────────────────────────────
    fun createTask(title: String, desc: String?, date: String?, priority: String) {
        viewModelScope.launch {
            taskRepo.create(categoryId, TaskRequest(title, desc?.ifBlank { null }, date?.ifBlank { null }, priority))
                .fold(
                    onSuccess = { created ->
                        _tasks.value = (_tasks.value + created).sortedWith(taskComparator)
                        showTaskForm.value = false
                    },
                    onFailure = { _snackbarMessage.value = it.message ?: "Erro ao criar tarefa." }
                )
        }
    }

    fun updateTask(id: Long, title: String, desc: String?, date: String?, priority: String, newStatus: String) {
        viewModelScope.launch {
            val oldTask = _tasks.value.find { it.id == id } ?: return@launch
            val updateResult = taskRepo.update(id, TaskRequest(title, desc?.ifBlank { null }, date?.ifBlank { null }, priority))
            if (updateResult.isFailure) {
                _snackbarMessage.value = updateResult.exceptionOrNull()?.message ?: "Erro ao atualizar."
                return@launch
            }
            var updated = updateResult.getOrNull()!!
            if (newStatus != oldTask.status) {
                taskRepo.updateStatus(id, newStatus).onSuccess { updated = it }
            }
            _tasks.value = _tasks.value.map { if (it.id == id) updated else it }.sortedWith(taskComparator)
            showTaskForm.value = false
            editingTask.value  = null
        }
    }

    fun updateTaskStatus(id: Long, status: String) {
        viewModelScope.launch {
            taskRepo.updateStatus(id, status).fold(
                onSuccess = { updated ->
                    _tasks.value = _tasks.value.map { if (it.id == id) updated else it }
                        .sortedWith(taskComparator)
                },
                onFailure = { _snackbarMessage.value = "Erro ao atualizar status." }
            )
        }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch {
            taskRepo.delete(id).fold(
                onSuccess = {
                    _tasks.value = _tasks.value.filter { it.id != id }
                    _subtasksByTask.value = _subtasksByTask.value - id
                    _expandedTaskIds.value = _expandedTaskIds.value - id
                    deletingTask.value = null
                },
                onFailure = { _snackbarMessage.value = "Erro ao excluir tarefa."; deletingTask.value = null }
            )
        }
    }

    // ── Operações de subtarefa ────────────────────────────────────────────────
    fun toggleTaskExpansion(taskId: Long) {
        val expanded = _expandedTaskIds.value
        if (taskId in expanded) {
            _expandedTaskIds.value = expanded - taskId
        } else {
            _expandedTaskIds.value = expanded + taskId
            if (_subtasksByTask.value[taskId] == null) {
                loadSubtasks(taskId)
            }
        }
    }

    private fun loadSubtasks(taskId: Long) {
        viewModelScope.launch {
            _loadingSubtaskIds.value = _loadingSubtaskIds.value + taskId
            subtaskRepo.getByTask(taskId).fold(
                onSuccess = { list -> _subtasksByTask.value = _subtasksByTask.value + (taskId to list) },
                onFailure = { _snackbarMessage.value = "Erro ao carregar subtarefas." }
            )
            _loadingSubtaskIds.value = _loadingSubtaskIds.value - taskId
        }
    }

    fun addSubtask(taskId: Long, text: String) {
        viewModelScope.launch {
            subtaskRepo.create(taskId, text).fold(
                onSuccess = { created ->
                    val current = _subtasksByTask.value[taskId] ?: emptyList()
                    _subtasksByTask.value = _subtasksByTask.value + (taskId to current + created)
                },
                onFailure = { _snackbarMessage.value = it.message ?: "Erro ao adicionar subtarefa." }
            )
        }
    }

    fun toggleSubtask(subtaskId: Long, taskId: Long) {
        viewModelScope.launch {
            subtaskRepo.toggle(subtaskId).fold(
                onSuccess = { updated ->
                    val list = _subtasksByTask.value[taskId]?.map { if (it.id == subtaskId) updated else it }
                    if (list != null) _subtasksByTask.value = _subtasksByTask.value + (taskId to list)
                },
                onFailure = { _snackbarMessage.value = "Erro ao atualizar subtarefa." }
            )
        }
    }

    fun deleteSubtask(subtaskId: Long, taskId: Long) {
        viewModelScope.launch {
            subtaskRepo.delete(subtaskId).fold(
                onSuccess = {
                    val list = _subtasksByTask.value[taskId]?.filter { it.id != subtaskId }
                    if (list != null) _subtasksByTask.value = _subtasksByTask.value + (taskId to list)
                },
                onFailure = { _snackbarMessage.value = "Erro ao excluir subtarefa." }
            )
        }
    }

    // ── Operações de anotação ─────────────────────────────────────────────────
    fun createNote(title: String, content: String?) {
        viewModelScope.launch {
            noteRepo.create(categoryId, title, content?.ifBlank { null }).fold(
                onSuccess = { created -> _notes.value = listOf(created) + _notes.value; showNoteForm.value = false },
                onFailure = { _snackbarMessage.value = it.message ?: "Erro ao criar anotação." }
            )
        }
    }

    fun updateNote(id: Long, title: String, content: String?) {
        viewModelScope.launch {
            noteRepo.update(id, title, content?.ifBlank { null }).fold(
                onSuccess = { updated -> _notes.value = _notes.value.map { if (it.id == id) updated else it } },
                onFailure = { _snackbarMessage.value = it.message ?: "Erro ao atualizar anotação." }
            )
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            noteRepo.delete(id).fold(
                onSuccess = { _notes.value = _notes.value.filter { it.id != id }; deletingNote.value = null },
                onFailure = { _snackbarMessage.value = "Erro ao excluir anotação."; deletingNote.value = null }
            )
        }
    }

    fun clearSnackbar() { _snackbarMessage.value = null }

    // ── Sincronização / detecção de conflito ──────────────────────────────────
    fun startNoteEdit(noteId: Long, loadedUpdatedAt: String) {
        _noteSyncState.value = _noteSyncState.value + (noteId to SyncStateHolder(loadedUpdatedAt))
    }

    fun endNoteEdit(noteId: Long) {
        _noteSyncState.value = _noteSyncState.value - noteId
    }

    fun checkNoteConflict(noteId: Long, localTitle: String, localContent: String?) {
        val loadedAt = _noteSyncState.value[noteId]?.loadedUpdatedAt ?: return
        viewModelScope.launch {
            noteRepo.getById(noteId).fold(
                onSuccess = { serverNote ->
                    if (serverNote.updatedAt != loadedAt) {
                        _conflictNote.value = NoteConflictState(noteId, serverNote, localTitle, localContent)
                    } else {
                        _snackbarMessage.value = "Sem conflitos. Dados atualizados."
                    }
                },
                onFailure = { _snackbarMessage.value = "Erro ao verificar atualização." }
            )
        }
    }

    fun discardNoteConflict() {
        val state = _conflictNote.value ?: return
        _notes.value = _notes.value.map { if (it.id == state.noteId) state.serverNote else it }
        _noteSyncState.value = _noteSyncState.value - state.noteId
        _conflictNote.value  = null
    }

    fun resolveNoteConflict(mergedTitle: String, mergedContent: String?) {
        val state = _conflictNote.value ?: return
        _conflictNote.value  = null
        _noteSyncState.value = _noteSyncState.value - state.noteId
        updateNote(state.noteId, mergedTitle, mergedContent)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private val taskComparator = Comparator<Task> { a, b ->
        val priorityOrder = mapOf("HIGH" to 0, "MEDIUM" to 1, "LOW" to 2)
        val p = (priorityOrder[a.priority] ?: 1) - (priorityOrder[b.priority] ?: 1)
        if (p != 0) p else a.createdAt.compareTo(b.createdAt)
    }
}
