package com.kaamo.app.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kaamo.app.data.PolicyStatus
import com.kaamo.app.model.NoticeLanguage
import com.kaamo.app.ui.components.ConsentCheckbox
import com.kaamo.app.ui.components.FormScaffold
import com.kaamo.app.ui.components.GradientButton
import com.kaamo.app.ui.components.NoticeCard

/**
 * Shown when the Terms / Privacy Notice version changed, or once a year (IT Rules 2021, Rule 3).
 * Continuing is blocked until the user ticks the (initially unticked) box.
 */
@Composable
fun PolicyUpdateScreen(
    status: PolicyStatus,
    language: NoticeLanguage,
    onBack: () -> Unit,
    onAccept: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    var accepted by rememberSaveable { mutableStateOf(false) }
    val summary = if (language == NoticeLanguage.Hindi) status.summaryHi else status.summaryEn

    FormScaffold(
        title = "Our terms have changed",
        subtitle = if (status.reason == "annual") {
            "Yearly reminder: please review our Terms of Use and Privacy Notice."
        } else {
            "We updated our Terms of Use, Privacy Notice or Rules. Please review the changes."
        },
        onBack = onBack,
        footer = {
            GradientButton(text = "Accept and continue", enabled = accepted, onClick = onAccept)
        },
    ) {
        if (summary.isNotBlank()) {
            NoticeCard(text = summary, title = "What changed")
            Spacer(Modifier.height(16.dp))
        }
        ConsentCheckbox(
            checked = accepted,
            onCheckedChange = { accepted = it },
            text = "I have read and agree to the updated Terms of Use and Privacy Notice.",
            links = listOf("Terms of Use" to onOpenTerms, "Privacy Notice" to onOpenPrivacy),
        )
    }
}
