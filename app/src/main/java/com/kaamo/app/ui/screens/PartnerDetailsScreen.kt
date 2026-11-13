package com.kaamo.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kaamo.app.legal.ProhibitedTerms
import com.kaamo.app.location.rememberLocationRequester
import com.kaamo.app.model.CevUse
import com.kaamo.app.model.ServiceCategory
import com.kaamo.app.model.NoticeLanguage
import com.kaamo.app.ui.OnboardingViewModel
import com.kaamo.app.ui.components.CheckOption
import com.kaamo.app.ui.components.FormScaffold
import com.kaamo.app.ui.components.GradientButton
import com.kaamo.app.ui.components.KaamoTextField
import com.kaamo.app.ui.components.NoticeCard
import com.kaamo.app.ui.components.RadioOption
import com.kaamo.app.ui.components.RemovableChip
import com.kaamo.app.ui.components.SelectableCard
import com.kaamo.app.ui.theme.KaamoColors

/** Step 2 of 4: basic details, primary machine, conditional sub-categories, service area, optional rate. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PartnerDetailsScreen(
    vm: OnboardingViewModel,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    var areaInput by rememberSaveable { mutableStateOf("") }
    var areaError by rememberSaveable { mutableStateOf<String?>(null) }
    val machine = vm.machine

    fun addArea() {
        val value = areaInput.trim()
        if (value.isEmpty()) return
        if (ProhibitedTerms.isFlagged(value)) {
            areaError = "Listings can't mention sand, bricks, earth or other construction-material haulage."
            return
        }
        areaError = null
        if (vm.serviceAreas.none { it.equals(value, ignoreCase = true) }) {
            vm.serviceAreas = vm.serviceAreas + value
        }
        areaInput = ""
    }

    val valid = vm.name.isNotBlank() && vm.village.isNotBlank() && vm.district.isNotBlank() &&
        machine != null &&
        (machine != ServiceCategory.Jcb || vm.cevUse != null) &&
        (vm.serviceAreas.isNotEmpty() || areaInput.isNotBlank())

    FormScaffold(
        title = "Your machine or service",
        subtitle = "People nearby will find and call you.",
        onBack = onBack,
        step = 2,
        footer = {
            GradientButton(
                text = "Continue",
                enabled = valid,
                onClick = {
                    if (areaInput.isNotBlank()) addArea()
                    if (areaError == null && vm.serviceAreas.isNotEmpty()) onContinue()
                },
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
            value = vm.village,
            onValueChange = { vm.village = it },
            label = "Village / area",
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
        )
        Spacer(Modifier.height(16.dp))
        KaamoTextField(
            value = vm.district,
            onValueChange = { vm.district = it },
            label = "City / District",
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        )

        Spacer(Modifier.height(16.dp))
        var gpsMessage by rememberSaveable { mutableStateOf<String?>(null) }
        val requestLocation = rememberLocationRequester { fix ->
            if (fix != null) {
                vm.workLatitude = fix.latitude
                vm.workLongitude = fix.longitude
                gpsMessage = "Location added."
            } else {
                gpsMessage = "Couldn't get your location. Turn on location and allow permission, or skip this."
            }
        }
        NoticeCard(
            title = "Show your distance to customers (optional)",
            text = "Kaamo can use your phone's GPS once to work out how far you are from people searching. " +
                "Only an approximate position (about 1 km) is stored, and it is never shown to anyone.",
        )
        TextButton(onClick = requestLocation) {
            Text(if (vm.workLatitude != null) "Update my location" else "Use my current location")
        }
        gpsMessage?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = KaamoColors.Muted) }

        Spacer(Modifier.height(32.dp))
        Text("What do you offer?", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        ServiceCategory.entries.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                pair.forEach { type ->
                    SelectableCard(
                        selected = machine == type,
                        onClick = { vm.selectMachine(type) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        contentPadding = 16.dp,
                    ) {
                        Text(type.label, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // ---- Conditional blocks ----
        when (machine) {
            ServiceCategory.Tractor -> {
                Spacer(Modifier.height(8.dp))
                NoticeCard(
                    title = "Farm work only",
                    text = "Tractor listings are for farm operations such as tillage, sowing, harvesting and threshing. " +
                        "Construction-material haulage (sand, bricks, earth) is not allowed.",
                )
            }
            ServiceCategory.Transport -> {
                Spacer(Modifier.height(8.dp))
                NoticeCard(
                    title = "Goods transport",
                    text = "Only list a vehicle that is registered, permitted and insured for goods carriage.",
                )
            }
            else -> Unit
        }

        if (machine != null && machine.subCategories.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text("Services you provide", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            machine.subCategories.forEach { option ->
                CheckOption(
                    label = option,
                    checked = option in vm.subCategories,
                    onCheckedChange = { checked ->
                        vm.subCategories = if (checked) vm.subCategories + option else vm.subCategories - option
                    },
                )
            }
        }

        if (machine == ServiceCategory.Jcb) {
            Spacer(Modifier.height(20.dp))
            Text("How is your machine hired?", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            CevUse.entries.forEach { use ->
                RadioOption(label = use.label, selected = vm.cevUse == use, onClick = { vm.cevUse = use })
            }
        }

        // ---- Service area ----
        Spacer(Modifier.height(28.dp))
        Text("Where do you work?", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text("Add the villages or regions you cover.", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        KaamoTextField(
            value = areaInput,
            onValueChange = { areaInput = it; areaError = null },
            label = "Village or region",
            isError = areaError != null,
            supportingText = areaError,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { addArea() }),
            trailingIcon = {
                TextButton(onClick = { addArea() }, enabled = areaInput.isNotBlank()) {
                    Text("Add", color = if (areaInput.isNotBlank()) KaamoColors.Forest else KaamoColors.Muted)
                }
            },
        )
        if (vm.serviceAreas.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                vm.serviceAreas.forEach { area ->
                    RemovableChip(label = area, onRemove = { vm.serviceAreas = vm.serviceAreas - area })
                }
            }
        }

        // ---- Optional indicative rate (never set or suggested by Kaamo) ----
        if (machine != null) {
            Spacer(Modifier.height(28.dp))
            Text("Owner's indicative rate (optional)", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Negotiate directly with customers. Kaamo does not set, suggest or collect rates.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))
            KaamoTextField(
                value = vm.indicativeRate,
                onValueChange = { if (it.length <= 12) vm.indicativeRate = it },
                label = "Rate",
                prefix = "₹ ",
                suffix = " / hour",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            )
        }
    }
}
