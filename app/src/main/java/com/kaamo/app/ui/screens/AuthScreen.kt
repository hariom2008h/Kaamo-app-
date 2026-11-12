package com.kaamo.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kaamo.app.model.UserRole
import com.kaamo.app.ui.components.ConsentCheckbox
import com.kaamo.app.ui.components.FormScaffold
import com.kaamo.app.ui.components.GradientButton
import com.kaamo.app.ui.components.KaamoTextField
import com.kaamo.app.ui.theme.KaamoColors

/**
 * Step 1: sign in with a mobile OTP and accept the general Terms + Privacy Notice.
 * The checkbox starts unticked. For partners, accepting terms does NOT make the number public;
 * that is a separate, later choice.
 */
@Composable
fun AuthScreen(
    role: UserRole,
    onBack: () -> Unit,
    onSendOtp: (mobile: String) -> Unit,
    onVerify: (mobile: String, otp: String, done: (Boolean) -> Unit) -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    var mobile by rememberSaveable { mutableStateOf("") }
    var otp by rememberSaveable { mutableStateOf("") }
    var otpSent by rememberSaveable { mutableStateOf(false) }
    var accepted by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var otpError by remember { mutableStateOf(false) }

    FormScaffold(
        title = "Confirm your mobile number",
        subtitle = "We'll send a one-time password by SMS.",
        onBack = onBack,
        step = if (role == UserRole.Partner) 1 else null,
        footer = {
            if (!otpSent) {
                GradientButton(
                    text = "Send OTP",
                    enabled = mobile.length == 10,
                    onClick = {
                        onSendOtp(mobile)
                        otpSent = true
                    },
                )
            } else {
                GradientButton(
                    text = if (busy) "Checking…" else "Confirm and continue",
                    enabled = otp.length == 6 && accepted && !busy,
                    onClick = {
                        busy = true
                        otpError = false
                        onVerify(mobile, otp) { ok ->
                            busy = false
                            otpError = !ok
                        }
                    },
                )
            }
        },
    ) {
        KaamoTextField(
            value = mobile,
            onValueChange = { if (it.length <= 10 && it.all(Char::isDigit)) mobile = it },
            label = "Mobile number",
            prefix = "+91  ",
            readOnly = otpSent,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
            trailingIcon = if (otpSent) {
                {
                    TextButton(onClick = { otpSent = false; otp = ""; accepted = false }) {
                        Text("Change", color = KaamoColors.Forest)
                    }
                }
            } else null,
        )

        if (otpSent) {
            Spacer(Modifier.height(16.dp))
            KaamoTextField(
                value = otp,
                onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) { otp = it; otpError = false } },
                label = "6-digit OTP",
                isError = otpError,
                supportingText = if (otpError) "That code didn't work. Check it and try again." else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
            )
            TextButton(onClick = { onSendOtp(mobile) }) {
                Text("Resend OTP", color = KaamoColors.Forest)
            }

            Spacer(Modifier.height(20.dp))
            Column {
                ConsentCheckbox(
                    checked = accepted,
                    onCheckedChange = { accepted = it },
                    text = "I agree to the Terms of Service and the Privacy Notice.",
                    links = listOf("Terms of Service" to onOpenTerms, "Privacy Notice" to onOpenPrivacy),
                )
                if (role == UserRole.Partner) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "This does not make your mobile number public. You'll choose that separately in the last step.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
