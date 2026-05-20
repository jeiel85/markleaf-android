package com.markleaf.notes.feature.lock

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.markleaf.notes.R

/**
 * Gates the wrapped [content] behind a [BiometricPrompt] when [enabled] is true.
 *
 * Why this is local-first safe: BiometricPrompt is part of AOSP / androidx and runs
 * entirely on-device. We never read the credential itself — only an authenticated /
 * not-authenticated signal. Notes remain in the same Room DB; lock only hides the UI.
 */
@Composable
fun BiometricLockGate(
    enabled: Boolean,
    content: @Composable () -> Unit
) {
    if (!enabled) {
        content()
        return
    }

    val context = LocalContext.current
    val activity = context.findFragmentActivity()
    var unlocked by rememberSaveable { mutableStateOf(false) }
    var lastError by remember { mutableStateOf<String?>(null) }
    val canUseBiometric = remember(context) { context.canUseBiometric() }

    if (unlocked) {
        content()
        return
    }

    val promptTitle = stringResource(R.string.biometric_lock_prompt_title)
    val promptSubtitle = stringResource(R.string.biometric_lock_prompt_subtitle)
    val cancelLabel = stringResource(R.string.biometric_lock_cancel)
    val unavailableMessage = stringResource(R.string.biometric_lock_unavailable)

    LaunchedEffect(activity, canUseBiometric) {
        if (activity != null && canUseBiometric) {
            showPrompt(
                activity = activity,
                title = promptTitle,
                subtitle = promptSubtitle,
                cancelLabel = cancelLabel,
                onSuccess = { unlocked = true },
                onError = { msg -> lastError = msg }
            )
        } else if (!canUseBiometric) {
            // Hardware/enrollment missing — fail open rather than locking the user out
            // forever. The settings screen explains how to re-enable.
            lastError = unavailableMessage
            unlocked = true
        }
    }

    LockScaffold(
        message = lastError ?: stringResource(R.string.biometric_lock_locked_message),
        onRetry = {
            if (activity != null && canUseBiometric) {
                showPrompt(
                    activity = activity,
                    title = promptTitle,
                    subtitle = promptSubtitle,
                    cancelLabel = cancelLabel,
                    onSuccess = { unlocked = true },
                    onError = { msg -> lastError = msg }
                )
            }
        }
    )
}

@Composable
private fun LockScaffold(message: String, onRetry: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = onRetry) {
                    Text(stringResource(R.string.biometric_lock_retry))
                }
            }
        }
    }
}

private fun showPrompt(
    activity: FragmentActivity,
    title: String,
    subtitle: String,
    cancelLabel: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val executor = androidx.core.content.ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onError(errString.toString())
            }
        }
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setNegativeButtonText(cancelLabel)
        .setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.BIOMETRIC_STRONG
        )
        .build()
    prompt.authenticate(info)
}

fun Context.canUseBiometric(): Boolean {
    val manager = BiometricManager.from(this)
    val result = manager.canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.BIOMETRIC_STRONG
    )
    return result == BiometricManager.BIOMETRIC_SUCCESS
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is android.content.ContextWrapper -> this.baseContext.findFragmentActivity()
    else -> null
}
