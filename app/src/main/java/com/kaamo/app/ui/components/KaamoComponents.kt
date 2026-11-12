package com.kaamo.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaamo.app.AppConfig
import com.kaamo.app.R
import com.kaamo.app.model.NoticeLanguage
import com.kaamo.app.ui.theme.KaamoColors
import com.kaamo.app.ui.theme.KaamoGradient

private val CardShape = RoundedCornerShape(16.dp) // rounded-2xl

/** Logo mark. Asset: res/drawable/logo_kaamo.xml */
@Composable
fun KaamoLogo(modifier: Modifier = Modifier, size: Dp = 72.dp) {
    Image(
        painter = painterResource(R.drawable.logo_kaamo),
        contentDescription = "Kaamo",
        modifier = modifier.size(size),
    )
}

/** Splash logo: the mark plus a text wordmark (no image asset, so the name is changed in AppConfig). */
@Composable
fun KaamoFullLogo(modifier: Modifier = Modifier, size: Dp = 220.dp) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(R.drawable.logo_kaamo),
            contentDescription = null,
            modifier = Modifier.size(size * 0.45f),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = AppConfig.NAME,
            style = MaterialTheme.typography.headlineLarge,
            color = KaamoColors.Forest,
            fontSize = (size.value * 0.2f).sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Primary action. Brand gradient when enabled, flat neutral when disabled. */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 56.dp,
) {
    val shape = RoundedCornerShape(16.dp)
    val fill = if (enabled) Modifier.background(KaamoGradient, shape)
    else Modifier.background(KaamoColors.Border, shape)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .then(fill)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) KaamoColors.White else KaamoColors.Muted,
        )
    }
}

/** Secondary action: white with a subtle border. */
@Composable
fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CardShape)
            .border(1.dp, KaamoColors.Border, CardShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = KaamoColors.Ink)
    }
}

@Composable
fun KaamoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    prefix: String? = null,
    suffix: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    readOnly: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        readOnly = readOnly,
        isError = isError,
        shape = CardShape,
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it) } },
        prefix = prefix?.let { { Text(it, color = KaamoColors.Ink) } },
        suffix = suffix?.let { { Text(it, color = KaamoColors.Muted) } },
        supportingText = supportingText?.let { { Text(it) } },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = KaamoColors.Ink),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = KaamoColors.Forest,
            unfocusedBorderColor = KaamoColors.Border,
            errorBorderColor = KaamoColors.Error,
            focusedContainerColor = KaamoColors.White,
            unfocusedContainerColor = KaamoColors.White,
            errorContainerColor = KaamoColors.White,
            cursorColor = KaamoColors.Forest,
            focusedLabelColor = KaamoColors.Forest,
            unfocusedLabelColor = KaamoColors.Muted,
            focusedPlaceholderColor = KaamoColors.Muted,
            unfocusedPlaceholderColor = KaamoColors.Muted,
            focusedLeadingIconColor = KaamoColors.Muted,
            unfocusedLeadingIconColor = KaamoColors.Muted,
            focusedSupportingTextColor = KaamoColors.Muted,
            unfocusedSupportingTextColor = KaamoColors.Muted,
            errorSupportingTextColor = KaamoColors.Error,
        ),
    )
}

/** Single-choice card. Selected state uses the brand gradient border and a faint teal wash. */
@Composable
fun SelectableCard(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val border = if (selected) Modifier.border(1.5.dp, KaamoGradient, CardShape)
    else Modifier.border(1.dp, KaamoColors.Border, CardShape)
    Column(
        modifier = modifier
            .clip(CardShape)
            .background(if (selected) KaamoColors.Teal.copy(alpha = 0.05f) else KaamoColors.White)
            .then(border)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun CheckOption(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KaamoCheckbox(checked)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = KaamoColors.Ink)
    }
}

@Composable
private fun KaamoCheckbox(checked: Boolean) {
    Checkbox(
        checked = checked,
        onCheckedChange = null,
        colors = CheckboxDefaults.colors(
            checkedColor = KaamoColors.Forest,
            uncheckedColor = Color(0xFF9CA3AF),
            checkmarkColor = KaamoColors.White,
        ),
    )
}

