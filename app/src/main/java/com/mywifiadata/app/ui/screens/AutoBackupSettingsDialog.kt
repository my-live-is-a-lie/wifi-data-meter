package com.mywifiadata.app.ui.screens

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.East
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.West
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mywifiadata.app.R
import com.mywifiadata.app.backup.AutoBackupFrequency
import com.mywifiadata.app.backup.AutomaticBackupSettings
import com.mywifiadata.app.ui.components.ExpressiveCard
import com.mywifiadata.app.ui.components.SettingSwitchRow
import java.util.Date

@Composable
internal fun AutoBackupSettingsDialog(
    settings: AutomaticBackupSettings,
    folderDisplayName: String?,
    onDismiss: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onFrequencyChange: (AutoBackupFrequency) -> Unit,
    onChooseFolder: (enableAfterSelection: Boolean) -> Unit,
    onBackupNow: () -> Unit,
) {
    val context = LocalContext.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val lastBackupLabel = if (settings.lastBackupMillis <= 0L) {
        stringResource(R.string.settings_auto_backup_never)
    } else {
        val date = Date(settings.lastBackupMillis)
        "${DateFormat.getDateFormat(context).format(date)} · ${DateFormat.getTimeFormat(context).format(date)}"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = if (isRtl) Icons.Rounded.East else Icons.Rounded.West,
                            contentDescription = stringResource(R.string.settings_auto_backup_back),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.settings_auto_backup_title),
                        style = MaterialTheme.typography.titleLargeEmphasized,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    ExpressiveCard {
                        SettingSwitchRow(
                            icon = Icons.Rounded.Schedule,
                            title = stringResource(R.string.settings_auto_backup_enabled),
                            subtitle = stringResource(R.string.settings_auto_backup_enabled_desc),
                            checked = settings.enabled,
                            onCheckedChange = onEnabledChange,
                        )
                    }

                    ExpressiveCard {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = stringResource(R.string.settings_auto_backup_schedule),
                                style = MaterialTheme.typography.titleMediumEmphasized,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.settings_auto_backup_schedule_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(14.dp))
                            val frequencies = AutoBackupFrequency.entries
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                frequencies.forEachIndexed { index, frequency ->
                                    SegmentedButton(
                                        selected = settings.frequency == frequency,
                                        onClick = { onFrequencyChange(frequency) },
                                        shape = SegmentedButtonDefaults.itemShape(
                                            index = index,
                                            count = frequencies.size,
                                        ),
                                    ) {
                                        Text(text = frequencyLabel(frequency))
                                    }
                                }
                            }
                        }
                    }

                    ExpressiveCard(onClick = { onChooseFolder(false) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_auto_backup_folder),
                                    style = MaterialTheme.typography.bodyLargeEmphasized,
                                )
                                Text(
                                    text = folderDisplayName
                                        ?: stringResource(R.string.settings_auto_backup_no_folder),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Icon(
                                imageVector = if (isRtl) Icons.Rounded.West else Icons.Rounded.East,
                                contentDescription = stringResource(R.string.settings_auto_backup_choose_folder),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    ExpressiveCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_auto_backup_last),
                                    style = MaterialTheme.typography.bodyLargeEmphasized,
                                )
                                Text(
                                    text = lastBackupLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    FilledTonalButton(
                        onClick = {
                            if (settings.folderUri == null) onChooseFolder(false) else onBackupNow()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        enabled = settings.folderUri != null,
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Icon(Icons.Rounded.CloudUpload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(text = stringResource(R.string.settings_auto_backup_now))
                    }

                    Text(
                        text = stringResource(R.string.settings_auto_backup_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                    if (settings.lastAttemptFailed) {
                        Text(
                            text = stringResource(R.string.settings_auto_backup_failed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun frequencyLabel(frequency: AutoBackupFrequency): String = when (frequency) {
    AutoBackupFrequency.DAILY -> stringResource(R.string.settings_auto_backup_daily)
    AutoBackupFrequency.WEEKLY -> stringResource(R.string.settings_auto_backup_weekly)
    AutoBackupFrequency.MONTHLY -> stringResource(R.string.settings_auto_backup_monthly)
}
