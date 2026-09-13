package ca.gapwise.android.data.account

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class AccountMaintenance(
    private val accounts: GapwiseAccountManager,
) {
    suspend fun deleteAccount(): Unit = withContext(Dispatchers.IO) {
        val session = accounts.requireSession()
        val connection = (
            URL("${GapwiseCloudConfig.SUPABASE_URL}/functions/v1/delete-account")
                .openConnection() as HttpURLConnection
            ).apply {
            requestMethod = "POST"
            connectTimeout = 12_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("apikey", GapwiseCloudConfig.SUPABASE_PUBLISHABLE_KEY)
            setRequestProperty("Authorization", "Bearer ${session.accessToken}")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }
        connection.outputStream.use { it.write("{}".toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        if (code in 200..299) connection.inputStream?.close() else connection.errorStream?.close()
        connection.disconnect()
        require(code in 200..299) { "We couldn't delete your account. Please try again." }
        accounts.signOut()
    }
}
