package com.kaamo.app.data

import com.kaamo.app.location.DeviceLocation
import com.kaamo.app.location.distanceKm
import com.kaamo.app.model.ConsentEvent
import com.kaamo.app.model.CustomerRegistration
import com.kaamo.app.model.Partner
import com.kaamo.app.model.PartnerRegistration
import com.kaamo.app.model.SampleCoordinates
import com.kaamo.app.model.SamplePartners
import kotlinx.coroutines.delay

/**
 * Data contracts. Two implementations exist:
 *  - Demo (Fake*): no network, used when SUPABASE_URL / SUPABASE_ANON_KEY are blank.
 *  - Supabase (data/supabase): calls the Postgres functions defined in supabase/schema.sql.
 * The Android client only ever holds the anon key. Never ship the service_role key.
 */
interface AuthRepository {
    suspend fun sendOtp(mobile: String): Boolean
    suspend fun verifyOtp(mobile: String, code: String): Boolean
}

interface PartnerRepository {
    /** [location] is the live GPS fix (null = unknown). It is used for distance only and never stored. */
    suspend fun listPartners(location: DeviceLocation? = null): List<Partner>
}

sealed interface RevealResult {
    data class Success(val phoneNumber: String) : RevealResult
    data object RateLimited : RevealResult
    data object ListingUnavailable : RevealResult
    data object SignInRequired : RevealResult
    data object NetworkError : RevealResult
}

interface ContactRepository {
    suspend fun reveal(partnerId: String): RevealResult
}

/** Result of the server-side check: has the user accepted the current Terms/Privacy version within the last year? */
data class PolicyStatus(
    val currentVersion: String,
    val summaryEn: String,
    val summaryHi: String,
    val acceptedVersion: String?,
    val needsAccept: Boolean,
    val reason: String?,
)

interface PolicyRepository {
    /** Null when the check is unavailable (demo mode or no network); the app then continues without the gate. */
    suspend fun status(): PolicyStatus?
}

interface ConsentRepository {
    suspend fun record(event: ConsentEvent)
}

enum class ReportReason(val code: String, val label: String) {
    Illegal("stolen_or_illegal", "Stolen or illegal vehicle"),
    Fake("fake", "Fake or fraudulent listing"),
    Unsafe("unsafe_use", "Unsafe or prohibited use"),
    WrongInfo("wrong_info", "Wrong information"),
    Other("other", "Something else"),
}

interface AccountRepository {
    suspend fun saveCustomer(registration: CustomerRegistration): Boolean
    suspend fun savePartner(registration: PartnerRegistration): Boolean
    suspend fun setPhoneVisible(visible: Boolean): Boolean
    suspend fun removeListing(): Boolean
    suspend fun report(partnerId: String, reason: ReportReason, details: String?): Boolean
}

/** Everything the UI needs, created once in [Backend.create]. */
class Services(
    val auth: AuthRepository,
    val partners: PartnerRepository,
    val contacts: ContactRepository,
    val consent: ConsentRepository,
    val policy: PolicyRepository,
    val account: AccountRepository,
    val isDemo: Boolean,
)

// ---- Demo implementations (no network) ----

class FakeAuthRepository : AuthRepository {
    override suspend fun sendOtp(mobile: String): Boolean { delay(300); return true }
    override suspend fun verifyOtp(mobile: String, code: String): Boolean { delay(300); return true } // any 6 digits
}

class FakePartnerRepository : PartnerRepository {
    override suspend fun listPartners(location: DeviceLocation?): List<Partner> =
        SamplePartners.map { partner ->
            val coords = SampleCoordinates[partner.id]
            partner.copy(
                distanceKm = if (location != null && coords != null) {
                    distanceKm(location.latitude, location.longitude, coords.first, coords.second)
                } else {
                    null
                },
            )
        }
}

class FakeContactRepository : ContactRepository {
    // Deliberately invalid numbers so a demo call can never ring a real person.
    private val demoNumbers = SamplePartners.associate { it.id to "00000000" + it.id.padStart(2, '0') }
    private val revealTimes = ArrayDeque<Long>()

    override suspend fun reveal(partnerId: String): RevealResult {
        delay(400)
        val now = System.currentTimeMillis()
        while (revealTimes.isNotEmpty() && now - revealTimes.first() > 3_600_000L) revealTimes.removeFirst()
        if (revealTimes.size >= 10) return RevealResult.RateLimited // mirrors the 10 reveals / hour rule
        val number = demoNumbers[partnerId] ?: return RevealResult.ListingUnavailable
        revealTimes.addLast(now)
        return RevealResult.Success(number)
    }
}

class FakePolicyRepository : PolicyRepository {
    override suspend fun status(): PolicyStatus? = null
}

class FakeConsentRepository : ConsentRepository {
    val events = mutableListOf<ConsentEvent>()
    override suspend fun record(event: ConsentEvent) {
        events += event
    }
}

class FakeAccountRepository : AccountRepository {
    override suspend fun saveCustomer(registration: CustomerRegistration) = true
    override suspend fun savePartner(registration: PartnerRegistration) = true
    override suspend fun setPhoneVisible(visible: Boolean) = true
    override suspend fun removeListing() = true
    override suspend fun report(partnerId: String, reason: ReportReason, details: String?) = true
}
