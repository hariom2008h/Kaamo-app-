package com.kaamo.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kaamo.app.legal.LegalConfig
import com.kaamo.app.legal.LegalCopy
import com.kaamo.app.ui.OnboardingViewModel
import com.kaamo.app.ui.components.FormScaffold
import com.kaamo.app.ui.components.GradientButton
import com.kaamo.app.ui.components.KaamoSwitch
import com.kaamo.app.ui.components.NoticeCard
import com.kaamo.app.ui.theme.KaamoColors

/**
 * Step 4 of 4: the standalone phone-visibility choice (DPDP s.6).
 * - Separate from the Terms checkbox, never pre-selected (default OFF).
 * - The notice is shown here, in the chosen language, before the switch is used.
 * - Turning it off later is exactly as easy: one tap in Account.
 */
@Composable
fun PartnerConsentScreen(
    vm: OnboardingViewModel,
    onBack: () -> Unit,
    onRequestLanguage: () -> Unit,
    onFinish: () -> Unit,
) {
    var noticeOpen by rememberSaveable { mutableStateOf(true) }
    val shape = RoundedCornerShape(16.dp)

    FormScaffold(
        title = "Show your number to customers?",
        subtitle = "This is your choice. You can change it any time.",
        onBack = onBack,
        step = 4,
        language = vm.language,
        onLanguageChange = { vm.language = it },
        onRequestLanguage = onRequestLanguage,
        footer = {
            GradientButton(text = "Finish", onClick = onFinish)
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Notice ${LegalConfig.NOTICE_VERSION} · ${vm.language.label}",
                style = MaterialTheme.typography.labelMedium,
                color = KaamoColors.Muted,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        },
    ) {
        // The switch itself
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, KaamoColors.Border, shape)
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text("Show my mobile number", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Signed-in customers can tap Call Partner to reach you. Your full number never appears in lists.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            KaamoSwitch(checked = vm.phonePublic, onCheckedChange = { vm.phonePublic = it })
        }

        Spacer(Modifier.height(16.dp))
        NoticeCard(
            text = if (vm.phonePublic) {
                "Your listing will be visible in the directory."
            } else {
                "Your listing stays hidden until you turn this on. You can do it later in Account."
            },
        )

        // Collapsible plain-language notice
        Spacer(Modifier.height(24.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .border(1.dp, KaamoColors.Border, shape),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { noticeOpen = !noticeOpen }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "How your number is used",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = if (noticeOpen) "Collapse" else "Expand",
                    tint = KaamoColors.Muted,
                    modifier = Modifier.size(24.dp).rotate(if (noticeOpen) 180f else 0f),
                )
            }
            AnimatedVisibility(visible = noticeOpen) {
                Text(
                    text = LegalCopy.phoneNotice(vm.language),
                    style = MaterialTheme.typography.bodyMedium,
                    color = KaamoColors.Ink,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                )
            }
        }
    }
}
