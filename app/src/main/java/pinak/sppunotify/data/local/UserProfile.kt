package pinak.sppunotify.data.local

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class UserProfile(
    val id: String,
    val name: String,
    val seatNo: String,
    val motherName: String
)

object ProfileSerializer {
    fun serializeEncryptedList(profiles: List<UserProfile>): Set<String> {
        return profiles.map { CryptoManager.encrypt(Json.encodeToString(it)) }.toSet()
    }

    fun deserializeEncryptedList(encryptedStrings: Set<String>): List<UserProfile> {
        return encryptedStrings.mapNotNull {
            try {
                Json.decodeFromString<UserProfile>(CryptoManager.decrypt(it))
            } catch (_: Exception) {
                null
            }
        }
    }
}
