package ca.gapwise.android.data.account

import android.net.Uri
import android.util.Base64
import ca.gapwise.android.core.persistence.SecureLocalStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom

internal object GapwiseCloudConfig {
    const val SUPABASE_URL = "https://olrtvbblxbgcxbhvujaw.supabase.co"
    const val SUPABASE_PUBLISHABLE_KEY = "sb_publishable_yjwr-9Rv7TZTB6iCyCuNoQ_yB38Kvab"
    const val KEY_BROKER_URL = "https://gapwise.ca/api/key-broker"
    const val AUTH_REDIRECT = "gapwise://auth-callback"
}

enum class AuthProvider(val id: String, val label: String) {
    GOOGLE("google", "Google"),
    MICROSOFT("azure", "Microsoft"),
    GITHUB("github", "GitHub"),
}

data class AccountIdentity(
    val userId: String,
    val email: String?,
)

internal data class AccountSession(
    val userId: String,
    val email: String?,
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochSeconds: Long,
)

/** Supabase Auth PKCE client with tokens encrypted in Android Keystore-backed local storage. */
class GapwiseAccountManager(
    private val secureStore: SecureLocalStore,
) {
    fun storedIdentity(): AccountIdentity? = readSession()?.let {
        AccountIdentity(userId = it.userId, email = it.email)
    }

    fun startOAuth(provider: AuthProvider): Uri {
        val verifierBytes = ByteArray(48).also(SecureRandom()::nextBytes)
        val verifier = base64Url(verifierBytes)
        val challenge = base64Url(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()))
        secureStore.put(
            PKCE_ENTRY,
            JSONObject()
                .put("provider", provider.id)
                .put("verifier", verifier)
                .toString(),
        )
        return Uri.parse("${GapwiseCloudConfig.SUPABASE_URL}/auth/v1/authorize")
            .buildUpon()
            .appendQueryParameter("provider", provider.id)
            .appendQueryParameter("redirect_to", GapwiseCloudConfig.AUTH_REDIRECT)
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "s256")
            .build()
    }

    suspend fun completeOAuth(callback: Uri): AccountIdentity = withContext(Dispatchers.IO) {
        require(callback.scheme == "gapwise" && callback.host == "auth-callback") {
            "Unexpected sign-in callback."
        }
        callback.getQueryParameter("error_description")?.let { error(it) }
        callback.getQueryParameter("error")?.let { error("Sign in failed: $it") }
        val code = callback.getQueryParameter("code") ?: error("Sign in callback is missing its authorization code.")
        val pkce = secureStore.get(PKCE_ENTRY)?.let(::JSONObject)
            ?: error("This sign-in attempt has expired. Start again.")
        val verifier = pkce.getString("verifier")

        val response = request(
            url = "${GapwiseCloudConfig.SUPABASE_URL}/auth/v1/token?grant_type=pkce",
            method = "POST",
            body = JSONObject()
                .put("auth_code", code)
                .put("code_verifier", verifier)
                .toString(),
        )
        if (response.code !in 200..299) error(authError(response.body))
        val session = parseSession(JSONObject(response.body), previous = null)
        writeSession(session)
        secureStore.remove(PKCE_ENTRY)
        AccountIdentity(session.userId, session.email)
    }

    internal suspend fun requireSession(): AccountSession = withContext(Dispatchers.IO) {
        val current = readSession() ?: error("Sign in before using account sync.")
        val now = System.currentTimeMillis() / 1000L
        if (current.expiresAtEpochSeconds > now + 90L) return@withContext current

        val response = request(
            url = "${GapwiseCloudConfig.SUPABASE_URL}/auth/v1/token?grant_type=refresh_token",
            method = "POST",
            body = JSONObject().put("refresh_token", current.refreshToken).toString(),
        )
        if (response.code !in 200..299) {
            secureStore.remove(SESSION_ENTRY)
            error("Your Gapwise session expired. Sign in again.")
        }
        parseSession(JSONObject(response.body), previous = current).also(::writeSession)
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        val current = readSession()
        if (current != null) {
            runCatching {
                request(
                    url = "${GapwiseCloudConfig.SUPABASE_URL}/auth/v1/logout",
                    method = "POST",
                    body = "{}",
                    accessToken = current.accessToken,
                )
            }
        }
        secureStore.remove(SESSION_ENTRY)
        secureStore.remove(PKCE_ENTRY)
    }

    private fun parseSession(json: JSONObject, previous: AccountSession?): AccountSession {
        val user = json.optJSONObject("user")
        val userId = user?.optString("id")?.takeIf(String::isNotBlank) ?: previous?.userId
            ?: error("Supabase did not return an account identity.")
        val email = user?.optString("email")?.takeIf(String::isNotBlank) ?: previous?.email
        val accessToken = json.optString("access_token").takeIf(String::isNotBlank)
            ?: error("Supabase did not return an access token.")
        val refreshToken = json.optString("refresh_token").takeIf(String::isNotBlank)
            ?: previous?.refreshToken
            ?: error("Supabase did not return a refresh token.")
        val expiresIn = json.optLong("expires_in", 3600L).coerceAtLeast(60L)
        val expiresAt = json.optLong("expires_at", 0L).takeIf { it > 0L }
            ?: (System.currentTimeMillis() / 1000L + expiresIn)
        return AccountSession(userId, email, accessToken, refreshToken, expiresAt)
    }

    private fun writeSession(session: AccountSession) {
        secureStore.put(
            SESSION_ENTRY,
            JSONObject()
                .put("userId", session.userId)
                .put("email", session.email ?: JSONObject.NULL)
                .put("accessToken", session.accessToken)
                .put("refreshToken", session.refreshToken)
                .put("expiresAt", session.expiresAtEpochSeconds)
                .toString(),
        )
    }

    private fun readSession(): AccountSession? = secureStore.get(SESSION_ENTRY)?.let { encoded ->
        runCatching {
            val json = JSONObject(encoded)
            AccountSession(
                userId = json.getString("userId"),
                email = if (json.isNull("email")) null else json.getString("email"),
                accessToken = json.getString("accessToken"),
                refreshToken = json.getString("refreshToken"),
                expiresAtEpochSeconds = json.getLong("expiresAt"),
            )
        }.getOrNull()
    }

    private data class HttpResult(val code: Int, val body: String)

    private fun request(
        url: String,
        method: String,
        body: String? = null,
        accessToken: String? = null,
    ): HttpResult {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 12_000
            readTimeout = 15_000
            setRequestProperty("apikey", GapwiseCloudConfig.SUPABASE_PUBLISHABLE_KEY)
            setRequestProperty("Accept", "application/json")
            if (accessToken != null) setRequestProperty("Authorization", "Bearer $accessToken")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }
        if (body != null) connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader()?.use { it.readText().take(64 * 1024) }.orEmpty()
        connection.disconnect()
        return HttpResult(code, text)
    }

    private fun authError(body: String): String = runCatching {
        val json = JSONObject(body)
        json.optString("msg").ifBlank { json.optString("error_description") }
            .ifBlank { json.optString("message") }
            .ifBlank { "Sign in failed." }
    }.getOrDefault("Sign in failed.")

    private fun base64Url(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

    private companion object {
        const val SESSION_ENTRY = "account.session"
        const val PKCE_ENTRY = "account.pkce"
    }
}
