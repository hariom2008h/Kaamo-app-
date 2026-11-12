package com.kaamo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kaamo.app.model.NoticeLanguage
import com.kaamo.app.ui.theme.KaamoColors

/**
 * Shared layout for form screens: back + logo (or language chip) header, optional step indicator,
 * scrolling body, and a pinned footer (primary action).
 */
@Composable
fun FormScaffold(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    step: Int? = null,
    totalSteps: Int = 4,
    language: NoticeLanguage? = null,
    onLanguageChange: (NoticeLanguage) -> Unit = {},
    onRequestLanguage: () -> Unit = {},
    footer: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KaamoColors.White)
            .statusBarsPadding()
            .imePadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = KaamoColors.Ink)
            }
            if (language != null) {
                LanguageChip(
                    selected = language,
                    onSelected = onLanguageChange,
                    onRequestOther = onRequestLanguage,
                    modifier = Modifier.padding(end = 12.dp),
                )
            } else {
                KaamoLogo(size = 36.dp, modifier = Modifier.padding(end = 12.dp))
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            if (step != null) {
                StepIndicator(step = step, total = totalSteps)
                Spacer(Modifier.height(24.dp))
            } else {
                Spacer(Modifier.height(8.dp))
            }
            Text(title, style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(28.dp))
            content()
            Spacer(Modifier.height(24.dp))
        }

        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 16.dp),
            content = footer,
        )
    }
}

/** Bottom navigation with exactly two destinations. No badges, no promotions. */
@Composable
fun KaamoBottomBar(
    selectedIndex: Int,
    onDirectory: () -> Unit,
    onAccount: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().background(KaamoColors.White).navigationBarsPadding()) {
        Spacer(Modifier.height(1.dp).fillMaxWidth().background(KaamoColors.Border))
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            BottomItem("Directory", selectedIndex == 0, onDirectory, isHome = true, modifier = Modifier.weight(1f))
            BottomItem("Account", selectedIndex == 1, onAccount, isHome = false, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun BottomItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    isHome: Boolean,
    modifier: Modifier = Modifier,
) {
    val tint = if (selected) KaamoColors.Forest else KaamoColors.Muted
    Column(
        modifier = modifier
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = if (isHome) Icons.Filled.Home else Icons.Filled.Person,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
        Text(label, style = MaterialTheme.typography.labelMedium, color = tint)
    }
}
