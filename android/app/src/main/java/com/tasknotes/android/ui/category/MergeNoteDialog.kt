package com.tasknotes.android.ui.category

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tasknotes.android.model.Note

@Composable
fun MergeNoteDialog(
    localTitle:   String,
    localContent: String?,
    serverNote:   Note,
    onConfirm:    (title: String, content: String?) -> Unit,
    onDismiss:    () -> Unit
) {
    var titleChoice   by remember { mutableStateOf(Choice.LOCAL) }
    var contentChoice by remember { mutableStateOf(Choice.LOCAL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title            = { Text("Mesclar alterações") },
        text             = {
            Column(
                modifier            = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Selecione qual versão usar para cada campo:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                MergeField(
                    label        = "Título",
                    localValue   = localTitle,
                    serverValue  = serverNote.title,
                    selected     = titleChoice,
                    onSelect     = { titleChoice = it }
                )

                MergeField(
                    label        = "Conteúdo",
                    localValue   = localContent ?: "(vazio)",
                    serverValue  = serverNote.content ?: "(vazio)",
                    selected     = contentChoice,
                    onSelect     = { contentChoice = it }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val mergedTitle   = if (titleChoice   == Choice.LOCAL) localTitle          else serverNote.title
                val mergedContent = if (contentChoice == Choice.LOCAL) localContent        else serverNote.content
                onConfirm(mergedTitle, mergedContent)
            }) { Text("Confirmar mesclagem") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

enum class Choice { LOCAL, SERVER }

@Composable
private fun MergeField(
    label:       String,
    localValue:  String,
    serverValue: String,
    selected:    Choice,
    onSelect:    (Choice) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

        MergeOption(
            tag      = "Local",
            value    = localValue,
            isSelected = selected == Choice.LOCAL,
            onClick  = { onSelect(Choice.LOCAL) }
        )
        MergeOption(
            tag      = "Servidor",
            value    = serverValue,
            isSelected = selected == Choice.SERVER,
            onClick  = { onSelect(Choice.SERVER) }
        )
    }
}

@Composable
private fun MergeOption(
    tag:        String,
    value:      String,
    isSelected: Boolean,
    onClick:    () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFE5E7EB)
    val bgColor     = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RadioButton(selected = isSelected, onClick = onClick)
        Column {
            Text(
                text  = tag,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text  = value,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
