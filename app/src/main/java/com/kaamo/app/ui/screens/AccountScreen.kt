package com.kaamo.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kaamo.app.legal.LegalConfig
import com.kaamo.app.legal.LegalCopy
import com.kaamo.app.model.NoticeLanguage
import com.kaamo.app.model.UserRole
import com.kaamo.app.ui.components.KaamoBottomBar
import com.kaamo.app.ui.components.KaamoSwitch
import com.kaamo.app.ui.components.LanguageChip
import com.kaamo.app.ui.components.OutlineButton
import com.kaamo.app.ui.theme.KaamoColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Account: privacy controls (partners), notice language, legal documents and the Grievance Officer.
 * Withdrawing consent is ONE tap with no survey, no verification and no review.
 */
@Composable
fun AccountScreen(
    role: UserRole,
    language: NoticeLanguage,
    onLanguageChange: (NoticeLanguage) -> Unit,
    onRequestLanguage: () -> Unit,
    phonePublic: Boolean,
    lastConsentChange: Instant?,
    onPhonePublicChange: (Boolean) -> Unit,
    onRemoveListing: () -> Unit,
    onOpenDirectory: () -> Unit,
) {
    val context = LocalContext.current
    var confirmRemove by remember { mutableStateOf(false) }
    var showFullNotice by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)

    fun open(url: String) = context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    fun mail(subject: String) = context.startActivity(
        Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${LegalConfig.GRIEVANCE_EMAIL}?subject=${Uri.encode(subject)}")),
    )

    Column(Modifier.fillMaxSize().background(KaamoColors.White).statusBarsPadding()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Text("Account", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(24.dp))

            // ---- Privacy (partners) ----
            if (role == UserRole.Partner) {
                SectionLabel("Privacy")
                Column(Modifier.fillMaxWidth().border(1.dp, KaamoColors.Border, shape).padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("Show my mobile number", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (phonePublic) "Customers can call you from the directory."
                                else "Your number is hidden and your listing is not shown.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        KaamoSwitch(checked = phonePublic, onCheckedChange = onPhonePublicChange)
                    }
                    if (lastConsentChange != null) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Last changed ${formatTime(lastConsentChange)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = KaamoColors.Muted,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlineButton(text = "Remove my listing", onClick = { confirmRemove = true })
                }
                Spacer(Modifier.height(28.dp))
            }

            // ---- Language ----
            SectionLabel("Notice language")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Read legal notices in your language",
                    style = MaterialTheme.typography.bodyLarge,
                    color = KaamoColors.Ink,
                    modifier = Modifier.weight(1f),
                )
                LanguageChip(language, onLanguageChange, onRequestLanguage)
            }
            Spacer(Modifier.height(28.dp))

            // ---- Legal ----
            SectionLabel("Legal")
            LinkRow("Terms of Service") { open(LegalConfig.TERMS_URL) }
            LinkRow("Privacy Notice") { open(LegalConfig.PRIVACY_URL) }
            LinkRow(if (showFullNotice) "Hide: what Kaamo is" else "What Kaamo is, and is not") {
                showFullNotice = !showFullNotice
            }
            if (showFullNotice) {
                Text(
                    LegalCopy.DIRECTORY_ONLY_FULL,
                    style = MaterialTheme.typography.bodyMedium,
                    color = KaamoColors.Ink,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
            Spacer(Modifier.height(28.dp))

            // ---- Grievance Officer (IT Rules 2021, Rule 3(2)) ----
            SectionLabel("Grievance Officer")
            Column(Modifier.fillMaxWidth().border(1.dp, KaamoColors.Border, shape).padding(20.dp)) {
                Text(LegalConfig.GRIEVANCE_OFFICER, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(LegalConfig.GRIEVANCE_ADDRESS, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(2.dp))
                Text(LegalConfig.GRIEVANCE_EMAIL, style = MaterialTheme.typography.bodyMedium, color = KaamoColors.Forest)
                Spacer(Modifier.height(12.dp))
                Text(LegalCopy.GRIEVANCE_SLA, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                OutlineButton(text = "Report a listing or raise a complaint", onClick = { mail("Complaint / report a listing") })
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "Terms ${LegalConfig.TERMS_VERSION} · Notice ${LegalConfig.NOTICE_VERSION}",
                style = MaterialTheme.typography.labelMedium,
                color = KaamoColors.Muted,
            )
            Spacer(Modifier.height(24.dp))
        }
        KaamoBottomBar(selectedIndex = 1, onDirectory = onOpenDirectory, onAccount = {})
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            containerColor = KaamoColors.White,
            title = { Text("Remove your listing?", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    "Your listing and phone visibility will be switched off now. Kaamo may keep limited records " +
                        "the law requires, such as consent history and complaint records.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmRemove = false; onRemoveListing() }) {
                    Text("Remove", color = KaamoColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = false }) { Text("Cancel", color = KaamoColors.Muted) }
            },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, color = KaamoColors.Muted)
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun LinkRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = KaamoColors.Ink)
    }
}

private fun formatTime(instant: Instant): String =
    DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a").withZone(ZoneId.systemDefault()).format(instant)
