package com.kaamo.app.model

import java.time.Instant
import java.time.LocalDate

enum class UserRole { Customer, Partner }

/**
 * What a partner offers. [isVehicle] categories also see the document step (insurance / fitness)
 * and the vehicle-specific declaration; the others (plumber, electrician, workers) only get the
 * general declaration. [code] is the value stored in Supabase (service_category column).
 */
enum class ServiceCategory(
    val code: String,
    val label: String,
    val filterLabel: String,
    val isVehicle: Boolean,
) {
    Plumber("plumber", "Plumber", "Plumbers", false),
    Electrician("electrician", "Electrician", "Electricians", false),
    Construction("construction", "Construction workers", "Workers", false),
    Borewell("borewell", "Borewell", "Borewell", true),
    Jcb("jcb", "JCB / Earthmover", "JCB", true),
    Tractor("tractor", "Tractor", "Tractors", true),
    Transport("transport", "Transport / Trailer", "Transport", true);

    /** Sub-categories shown as checkboxes during partner registration. */
    val subCategories: List<String>
        get() = when (this) {
            Plumber -> listOf("Pipe fitting", "Leak repair", "Bathroom fitting", "Water tank")
            Electrician -> listOf("House wiring", "Fan / light repair", "Inverter / UPS")
            Construction -> listOf("Mason (mistri)", "Labour", "Painter", "Carpenter", "Tiles")
            Borewell -> listOf("Borewell drilling", "Motor / pump repair", "Flushing")
            Jcb -> listOf("Excavation", "Loading", "Demolition", "Leveling")
            Tractor -> listOf("Trolley", "Cultivator", "Water Tanker", "Thresher")
            Transport -> emptyList()
        }

    /** Fitness certificate is asked only for road-going heavy vehicles. */
    val needsFitness: Boolean get() = this == Jcb || this == Transport || this == Borewell

    companion object {
        fun fromCode(code: String?): ServiceCategory? = entries.firstOrNull { it.code == code }
    }
}

/** CMVR Rule 2(cab): on-site hire and low-bed transport of chain excavators are different things. */
enum class CevUse(val label: String) {
    OnSite("On-site machine hire (excavation, demolition or loading at the worksite)"),
    LowBedTransport("Low-bed trailer transport required (chain excavator)"),
}

/** DPDP s.5(3): notice available in English or an Eighth Schedule language. Launch: English + Hindi. */
enum class NoticeLanguage(val code: String, val label: String) {
    English("en", "English"),
    Hindi("hi", "हिन्दी"),
}

enum class ConsentType { TermsAndPrivacy, PartnerDeclaration, PublicDirectoryPhone }
enum class ConsentAction { Granted, Withdrawn }

/** One row of the immutable consent audit trail (user id, IP and user agent are added server-side). */
data class ConsentEvent(
    val type: ConsentType,
    val action: ConsentAction,
    val version: String,
    val languageCode: String,
    val timestampUtc: Instant = Instant.now(),
)

data class CustomerRegistration(
    val fullName: String,
    val mobile: String,
    val location: String,
)

data class PartnerRegistration(
    val fullName: String,
    val mobile: String,
    val homeVillage: String,
    val district: String,
    val machine: ServiceCategory,
    val subCategories: List<String>,
    val cevUse: CevUse?,
    val serviceAreas: List<String>,
    val indicativeRate: String?,
    val rcNumber: String?,
    val insuranceValidUntil: LocalDate?,
    val fitnessValidUntil: LocalDate?,
    val phonePublic: Boolean,
    val noticeLanguage: String,
    /** Optional work location from GPS. The server stores it rounded to about 1 km and never returns it. */
    val latitude: Double? = null,
    val longitude: Double? = null,
)

/**
 * Public directory projection. It deliberately has NO raw phone number: the full number is only
 * returned by [com.kaamo.app.data.ContactRepository.reveal] after server-side checks.
 */
data class Partner(
    val id: String,
    val name: String,
    val machine: ServiceCategory,
    val subCategories: List<String>,
    val village: String,
    val district: String,
    /** null when the server has no location for the partner; the UI then hides distance. */
    val distanceKm: Int?,
    val maskedPhone: String,
    /** The account's mobile was OTP-confirmed. This says nothing about the machine or documents. */
    val phoneVerifiedAccount: Boolean,
    val indicativeRate: String?,
) {
    fun matches(query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        return name.contains(q, ignoreCase = true) ||
            village.contains(q, ignoreCase = true) ||
            district.contains(q, ignoreCase = true) ||
            machine.label.contains(q, ignoreCase = true) ||
            machine.filterLabel.contains(q, ignoreCase = true) ||
            subCategories.any { it.contains(q, ignoreCase = true) }
    }
}

/** "+91 98XXX XX123" style mask. */
fun maskIndianMobile(digits: String): String =
    if (digits.length == 10) "+91 ${digits.take(2)}XXX XX${digits.takeLast(3)}" else "+91 XXXXX XXXXX"
