package com.kaamo.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.kaamo.app.legal.DateInput
import com.kaamo.app.model.CevUse
import com.kaamo.app.model.CustomerRegistration
import com.kaamo.app.model.ServiceCategory
import com.kaamo.app.model.NoticeLanguage
import com.kaamo.app.model.PartnerRegistration
import com.kaamo.app.model.UserRole
import java.time.Instant

/** Holds the multi-step onboarding draft so it survives navigation between steps. */
class OnboardingViewModel : ViewModel() {
    var role by mutableStateOf(UserRole.Customer)
    var language by mutableStateOf(NoticeLanguage.English)
    var mobile by mutableStateOf("")

    // Shared by customer ("location") and partner ("home village")
    var name by mutableStateOf("")
    var village by mutableStateOf("")

    // Partner draft
    var district by mutableStateOf("")
    var machine by mutableStateOf<ServiceCategory?>(null)
    var subCategories by mutableStateOf(emptyList<String>())
    var cevUse by mutableStateOf<CevUse?>(null)
    var serviceAreas by mutableStateOf(emptyList<String>())
    var workLatitude by mutableStateOf<Double?>(null)
    var workLongitude by mutableStateOf<Double?>(null)
    var indicativeRate by mutableStateOf("")
    var rcNumber by mutableStateOf("")
    var insuranceText by mutableStateOf("")
    var fitnessText by mutableStateOf("")
    var declarationAccepted by mutableStateOf(false)

    /** Default OFF. The listing stays hidden until the partner opts in. */
    var phonePublic by mutableStateOf(false)
    var lastConsentChange by mutableStateOf<Instant?>(null)

    fun selectMachine(type: ServiceCategory) {
        if (machine == type) return
        machine = type
        subCategories = emptyList()
        cevUse = null
        declarationAccepted = false // the declaration text depends on the machine type
    }

    fun toCustomerRegistration() = CustomerRegistration(name.trim(), mobile, village.trim())

    fun toPartnerRegistration() = PartnerRegistration(
        fullName = name.trim(),
        mobile = mobile,
        homeVillage = village.trim(),
        district = district.trim(),
        machine = requireNotNull(machine),
        subCategories = subCategories,
        cevUse = cevUse,
        serviceAreas = serviceAreas,
        indicativeRate = indicativeRate.trim().ifEmpty { null },
        rcNumber = rcNumber.trim().ifEmpty { null },
        insuranceValidUntil = DateInput.parse(insuranceText),
        fitnessValidUntil = DateInput.parse(fitnessText),
        phonePublic = phonePublic,
        noticeLanguage = language.code,
        latitude = workLatitude,
        longitude = workLongitude,
    )
}
