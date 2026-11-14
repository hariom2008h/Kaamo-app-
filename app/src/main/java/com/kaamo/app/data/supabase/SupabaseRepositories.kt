package com.kaamo.app.data.supabase

import com.kaamo.app.data.AccountRepository
import com.kaamo.app.data.AuthRepository
import com.kaamo.app.data.ConsentRepository
import com.kaamo.app.data.ContactRepository
import com.kaamo.app.data.PartnerRepository
import com.kaamo.app.data.PolicyRepository
import com.kaamo.app.data.PolicyStatus
import com.kaamo.app.data.ReportReason
import com.kaamo.app.data.RevealResult
import com.kaamo.app.location.DeviceLocation
import com.kaamo.app.model.CevUse
import com.kaamo.app.model.ConsentEvent
import com.kaamo.app.model.CustomerRegistration
import com.kaamo.app.model.Partner
import com.kaamo.app.model.PartnerRegistration
import com.kaamo.app.model.ServiceCategory
import com.kaamo.app.model.maskIndianMobile
import org.json.JSONArray
import org.json.JSONObject

private fun List<String>.toJson(): JSONArray = JSONArray().also { array -> forEach { array.put(it) } }

class SupabaseAuthRepository(private val api: SupabaseApi) : AuthRepository {
    override suspend fun sendOtp(mobile: String): Boolean =
        api.auth("/otp", JSONObject().put("phone", "+91$mobile").put("create_user", true)).ok

    override suspend fun verifyOtp(mobile: String, code: String): Boolean {
        val response = api.auth(
            "/verify",
            JSONObject().put("type", "sms").put("phone", "+91$mobile").put("token", code),
        )
        if (!response.ok) return false
        api.saveSession(response.body)
        return api.signedIn
    }
}

class SupabasePartnerRepository(private val api: SupabaseApi) : PartnerRepository {
    override suspend fun listPartners(location: DeviceLocation?): List<Partner> {
        val args = JSONObject()
        if (location != null) args.put("p_lat", location.latitude).put("p_lng", location.longitude)
        val response = api.rpc("list_partners", args)
        if (!response.ok) return emptyList()
        return try {
            val rows = JSONArray(response.body)
            buildList {
                for (i in 0 until rows.length()) {
                    val row = rows.getJSONObject(i)
                    val category = ServiceCategory.fromCode(row.optString("service_category")) ?: continue
                    val subs = row.optJSONArray("sub_categories")
                    add(
                        Partner(
                            id = row.getString("id"),
                            name = row.getString("full_name"),
                            machine = category,
                            subCategories = buildList { if (subs != null) for (j in 0 until subs.length()) add(subs.getString(j)) },
                            village = if (row.isNull("village")) "" else row.optString("village"),
                            district = if (row.isNull("district")) "" else row.optString("district"),
                            distanceKm = if (row.isNull("distance_km")) null else row.getInt("distance_km"),
                            maskedPhone = row.optString("masked_phone").ifEmpty { maskIndianMobile("") },
                            phoneVerifiedAccount = row.optBoolean("phone_verified", false),
                            indicativeRate = row.optString("indicative_rate").ifEmpty { null },
                        ),
                    )
                }
            }
        } catch (e: org.json.JSONException) {
            emptyList()
        }
    }
}

class SupabaseContactRepository(private val api: SupabaseApi) : ContactRepository {
    override suspend fun reveal(partnerId: String): RevealResult {
        val response = api.rpc("reveal_contact", JSONObject().put("p_partner_id", partnerId))
        return when {
            response.code == -1 -> RevealResult.NetworkError
            response.code == 401 -> RevealResult.SignInRequired
            !response.ok -> RevealResult.NetworkError
            else -> try {
                val json = JSONObject(response.body)
                when (json.optString("status")) {
                    "ok" -> RevealResult.Success(json.getString("phone"))
                    "rate_limited" -> RevealResult.RateLimited
                    "not_signed_in" -> RevealResult.SignInRequired
                    else -> RevealResult.ListingUnavailable
                }
            } catch (e: org.json.JSONException) {
                RevealResult.NetworkError
            }
        }
    }
}

class SupabasePolicyRepository(private val api: SupabaseApi) : PolicyRepository {
    override suspend fun status(): PolicyStatus? {
        val response = api.rpc("get_policy_status")
        if (!response.ok) return null
        return try {
            val json = JSONObject(response.body)
            PolicyStatus(
                currentVersion = json.getString("current_version"),
                summaryEn = json.optString("summary_en"),
                summaryHi = json.optString("summary_hi"),
                acceptedVersion = if (json.isNull("accepted_version")) null else json.getString("accepted_version"),
                needsAccept = json.optBoolean("needs_accept", false),
                reason = if (json.isNull("reason")) null else json.optString("reason"),
            )
        } catch (e: org.json.JSONException) {
            null
        }
    }
}

class SupabaseConsentRepository(private val api: SupabaseApi) : ConsentRepository {
    override suspend fun record(event: ConsentEvent) {
        api.rpc(
            "record_consent",
            JSONObject()
                .put("p_type", event.type.name)
                .put("p_action", event.action.name)
                .put("p_version", event.version)
                .put("p_language", event.languageCode),
        )
    }
}

class SupabaseAccountRepository(private val api: SupabaseApi) : AccountRepository {
    override suspend fun saveCustomer(registration: CustomerRegistration): Boolean =
        api.rpc(
            "register_customer",
            JSONObject().put("p_full_name", registration.fullName).put("p_location", registration.location),
        ).ok

    override suspend fun savePartner(registration: PartnerRegistration): Boolean {
        val args = JSONObject()
            .put("p_full_name", registration.fullName)
            .put("p_village", registration.homeVillage)
            .put("p_district", registration.district)
            .put("p_category", registration.machine.code)
            .put("p_sub_categories", registration.subCategories.toJson())
            .put("p_service_areas", registration.serviceAreas.toJson())
            .put("p_phone_public", registration.phonePublic)
            .put("p_notice_language", registration.noticeLanguage)
            .put("p_indicative_rate", registration.indicativeRate ?: JSONObject.NULL)
            .put("p_rc_number", registration.rcNumber ?: JSONObject.NULL)
            .put("p_insurance_until", registration.insuranceValidUntil?.toString() ?: JSONObject.NULL)
            .put("p_fitness_until", registration.fitnessValidUntil?.toString() ?: JSONObject.NULL)
            .put(
                "p_cev_use",
                when (registration.cevUse) {
                    CevUse.OnSite -> "on_site"
                    CevUse.LowBedTransport -> "low_bed_transport"
                    null -> JSONObject.NULL
                },
            )
            .put("p_latitude", registration.latitude ?: JSONObject.NULL)
            .put("p_longitude", registration.longitude ?: JSONObject.NULL)
        return api.rpc("register_partner", args).ok
    }

    override suspend fun setPhoneVisible(visible: Boolean): Boolean =
        api.rpc("set_phone_visibility", JSONObject().put("p_visible", visible)).ok

    override suspend fun removeListing(): Boolean = api.rpc("remove_listing").ok

    override suspend fun report(partnerId: String, reason: ReportReason, details: String?): Boolean =
        api.rpc(
            "report_listing",
            JSONObject()
                .put("p_partner_id", partnerId)
                .put("p_reason", reason.code)
                .put("p_details", details ?: JSONObject.NULL),
        ).ok
}