/**
 * Click-wrap consent. Always starts UNTICKED. [links] render as underlined, tappable text under the
 * sentence so the user can read the full documents before agreeing.
 */
@Composable
fun ConsentCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    links: List<Pair<String, () -> Unit>> = emptyList(),
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange),
        ) { KaamoCheckbox(checked) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).padding(top = 12.dp)) {
            Text(text, style = MaterialTheme.typography.bodyMedium, color = KaamoColors.Ink)
            if (links.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    links.forEach { (label, onClick) ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = KaamoColors.Forest,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RadioOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = KaamoColors.Forest,
                unselectedColor = Color(0xFF9CA3AF),
            ),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = KaamoColors.Ink,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
fun KaamoSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = KaamoColors.White,
            checkedTrackColor = KaamoColors.Forest,
            checkedBorderColor = KaamoColors.Forest,
            uncheckedThumbColor = KaamoColors.White,
            uncheckedTrackColor = Color(0xFFD1D5DB),
            uncheckedBorderColor = Color(0xFFD1D5DB),
        ),
    )
}

@Composable
fun FilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(50)
    val style = if (selected) Modifier.background(KaamoGradient, shape)
    else Modifier.border(1.dp, KaamoColors.Border, shape)
    Box(
        modifier = modifier
            .clip(shape)
            .then(style)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) KaamoColors.White else KaamoColors.Ink,
        )
    }
}

/** Neutral chip with a remove affordance, used for service-area tags. */
@Composable
fun RemovableChip(label: String, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(shape)
            .border(1.dp, KaamoColors.Border, shape)
            .clickable(role = Role.Button, onClick = onRemove)
            .padding(start = 14.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = KaamoColors.Ink)
        Spacer(Modifier.width(8.dp))
        Text("✕", style = MaterialTheme.typography.labelMedium, color = KaamoColors.Muted)
    }
}

/** Read-only status chip. Deliberately neutral (no green, no gradient): it must never look like an endorsement. */
@Composable
fun StaticChip(label: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(shape)
            .border(1.dp, KaamoColors.Border, shape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = KaamoColors.Muted, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(5.dp))
        }
        Text(label, style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp), color = KaamoColors.Muted)
    }
}

/** Soft information callout used for legal notices. */
@Composable
fun NoticeCard(text: String, modifier: Modifier = Modifier, title: String? = null) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(KaamoColors.Teal.copy(alpha = 0.05f))
            .border(1.dp, KaamoColors.Teal.copy(alpha = 0.20f), shape)
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(Icons.Filled.Info, contentDescription = null, tint = KaamoColors.Forest, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
            }
            Text(text, style = MaterialTheme.typography.bodyMedium, color = KaamoColors.Ink)
        }
    }
}

@Composable
fun StepIndicator(step: Int, total: Int, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text("Step $step of $total", style = MaterialTheme.typography.labelMedium, color = KaamoColors.Muted)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(total) { index ->
                val segment = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(50))
                Box(if (index < step) segment.background(KaamoGradient) else segment.background(KaamoColors.Border))
            }
        }
    }
}

/** Notice-language selector (DPDP s.5(3)). Launch languages: English + Hindi, plus a request path for others. */
@Composable
fun LanguageChip(
    selected: NoticeLanguage,
    onSelected: (NoticeLanguage) -> Unit,
    onRequestOther: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(50)
    Box(modifier) {
        Row(
            modifier = Modifier
                .clip(shape)
                .border(1.dp, KaamoColors.Border, shape)
                .clickable(role = Role.Button) { open = true }
                .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(selected.label, style = MaterialTheme.typography.labelMedium, color = KaamoColors.Ink)
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = "Change language",
                tint = KaamoColors.Muted,
                modifier = Modifier.size(20.dp),
            )
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier.background(KaamoColors.White),
        ) {
            NoticeLanguage.entries.forEach { language ->
                DropdownMenuItem(
                    text = { Text(language.label, color = KaamoColors.Ink) },
                    onClick = { open = false; onSelected(language) },
                )
            }
            HorizontalDivider(color = KaamoColors.Border)
            DropdownMenuItem(
                text = { Text("Request in your language", color = KaamoColors.Forest) },
                onClick = { open = false; onRequestOther() },
            )
        }
    }
}
