package com.kaamo.app.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kaamo.app.legal.DateInput
import com.kaamo.app.legal.LegalCopy
import com.kaamo.app.model.ServiceCategory
import com.kaamo.app.model.NoticeLanguage
import com.kaamo.app.ui.OnboardingViewModel
import com.kaamo.app.ui.components.ConsentCheckbox
import com.kaamo.app.ui.components.FormScaffold
import com.kaamo.app.ui.components.GradientButton
import com.kaamo.app.ui.components.KaamoTextField
import com.kaamo.app.ui.components.NoticeCard
import com.kaamo.app.ui.theme.KaamoColors

/**
 * Step 3 of 4: document details + owner's self-declaration.
 * Wording on this screen must never imply that Kaamo checked anything.
 */
@Composable
fun PartnerDeclarationScreen(
    vm: OnboardingViewModel,
    onBack: () -> Unit,
    onRequestLanguage: () -> Unit,
    onContinue: () -> Unit,
) {
    val machine = requireNotNull(vm.machine)
    val needsInsurance = machine.isVehicle
    val needsFitness = machine.needsFitness

    val valid = (!needsInsurance || DateInput.isFutureDate(vm.insuranceText)) &&
        (!needsFitness || DateInput.isFutureDate(vm.fitnessText)) &&
        vm.declarationAccepted

    FormScaffold(
        title = if (machine.isVehicle) "Documents and declaration" else "Your declaration",
        subtitle = "Tell customers what you've declared. We don't inspect it.",
        onBack = onBack,
        step = 3,
        language = vm.language,
        onLanguageChange = { vm.language = it },
        onRequestLanguage = onRequestLanguage,
        footer = {
            GradientButton(text = "Continue", enabled = valid, onClick = onContinue)
        },
    ) {
        NoticeCard(
            title = "Self-declared by you",
            text = "Kaamo does not authenticate, inspect or certify your documents or machine. " +
                "Customers are told this on your listing.",
        )

        if (machine.isVehicle) {
            Spacer(Modifier.height(24.dp))
            KaamoTextField(
                value = vm.rcNumber,
                onValueChange = { if (it.length <= 14) vm.rcNumber = it.uppercase() },
                label = "Registration number (optional)",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
        }
        if (needsInsurance) {
            Spacer(Modifier.height(16.dp))
            DateField(
                value = vm.insuranceText,
                onChange = { vm.insuranceText = it },
                label = "Insurance valid until",
            )
        }
        if (needsFitness) {
            Spacer(Modifier.height(16.dp))
            DateField(
                value = vm.fitnessText,
                onChange = { vm.fitnessText = it },
                label = "Fitness certificate valid until",
            )
        }
        if (needsInsurance) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Your listing is hidden automatically on the day a document expires.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(Modifier.height(28.dp))
        Text("Your declaration", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, KaamoColors.Border, RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            LegalCopy.declaration(machine, vm.language).forEachIndexed { index, line ->
                if (index > 0) Spacer(Modifier.height(10.dp))
                Row {
                    Text("•", style = MaterialTheme.typography.bodyMedium, color = KaamoColors.Ink)
                    Spacer(Modifier.width(10.dp))
                    Text(line, style = MaterialTheme.typography.bodyMedium, color = KaamoColors.Ink)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        ConsentCheckbox(
            checked = vm.declarationAccepted,
            onCheckedChange = { vm.declarationAccepted = it },
            text = if (vm.language == NoticeLanguage.Hindi) "मैंने यह घोषणा पढ़ ली है और स्वीकार करता/करती हूँ।"
            else "I have read and accept this declaration.",
        )
    }
}

@Composable
private fun DateField(value: String, onChange: (String) -> Unit, label: String) {
    val showError = value.isNotEmpty() && !DateInput.isFutureDate(value)
    KaamoTextField(
        value = value,
        onValueChange = { if (it.length <= 10 && it.all { c -> c.isDigit() || c == '/' }) onChange(it) },
        label = label,
        placeholder = "DD/MM/YYYY",
        isError = showError,
        supportingText = if (showError) "Enter a future date as DD/MM/YYYY" else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
    )
}
