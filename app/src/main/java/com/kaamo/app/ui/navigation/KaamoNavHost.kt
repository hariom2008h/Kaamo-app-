package com.kaamo.app.ui.navigation

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kaamo.app.data.Backend
import com.kaamo.app.location.DeviceLocation
import com.kaamo.app.legal.LegalConfig
import com.kaamo.app.model.ConsentAction
import com.kaamo.app.model.ConsentEvent
import com.kaamo.app.model.ConsentType
import com.kaamo.app.model.Partner
import com.kaamo.app.model.UserRole
import com.kaamo.app.ui.OnboardingViewModel
import com.kaamo.app.ui.screens.AccountScreen
import com.kaamo.app.ui.screens.AuthScreen
import com.kaamo.app.ui.screens.CustomerDetailsScreen
import com.kaamo.app.ui.screens.DirectoryScreen
import com.kaamo.app.ui.screens.PartnerConsentScreen
import com.kaamo.app.ui.screens.PartnerDeclarationScreen
import com.kaamo.app.ui.screens.PartnerDetailsScreen
import com.kaamo.app.ui.screens.RoleSelectionScreen
import com.kaamo.app.ui.screens.SplashScreen
import kotlinx.coroutines.launch
import java.time.Instant

private object Routes {
    const val SPLASH = "splash"
    const val ROLE = "role"
    const val AUTH = "auth"
    const val CUSTOMER = "customer-details"
    const val PARTNER_DETAILS = "partner-details"
    const val PARTNER_DECLARATION = "partner-declaration"
    const val PARTNER_CONSENT = "partner-consent"
    const val DIRECTORY = "directory"
    const val ACCOUNT = "account"
}

