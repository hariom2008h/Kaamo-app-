package com.kaamo.app.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.kaamo.app.legal.LegalCopy
import com.kaamo.app.model.maskIndianMobile
import com.kaamo.app.ui.OnboardingViewModel
import com.kaamo.app.ui.components.FormScaffold
import com.kaamo.app.ui.components.GradientButton
import com.kaamo.app.ui.components.KaamoTextField
import com.kaamo.app.ui.components.NoticeCard

@Composable
fun CustomerDetailsScreen(
    vm: OnboardingViewModel,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
) {
    FormScaffold(
        title = "Create your account",
        subtitle = "Just a few details to get started.",
        onBack = onBack,
        footer = {
            GradientButton(
                text = "Get started",
                enabled = vm.name.isNotBlank() && vm.village.isNotBlank(),
                onClick = onSubmit,
            )
        },
    ) {
        KaamoTextField(
            value = vm.name,
            onValueChange = { vm.name = it },
            label = "Full name",
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
        )
        Spacer(Modifier.height(16.dp))
        KaamoTextField(
            value = maskIndianMobile(vm.mobile),
            onValueChange = {},
            label = "Mobile number (confirmed by OTP)",
            readOnly = true,
        )
        Spacer(Modifier.height(16.dp))
        KaamoTextField(
            value = vm.village,
            onValueChange = { vm.village = it },
            label = "Current location / village",
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        )
        Spacer(Modifier.height(24.dp))
        NoticeCard(text = LegalCopy.DIRECTORY_ONLY_SHORT)
    }
}
