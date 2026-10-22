package com.markleaf.notes.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R

/**
 * The "Locked notes" passcode control (#155): set, change, or remove the passcode
 * that gates the Locked space. This is a UI-visibility gate — the description says
 * so plainly rather than implying encryption at rest.
 */
@Composable
internal fun LockedNotesPasscodeSetting(
    passcodeSet: Boolean,
    onSetPasscode: (String) -> Unit,
    onRemovePasscode: () -> Unit,
    titleModifier: Modifier = Modifier
) {
    var showSetDialog by remember { mutableStateOf(false) }
    var showRemoveConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.locked_passcode_setting_title),
            modifier = titleModifier,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = stringResource(R.string.locked_passcode_setting_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
        Spacer(Modifier.height(8.dp))
        if (passcodeSet) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showSetDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.locked_passcode_change))
                }
                OutlinedButton(
                    onClick = { showRemoveConfirm = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.locked_passcode_remove))
                }
            }
        } else {
            OutlinedButton(
                onClick = { showSetDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.locked_passcode_set))
            }
        }
    }

    if (showSetDialog) {
        SetPasscodeDialog(
            onDismiss = { showSetDialog = false },
            onConfirm = { passcode ->
                showSetDialog = false
                onSetPasscode(passcode)
            }
        )
    }

    if (showRemoveConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirm = false },
            title = { Text(stringResource(R.string.locked_passcode_remove_confirm_title)) },
            text = { Text(stringResource(R.string.locked_passcode_remove_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveConfirm = false
                    onRemovePasscode()
                }) {
                    Text(stringResource(R.string.locked_passcode_remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun SetPasscodeDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newPasscode by remember { mutableStateOf("") }
    var confirmPasscode by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.locked_passcode_dialog_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = newPasscode,
                    onValueChange = { newPasscode = it; error = null },
                    label = { Text(stringResource(R.string.locked_passcode_new_label)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPasscode,
                    onValueChange = { confirmPasscode = it; error = null },
                    label = { Text(stringResource(R.string.locked_passcode_confirm_label)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = error != null,
                    supportingText = error?.let { { Text(stringResource(it)) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    newPasscode.length < MIN_PASSCODE_LENGTH ->
                        error = R.string.locked_passcode_too_short
                    newPasscode != confirmPasscode ->
                        error = R.string.locked_passcode_mismatch
                    else -> onConfirm(newPasscode)
                }
            }) {
                Text(stringResource(R.string.locked_passcode_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private const val MIN_PASSCODE_LENGTH = 4