@Composable
fun KaamoNavHost() {
    val nav = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vm: OnboardingViewModel = viewModel()

    // Demo data unless Supabase keys are set in local.properties (see README).
    val services = remember { Backend.create(context) }

    var partners by remember { mutableStateOf(emptyList<Partner>()) }
    var reloadKey by remember { mutableStateOf(0) }
    var userLocation by remember { mutableStateOf<DeviceLocation?>(null) }
    LaunchedEffect(reloadKey, userLocation) { partners = services.partners.listPartners(userLocation) }

    fun record(type: ConsentType, action: ConsentAction, version: String) {
        scope.launch { services.consent.record(ConsentEvent(type, action, version, vm.language.code)) }
    }

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    fun openUrl(url: String) = context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    fun requestLanguage() = context.startActivity(
        Intent(
            Intent.ACTION_SENDTO,
            Uri.parse("mailto:${LegalConfig.GRIEVANCE_EMAIL}?subject=${Uri.encode("Notice language request")}"),
        ),
    )

    fun goToDirectory() {
        reloadKey++
        nav.navigate(Routes.DIRECTORY) {
            popUpTo(Routes.ROLE) { inclusive = true }
            launchSingleTop = true
        }
    }

    fun setPhonePublic(value: Boolean) {
        vm.phonePublic = value
        vm.lastConsentChange = Instant.now()
        record(
            ConsentType.PublicDirectoryPhone,
            if (value) ConsentAction.Granted else ConsentAction.Withdrawn,
            LegalConfig.NOTICE_VERSION,
        )
        scope.launch {
            if (!services.account.setPhoneVisible(value)) toast("Couldn't update. Check your internet.")
        }
    }

    NavHost(
        navController = nav,
        startDestination = Routes.SPLASH,
        enterTransition = { fadeIn(tween(400)) },
        exitTransition = { fadeOut(tween(300)) },
        popEnterTransition = { fadeIn(tween(400)) },
        popExitTransition = { fadeOut(tween(300)) },
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onFinished = { nav.navigate(Routes.ROLE) { popUpTo(Routes.SPLASH) { inclusive = true } } },
            )
        }

        composable(Routes.ROLE) {
            RoleSelectionScreen(
                language = vm.language,
                onLanguageChange = { vm.language = it },
                onRequestLanguage = ::requestLanguage,
                onContinue = { role ->
                    vm.role = role
                    nav.navigate(Routes.AUTH)
                },
            )
        }

        composable(Routes.AUTH) {
            AuthScreen(
                role = vm.role,
                onBack = { nav.popBackStack() },
                onSendOtp = { mobile ->
                    scope.launch {
                        if (!services.auth.sendOtp(mobile)) toast("Couldn't send the OTP. Check the number and internet.")
                    }
                },
                onVerify = { mobile, otp, done ->
                    scope.launch {
                        val ok = services.auth.verifyOtp(mobile, otp)
                        done(ok)
                        if (ok) {
                            vm.mobile = mobile
                            record(ConsentType.TermsAndPrivacy, ConsentAction.Granted, LegalConfig.TERMS_VERSION)
                            nav.navigate(if (vm.role == UserRole.Partner) Routes.PARTNER_DETAILS else Routes.CUSTOMER)
                        }
                    }
                },
                onOpenTerms = { openUrl(LegalConfig.TERMS_URL) },
                onOpenPrivacy = { openUrl(LegalConfig.PRIVACY_URL) },
            )
        }

        composable(Routes.CUSTOMER) {
            CustomerDetailsScreen(
                vm = vm,
                onBack = { nav.popBackStack() },
                onSubmit = {
                    scope.launch {
                        if (services.account.saveCustomer(vm.toCustomerRegistration())) goToDirectory()
                        else toast("Couldn't save. Check your internet and try again.")
                    }
                },
            )
        }

        composable(Routes.PARTNER_DETAILS) {
            PartnerDetailsScreen(
                vm = vm,
                onBack = { nav.popBackStack() },
                onContinue = { nav.navigate(Routes.PARTNER_DECLARATION) },
            )
        }

        composable(Routes.PARTNER_DECLARATION) {
            PartnerDeclarationScreen(
                vm = vm,
                onBack = { nav.popBackStack() },
                onRequestLanguage = ::requestLanguage,
                onContinue = {
                    record(ConsentType.PartnerDeclaration, ConsentAction.Granted, LegalConfig.DECLARATION_VERSION)
                    nav.navigate(Routes.PARTNER_CONSENT)
                },
            )
        }

        composable(Routes.PARTNER_CONSENT) {
            PartnerConsentScreen(
                vm = vm,
                onBack = { nav.popBackStack() },
                onRequestLanguage = ::requestLanguage,
                onFinish = {
                    scope.launch {
                        // The listing stays hidden server-side unless phone_public is true.
                        if (!services.account.savePartner(vm.toPartnerRegistration())) {
                            toast("Couldn't save. Check your internet and try again.")
                            return@launch
                        }
                        if (vm.phonePublic) {
                            vm.lastConsentChange = Instant.now()
                            record(ConsentType.PublicDirectoryPhone, ConsentAction.Granted, LegalConfig.NOTICE_VERSION)
                        }
                        goToDirectory()
                    }
                },
            )
        }

        composable(Routes.DIRECTORY) {
            DirectoryScreen(
                partners = partners,
                hasLocation = userLocation != null,
                onLocation = { userLocation = it },
                contacts = services.contacts,
                onOpenAccount = { nav.navigateSingleTop(Routes.ACCOUNT) },
                onReport = { partnerId, reason ->
                    scope.launch { services.account.report(partnerId, reason, null) }
                },
            )
        }

        composable(Routes.ACCOUNT) {
            AccountScreen(
                role = vm.role,
                language = vm.language,
                onLanguageChange = { vm.language = it },
                onRequestLanguage = ::requestLanguage,
                phonePublic = vm.phonePublic,
                lastConsentChange = vm.lastConsentChange,
                onPhonePublicChange = ::setPhonePublic,
                onRemoveListing = {
                    setPhonePublic(false)
                    scope.launch {
                        if (services.account.removeListing()) toast("Your listing has been removed.")
                        else toast("Couldn't remove the listing. Check your internet.")
                    }
                },
                onOpenDirectory = { nav.popBackStack() },
            )
        }
    }
}

private fun NavController.navigateSingleTop(route: String) =
    navigate(route) { launchSingleTop = true }
