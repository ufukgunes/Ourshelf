package com.ourshelf.app.ui.intake

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ourshelf.app.core.model.MediaSearchResult
import com.ourshelf.app.resources.Res
import com.ourshelf.app.resources.intake_add
import com.ourshelf.app.resources.intake_added
import com.ourshelf.app.resources.intake_close
import com.ourshelf.app.resources.intake_nothing
import com.ourshelf.app.resources.intake_title
import com.ourshelf.app.ui.components.Poster
import com.ourshelf.app.ui.components.formatLength
import com.ourshelf.app.ui.components.labelRes
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Başka bir uygulamadan "Paylaş → Ourshelf" ile gelinen alt kart. Uygulamanın tamamı açılmaz;
 * paylaşılan eser tanınır ve tek dokunuşla listeye eklenir.
 */
@Composable
fun ShareIntakeSheet(
    sharedText: String,
    onClose: () -> Unit,
    viewModel: ShareIntakeViewModel = koinViewModel { parametersOf(sharedText) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ModalBottomSheet(onDismissRequest = onClose, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp),
        ) {
            Text(stringResource(Res.string.intake_title), style = MaterialTheme.typography.titleLarge)
            Text(
                state.sharedText.trim(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(16.dp))
            when (val phase = state.phase) {
                IntakePhase.Loading -> Box(Modifier.fillMaxWidth().height(160.dp), Alignment.Center) {
                    CircularProgressIndicator()
                }
                IntakePhase.NotRecognised -> Text(
                    stringResource(Res.string.intake_nothing),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                )
                is IntakePhase.Results -> Column(Modifier.verticalScroll(rememberScrollState())) {
                    phase.results.forEach { result ->
                        IntakeRow(
                            result = result,
                            added = result.externalId in state.savedIds,
                            onAdd = { viewModel.add(result) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(Res.string.intake_close))
            }
        }
    }
}

@Composable
private fun IntakeRow(result: MediaSearchResult, added: Boolean, onAdd: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Poster(
            title = result.title,
            type = result.mediaType,
            model = result.posterUrl,
            modifier = Modifier.width(52.dp),
            cornerRadius = 6.dp,
            showFallbackTitle = false,
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(result.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(
                    stringResource(result.mediaType.labelRes),
                    result.subtitle,
                    result.lengthValue?.let { formatLength(result.mediaType, it) },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        if (added) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(Res.string.intake_added), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
            }
        } else {
            FilledTonalButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(Res.string.intake_add))
            }
        }
    }
}
