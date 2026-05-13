package com.tasknotes.android.ui.category

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun ConflictDialog(
    onDiscard: () -> Unit,
    onMerge:   () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title            = { Text("Conflito detectado") },
        text             = {
            Text("Esta anotação foi modificada no servidor desde que você a carregou. Como deseja proceder?")
        },
        confirmButton    = {
            TextButton(onClick = onMerge) { Text("Mesclar") }
        },
        dismissButton    = {
            TextButton(onClick = onDiscard) { Text("Descartar local") }
        }
    )
}
