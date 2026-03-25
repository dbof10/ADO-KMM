package dev.azure.desktop.data.auth

import com.microsoft.credentialstorage.SecretStore
import com.microsoft.credentialstorage.StorageProvider
import com.microsoft.credentialstorage.model.StoredToken
import com.microsoft.credentialstorage.model.StoredTokenType
import dev.azure.desktop.domain.auth.PatStorage

private const val PatKey = "dev.azure.desktop.ado_pat"

/**
 * Stores the PAT in the OS credential store via
 * [com.microsoft.credentialstorage](https://github.com/microsoft/credential-secure-storage-for-java)
 * (macOS Keychain, Windows Credential Manager, Linux libsecret / GNOME Keyring when available).
 */
class OsCredentialPatStorage : PatStorage {

    private val store: SecretStore<StoredToken>? =
        StorageProvider.getTokenStorage(true, StorageProvider.SecureOption.REQUIRED)

    override fun savePat(pat: String): Result<Unit> =
        runCatching {
            val s = store ?: error(NO_STORE_MESSAGE)
            s.delete(PatKey)
            val token = StoredToken(pat.toCharArray(), StoredTokenType.PERSONAL)
            if (!s.add(PatKey, token)) {
                error("Could not save the token to the OS credential store.")
            }
        }

    override fun loadPat(): String? {
        val s = store ?: return null
        val token = s.get(PatKey) ?: return null
        return try {
            String(token.value)
        } finally {
            token.clear()
        }
    }

    override fun clearPat() {
        store?.delete(PatKey)
    }

    companion object {
        const val NO_STORE_MESSAGE =
            "No secure credential store is available. On Linux, ensure libsecret or GNOME Keyring is installed."
    }
}
