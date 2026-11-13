package com.kaamo.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.kaamo.app.AppConfig
import com.kaamo.app.data.ContactRepository
import com.kaamo.app.data.ReportReason
import com.kaamo.app.location.DeviceLocation
import com.kaamo.app.location.rememberLocationRequester
import com.kaamo.app.data.RevealResult
import com.kaamo.app.legal.LegalCopy
import com.kaamo.app.model.ServiceCategory
import com.kaamo.app.model.Partner
import com.kaamo.app.ui.components.CheckOption
import com.kaamo.app.ui.components.FilterPill
import com.kaamo.app.ui.components.GradientButton
import com.kaamo.app.ui.components.KaamoBottomBar
import com.kaamo.app.ui.components.KaamoLogo
import com.kaamo.app.ui.components.RadioOption
import com.kaamo.app.ui.components.KaamoTextField
import com.kaamo.app.ui.components.StaticChip
import com.kaamo.app.ui.theme.KaamoColors
import kotlinx.coroutines.launch

/**
 * Directory. Neutral by design: filters and distance only, no ranking, no "recommended", no badges that
 * imply Kaamo checked anything, no booking. Full phone numbers are never in the list payload.
 */
@Composable
fun DirectoryScreen(
    partners: List<Partner>,
    hasLocation: Boolean,
    onLocation: (DeviceLocation?) -> Unit,
    contacts: ContactRepository,
    onOpenAccount: () -> Unit,
    onReport: (partnerId: String, reason: ReportReason) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf<ServiceCategory?>(null) } // null = All
    var pending by remember { mutableStateOf<Partner?>(null) }
    var busy by remember { mutableStateOf(false) }
    var reporting by remember { mutableStateOf<Partner?>(null) }

    val requestLocation = rememberLocationRequester { fix ->
        onLocation(fix)
        if (fix == null) {
            scope.launch { snackbar.showSnackbar("Couldn't get your location. Turn on location and allow permission.") }
        }
    }

    val results = remember(partners, query, filter) {
        partners
            .filter { (filter == null || it.machine == filter) && it.matches(query) }
            .sortedWith(compareBy<Partner>({ it.distanceKm ?: Int.MAX_VALUE }, { it.name })) // only ordering: distance, then name
    }

    Box(Modifier.fillMaxSize().background(KaamoColors.White)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KaamoLogo(size = 28.dp)
                Spacer(Modifier.width(10.dp))
                Text(AppConfig.NAME, style = MaterialTheme.typography.titleMedium)
            }
            Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                Text("What work do you need?", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(4.dp))
                Text("आपको क्या काम है?", style = MaterialTheme.typography.bodyLarge)
            }

            KaamoTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.padding(horizontal = 24.dp),
                placeholder = "Search plumber, JCB, borewell, workers, area...",
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )
            Spacer(Modifier.height(16.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { FilterPill("All", selected = filter == null, onClick = { filter = null }) }
                items(ServiceCategory.entries) { type ->
                    FilterPill(type.filterLabel, selected = filter == type, onClick = { filter = type })
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (results.any { it.distanceKm != null }) "Sorted by distance from you" else "Listed alphabetically",
                    style = MaterialTheme.typography.labelMedium,
                    color = KaamoColors.Muted,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = requestLocation) {
                    Text(if (hasLocation) "Update my location" else "Use my live location")
                }
            }
            if (!hasLocation) {
                Text(
                    text = "Your GPS position is used only to work out distance. It is not stored.",
                    style = MaterialTheme.typography.labelMedium,
                    color = KaamoColors.Muted,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (results.isEmpty()) {
                    item {
                        Text(
                            text = "No partners found. Try a different search or filter.",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 32.dp),
                        )
                    }
                } else {
                    items(results, key = { it.id }) { partner ->
                        PartnerCard(partner = partner, onCall = { pending = partner }, onReport = { reporting = partner })
                    }
                }
            }

            Text(
                text = LegalCopy.DIRECTORY_ONLY_SHORT,
                style = MaterialTheme.typography.labelMedium,
                color = KaamoColors.Muted,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp),
            )
            KaamoBottomBar(selectedIndex = 0, onDirectory = {}, onAccount = onOpenAccount)
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp),
        )
    }

    reporting?.let { partner ->
        ReportSheet(
            partnerName = partner.name,
            onDismiss = { reporting = null },
            onSubmit = { reason ->
                reporting = null
                onReport(partner.id, reason)
                scope.launch { snackbar.showSnackbar("Report received. We acknowledge every report within 24 hours.") }
            },
        )
    }

    pending?.let { partner ->
        PreCallSheet(
            partnerName = partner.name,
            busy = busy,
            onDismiss = { if (!busy) pending = null },
            onConfirm = {
                scope.launch {
                    busy = true
                    val result = contacts.reveal(partner.id)
                    busy = false
                    pending = null
                    when (result) {
                        is RevealResult.Success -> context.startActivity(
                            Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", result.phoneNumber, null)),
                        )
                        RevealResult.RateLimited ->
                            snackbar.showSnackbar("You've reached the hourly call limit. Please try again later.")
                        RevealResult.ListingUnavailable ->
                            snackbar.showSnackbar("This partner is no longer listed.")
                        RevealResult.SignInRequired ->
                            snackbar.showSnackbar("Please sign in to call a partner.")
                        RevealResult.NetworkError ->
                            snackbar.showSnackbar("Couldn't connect. Check your internet and try again.")
                    }
                }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PartnerCard(partner: Partner, onCall: () -> Unit, onReport: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(KaamoColors.White)
            .border(1.dp, KaamoColors.Border, shape)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(partner.name, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(partner.machine.label, style = MaterialTheme.typography.bodyMedium, color = KaamoColors.Ink)
                if (partner.subCategories.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(partner.subCategories.joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
                }
            }
            Text(
                text = "Report",
                style = MaterialTheme.typography.labelMedium,
                color = KaamoColors.Muted,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClick = onReport)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = KaamoColors.Muted, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                text = partner.distanceKm?.let { "${partner.village} · $it km away" } ?: partner.village,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Phone, contentDescription = null, tint = KaamoColors.Muted, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(partner.maskedPhone, style = MaterialTheme.typography.bodyMedium, color = KaamoColors.Ink)
        }

        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (partner.phoneVerifiedAccount) StaticChip("Phone verified account", icon = Icons.Filled.CheckCircle)
            StaticChip("Self-declared by provider", icon = Icons.Filled.Info)
        }

        if (partner.indicativeRate != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Indicative rate: ${partner.indicativeRate} (negotiate directly)",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(Modifier.height(16.dp))
        GradientButton(text = "Call Partner", onClick = onCall, height = 48.dp)
    }
}

/** Shown before EVERY call. The call button stays disabled until the user acknowledges. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreCallSheet(
    partnerName: String,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    var acknowledged by remember { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = KaamoColors.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Text("Before you call $partnerName", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            Text(LegalCopy.PRECALL_EN, style = MaterialTheme.typography.bodyLarge, color = KaamoColors.Ink)
            Spacer(Modifier.height(10.dp))
            Text(LegalCopy.PRECALL_HI, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            CheckOption(
                label = LegalCopy.PRECALL_CHECK_EN,
                checked = acknowledged,
                onCheckedChange = { acknowledged = it },
            )
            Text(
                text = LegalCopy.PRECALL_CHECK_HI,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 48.dp),
            )
            Spacer(Modifier.height(20.dp))
            GradientButton(
                text = if (busy) "Connecting…" else "Call",
                enabled = acknowledged && !busy,
                onClick = onConfirm,
            )
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), enabled = !busy) {
                Text("Cancel", color = KaamoColors.Muted)
            }
        }
    }
}

/** One-tap report: the reason goes to the grievance queue, which can pause the listing and keep evidence. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportSheet(
    partnerName: String,
    onDismiss: () -> Unit,
    onSubmit: (ReportReason) -> Unit,
) {
    var reason by remember { mutableStateOf<ReportReason?>(null) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = KaamoColors.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Text("Report $partnerName", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text("What is wrong with this listing?", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            ReportReason.entries.forEach { option ->
                RadioOption(label = option.label, selected = reason == option, onClick = { reason = option })
            }
            Spacer(Modifier.height(16.dp))
            GradientButton(text = "Submit report", enabled = reason != null, onClick = { reason?.let(onSubmit) })
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel", color = KaamoColors.Muted)
            }
        }
    }
}
