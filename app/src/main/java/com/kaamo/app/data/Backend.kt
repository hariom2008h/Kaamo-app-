package com.kaamo.app.data

import android.content.Context
import com.kaamo.app.BuildConfig
import com.kaamo.app.data.supabase.SupabaseAccountRepository
import com.kaamo.app.data.supabase.SupabaseApi
import com.kaamo.app.data.supabase.SupabaseAuthRepository
import com.kaamo.app.data.supabase.SupabaseConsentRepository
import com.kaamo.app.data.supabase.SupabaseContactRepository
import com.kaamo.app.data.supabase.SupabasePartnerRepository
import com.kaamo.app.data.supabase.SupabasePolicyRepository

object Backend {
    /** Demo mode (sample data, any 6-digit OTP works) unless both Supabase values are set in local.properties. */
    fun create(context: Context): Services {
        val url = BuildConfig.SUPABASE_URL
        val key = BuildConfig.SUPABASE_ANON_KEY
        if (url.isBlank() || key.isBlank()) {
            return Services(
                auth = FakeAuthRepository(),
                partners = FakePartnerRepository(),
                contacts = FakeContactRepository(),
                consent = FakeConsentRepository(),
                policy = FakePolicyRepository(),
                account = FakeAccountRepository(),
                isDemo = true,
            )
        }
        val api = SupabaseApi(context.applicationContext, url, key)
        return Services(
            auth = SupabaseAuthRepository(api),
            partners = SupabasePartnerRepository(api),
            contacts = SupabaseContactRepository(api),
            consent = SupabaseConsentRepository(api),
            policy = SupabasePolicyRepository(api),
            account = SupabaseAccountRepository(api),
            isDemo = false,
        )
    }
}
