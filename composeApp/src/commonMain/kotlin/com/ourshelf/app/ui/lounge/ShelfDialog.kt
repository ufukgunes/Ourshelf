package com.ourshelf.app.ui.lounge

import com.ourshelf.app.resources.Res
import com.ourshelf.app.resources.action_cancel
import com.ourshelf.app.resources.action_create
import com.ourshelf.app.resources.action_save
import com.ourshelf.app.resources.shelf_delete
import com.ourshelf.app.resources.shelf_dialog_rename_title
import com.ourshelf.app.resources.shelf_dialog_title
import com.ourshelf.app.resources.shelf_emoji_label
import com.ourshelf.app.resources.shelf_name_hint
import com.ourshelf.app.resources.shelf_name_label
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.ourshelf.app.core.model.MediaCollection
import com.ourshelf.app.data.repository.CollectionRepository

@Composable
fun ShelfDialog(
    collection: MediaCollection?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, emoji: String) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by rememberSaveable { mutableStateOf(collection?.name.orEmpty()) }
    var emoji by rememberSaveable { mutableStateOf(collection?.iconEmoji ?: CollectionRepository.DEFAULT_EMOJI) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (collection == null) Res.string.shelf_dialog_title else Res.string.shelf_dialog_rename_title,
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = emoji,
                        // Bir emoji birden fazla UTF-16 karakteri olabilir; kısa tut ama kesme.
                        onValueChange = { if (it.length <= 8) emoji = it },
                        label = { Text(stringResource(Res.string.shelf_emoji_label)) },
                        singleLine = true,
                        modifier = Modifier.width(88.dp),
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { if (it.length <= 60) name = it },
                        label = { Text(stringResource(Res.string.shelf_name_label)) },
                        placeholder = { Text(stringResource(Res.string.shelf_name_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text(stringResource(Res.string.shelf_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, emoji) }, enabled = name.isNotBlank()) {
                Text(stringResource(if (collection == null) Res.string.action_create else Res.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
        },
    )
}
