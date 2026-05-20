package com.markleaf.notes.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R

private data class OnboardingPage(
    val titleRes: Int,
    val bodyRes: Int
)

private val pages = listOf(
    OnboardingPage(R.string.onboarding_title_welcome, R.string.onboarding_body_welcome),
    OnboardingPage(R.string.onboarding_title_markdown, R.string.onboarding_body_markdown),
    OnboardingPage(R.string.onboarding_title_tags, R.string.onboarding_body_tags),
    OnboardingPage(R.string.onboarding_title_local, R.string.onboarding_body_local)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeOnboardingSheet(
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var pageIndex by rememberSaveable { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            AnimatedContent(
                targetState = pageIndex,
                transitionSpec = {
                    (fadeIn() togetherWith fadeOut()).using(SizeTransform(clip = false))
                },
                label = "onboarding_page"
            ) { index ->
                val page = pages[index]
                Column {
                    Text(
                        text = stringResource(page.titleRes),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(page.bodyRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                pages.indices.forEach { i ->
                    val selected = i == pageIndex
                    val color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }
                    Surface(
                        shape = CircleShape,
                        color = color,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (selected) 10.dp else 8.dp)
                    ) { Box(Modifier) }
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.onboarding_skip))
                }
                Button(onClick = {
                    if (pageIndex < pages.lastIndex) {
                        pageIndex += 1
                    } else {
                        onDismiss()
                    }
                }) {
                    Text(
                        if (pageIndex == pages.lastIndex) {
                            stringResource(R.string.onboarding_done)
                        } else {
                            stringResource(R.string.onboarding_next)
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
