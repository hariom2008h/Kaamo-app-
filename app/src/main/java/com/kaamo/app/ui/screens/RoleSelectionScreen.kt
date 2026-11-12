package com.kaamo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.kaamo.app.legal.LegalCopy
import com.kaamo.app.model.NoticeLanguage
import com.kaamo.app.model.UserRole
import com.kaamo.app.ui.components.GradientButton
import com.kaamo.app.ui.components.KaamoLogo
import com.kaamo.app.ui.components.LanguageChip
import com.kaamo.app.ui.components.SelectableCard
import com.kaamo.app.ui.theme.KaamoColors
import com.kaamo.app.ui.theme.KaamoGradient

@Composable
fun RoleSelectionScreen(
    language: NoticeLanguage,
    onLanguageChange: (NoticeLanguage) -> Unit,
    onRequestLanguage: () -> Unit,
    onContinue: (UserRole) -> Unit,
) {
    var selected by rememberSaveable { mutableStateOf<UserRole?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KaamoColors.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            KaamoLogo(size = 44.dp)
            LanguageChip(
                selected = language,
                onSelected = onLanguageChange,
                onRequestOther = onRequestLanguage,
            )
        }
        Spacer(Modifier.height(36.dp))

        Text("How would you like to use Kaamo?", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(12.dp))
        Text("Choose one to get started.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(32.dp))

        RoleCard(
            icon = Icons.Filled.Search,
            title = "I need work done",
            subtitle = "Find plumbers, borewell, JCB, workers and more near you.",
            selected = selected == UserRole.Customer,
            onClick = { selected = UserRole.Customer },
        )
        Spacer(Modifier.height(16.dp))
        RoleCard(
            icon = Icons.Filled.Build,
            title = "I want to list my work or machine",
            subtitle = "List what you offer and get calls from people nearby.",
            selected = selected == UserRole.Partner,
            onClick = { selected = UserRole.Partner },
        )

        Spacer(Modifier.weight(1f))
        Text(
            text = LegalCopy.DIRECTORY_ONLY_SHORT,
            style = MaterialTheme.typography.labelMedium,
            color = KaamoColors.Muted,
        )
        Spacer(Modifier.height(16.dp))
        GradientButton(
            text = "Continue",
            enabled = selected != null,
            onClick = { selected?.let(onContinue) },
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun RoleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    SelectableCard(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            val iconShape = RoundedCornerShape(12.dp)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(iconShape)
                    .background(if (selected) KaamoGradient else SolidColor(Color(0xFFF3F4F6)), iconShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) KaamoColors.White else KaamoColors.Ink,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
