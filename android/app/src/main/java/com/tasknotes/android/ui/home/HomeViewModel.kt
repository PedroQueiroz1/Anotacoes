package com.tasknotes.android.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasknotes.android.model.Category
import com.tasknotes.android.repository.CategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val repository = CategoryRepository()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    val showCreateDialog  = MutableStateFlow(false)
    val editingCategory   = MutableStateFlow<Category?>(null)
    val deletingCategory  = MutableStateFlow<Category?>(null)

    init { load() }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = null
            repository.getAll().fold(
                onSuccess = { _categories.value = it },
                onFailure = { _loadError.value = "Não foi possível carregar as categorias.\nVerifique a conexão e tente novamente." }
            )
            _isLoading.value = false
        }
    }

    fun createCategory(name: String) {
        viewModelScope.launch {
            repository.create(name).fold(
                onSuccess = { created ->
                    _categories.value = _categories.value + created
                    showCreateDialog.value = false
                },
                onFailure = { e ->
                    _snackbarMessage.value = e.message ?: "Erro ao criar categoria."
                    showCreateDialog.value = false
                }
            )
        }
    }

    fun updateCategory(id: Long, name: String) {
        viewModelScope.launch {
            repository.update(id, name).fold(
                onSuccess = { updated ->
                    _categories.value = _categories.value.map { if (it.id == id) updated else it }
                    editingCategory.value = null
                },
                onFailure = { e ->
                    _snackbarMessage.value = e.message ?: "Erro ao atualizar categoria."
                    editingCategory.value = null
                }
            )
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            repository.delete(id).fold(
                onSuccess = {
                    _categories.value = _categories.value.filter { it.id != id }
                    deletingCategory.value = null
                },
                onFailure = { e ->
                    _snackbarMessage.value = e.message ?: "Erro ao excluir categoria."
                    deletingCategory.value = null
                }
            )
        }
    }

    fun clearSnackbar() { _snackbarMessage.value = null }
}
