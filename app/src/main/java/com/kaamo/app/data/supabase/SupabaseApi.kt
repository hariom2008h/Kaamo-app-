package com.kaamo.app.data.supabase

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Result of one HTTP call. [code] is -1 when the network failed. */
class ApiResponse(val code: Int, val body: String) {
    val ok: Boolean get() = code in 200..299
}

/**
 * Minimal Supabase client (Auth + PostgREST RPC) over OkHttp, so the project needs no extra SDK.
 * The session lives in private SharedPreferences. For production, store the refresh token in
 * EncryptedSharedPreferences or the Android Keystore.
 */
class SupabaseApi(context: Context, baseUrl: String, private val anonKey: String) {
    private val baseUrl = baseUrl.trimEnd('/')
    private val prefs = context.getSharedPreferences("kaamo_session", Context.MODE_PRIVATE)
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    private var accessToken: String?
        get() = prefs.getString("access_token", null)
        set(value) { prefs.edit().putString("access_token", value).apply() }
    private var refreshToken: String?
        get() = prefs.getString("refresh_token", null)
        set(value) { prefs.edit().putString("refresh_token", value).apply() }

    val signedIn: Boolean get() = accessToken != null

    fun saveSession(body: String) {
        val json = JSONObject(body)
        accessToken = json.optString("access_token").ifEmpty { null }
        refreshToken = json.optString("refresh_token").ifEmpty { null }
    }

    /** The current session as JSON for the embedded web form, or null when signed out. */
    fun sessionJson(): String? {
        val access = accessToken ?: return null
        return JSONObject().put("access_token", access).put("refresh_token", refreshToken.orEmpty()).toString()
    }

    fun clearSession() {
        accessToken = null
        refreshToken = null
    }

    /** Auth endpoints (/auth/v1/...) never send the user token. */
    suspend fun auth(path: String, body: JSONObject): ApiResponse =
        withContext(Dispatchers.IO) { execute("/auth/v1$path", body, useUserToken = false) }

    /** Calls a Postgres function. Retries once after refreshing an expired session. */
    suspend fun rpc(function: String, args: JSONObject = JSONObject()): ApiResponse =
        withContext(Dispatchers.IO) {
            val path = "/rest/v1/rpc/$function"
            var response = execute(path, args, useUserToken = true)
            if (response.code == 401 && refresh()) response = execute(path, args, useUserToken = true)
            response
        }

    private fun refresh(): Boolean {
        val token = refreshToken ?: return false
        val response = execute(
            "/auth/v1/token?grant_type=refresh_token",
            JSONObject().put("refresh_token", token),
            useUserToken = false,
        )
        if (!response.ok) {
            clearSession()
            return false
        }
        saveSession(response.body)
        return true
    }

    private fun execute(path: String, body: JSONObject, useUserToken: Boolean): ApiResponse {
        val bearer = if (useUserToken) accessToken ?: anonKey else anonKey
        val request = Request.Builder()
            .url(baseUrl + path)
            .header("apikey", anonKey)
            .header("Authorization", "Bearer $bearer")
            .post(body.toString().toRequestBody(jsonType))
            .build()
        return try {
            http.newCall(request).execute().use { ApiResponse(it.code, it.body?.string().orEmpty()) }
        } catch (e: IOException) {
            ApiResponse(-1, "")
        }
    }
}
