package dev.azure.desktop.data.auth

import com.microsoft.credentialstorage.SecretStore
import com.microsoft.credentialstorage.StorageProvider
import com.microsoft.credentialstorage.model.StoredCredential
import com.microsoft.credentialstorage.model.StoredToken
import com.microsoft.credentialstorage.model.StoredTokenType
import dev.azure.desktop.domain.auth.PatStorage

private const val PatKey = "dev.azure.desktop.ado_pat"
private const val OrgKey = "dev.azure.desktop.ado_org"

/**
 * Stores the PAT in the OS credential store via
 * [com.microsoft.credentialstorage](https://github.com/microsoft/credential-secure-storage-for-java)
 * (macOS Keychain, Windows Credential Manager, Linux libsecret / GNOME Keyring when available).
 * Organization name is stored as a generic credential (username field).
 */
class OsCredentialPatStorage : PatStorage {

    private val tokenStore: SecretStore<StoredToken>? =
        StorageProvider.getTokenStorage(true, StorageProvider.SecureOption.REQUIRED)

    private val credentialStore: SecretStore<StoredCredential>? =
        StorageProvider.getCredentialStorage(true, StorageProvider.SecureOption.REQUIRED)

    override fun savePat(pat: String): Result<Unit> =
        runCatching {
            val s = tokenStore ?: error(NO_STORE_MESSAGE)
            s.delete(PatKey)
            val token = StoredToken(pat.toCharArray(), StoredTokenType.PERSONAL)
            if (!s.add(PatKey, token)) {
                error("Could not save the token to the OS credential store.")
            }
        }

    override fun loadPat(): String? {
        val s = tokenStore ?: return null
        val token = s.get(PatKey) ?: return null
        return try {
            String(token.value)
        } finally {
            token.clear()
        }
    }

    override fun saveOrganization(name: String): Result<Unit> =
        runCatching {
            val trimmed = name.trim()
            val s = credentialStore ?: error(NO_STORE_MESSAGE)
            s.delete(OrgKey)
            if (trimmed.isEmpty()) {
                return@runCatching
            }
            val emptyPassword = charArrayOf()
            val cred = StoredCredential(trimmed, emptyPassword)
            try {
                if (!s.add(OrgKey, cred)) {
                    error("Could not save the organization to the OS credential store.")
                }
            } finally {
                cred.clear()
            }
        }

    override fun loadOrganization(): String? {
        val s = credentialStore ?: return null
        val cred = s.get(OrgKey) ?: return null
        return try {
            cred.username.takeIf { it.isNotBlank() }
        } finally {
            cred.clear()
        }
    }

    override fun clearCredentials() {
        tokenStore?.delete(PatKey)
        credentialStore?.delete(OrgKey)
    }

    override fun clearPatOnly() {
        tokenStore?.delete(PatKey)
    }

    companion object {
        const val NO_STORE_MESSAGE =
            "No secure credential store is available. On Linux, ensure libsecret or GNOME Keyring is installed."
    }
}
