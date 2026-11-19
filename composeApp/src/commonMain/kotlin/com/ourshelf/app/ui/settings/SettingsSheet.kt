package com.ourshelf.app.ui.settings

import com.ourshelf.app.resources.Res
import com.ourshelf.app.resources.attribution_justwatch
import com.ourshelf.app.resources.attribution_sources
import com.ourshelf.app.resources.attribution_tmdb
import com.ourshelf.app.resources.settings_haptics
import com.ourshelf.app.resources.settings_haptics_desc
import com.ourshelf.app.resources.settings_motion
import com.ourshelf.app.resources.settings_motion_desc
import com.ourshelf.app.resources.settings_motion_system_off
import com.ourshelf.app.resources.settings_sources
import com.ourshelf.app.resources.settings_title
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ourshelf.app.data.prefs.UserPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    preferences: UserPreferences,
    systemAnimationsEnabled: Boolean,
    onHapticsChange: (Boolean) -> Unit,
    onMotionChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text(stringResource(Res.string.settings_title), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            ToggleRow(
                title = stringResource(Res.string.settings_haptics),
                description = stringResource(Res.string.settings_haptics_desc),
                checked = preferences.hapticsEnabled,
                onCheckedChange = onHapticsChange,
            )
            ToggleRow(
                title = stringResource(Res.string.settings_motion),
                description = if (systemAnimationsEnabled) {
                    stringResource(Res.string.settings_motion_desc)
                } else {
                    stringResource(Res.string.settings_motion_system_off)
                },
                checked = preferences.motionEffectsEnabled && systemAnimationsEnabled,
                enabled = systemAnimationsEnabled,
                onCheckedChange = onMotionChange,
            )
            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            Text(stringResource(Res.string.settings_sources), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            listOf(Res.string.attribution_sources, Res.string.attribution_justwatch, Res.string.attribution_tmdb).forEach {
                Text(
                    stringResource(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}
