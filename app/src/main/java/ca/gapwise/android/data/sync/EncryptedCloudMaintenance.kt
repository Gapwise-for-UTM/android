package ca.gapwise.android.data.sync

import ca.gapwise.android.data.account.GapwiseAccountManager
import ca.gapwise.android.data.account.GapwiseCloudConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class EncryptedCloudMaintenance(
    private val accounts: GapwiseAccountManager,
) {
    suspend fun deletePrivateCloud(): Unit = withContext(Dispatchers.IO) {
        val session = accounts.requireSession()
        listOf("encrypted_friend_availability", "encrypted_private_data").forEach { table ->
            val connection = (
                URL("${GapwiseCloudConfig.SUPABASE_URL}/rest/v1/$table?user_id=eq.${session.userId}")
                    .openConnection() as HttpURLConnection
                ).apply {
                requestMethod = "DELETE"
                connectTimeout = 12_000
                readTimeout = 15_000
                setRequestProperty("apikey", GapwiseCloudConfig.SUPABASE_PUBLISHABLE_KEY)
                setRequestProperty("Authorization", "Bearer ${session.accessToken}")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Prefer", "return=minimal")
            }
            val code = connection.responseCode
            connection.errorStream?.close()
            if (code in 200..299) connection.inputStream?.close()
            connection.disconnect()
            require(code in 200..299) { "Encrypted cloud deletion failed." }
        }
    }
}
