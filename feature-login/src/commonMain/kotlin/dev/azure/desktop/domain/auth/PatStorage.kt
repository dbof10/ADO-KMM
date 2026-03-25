package dev.azure.desktop.domain.auth

/**
 * Persists the Azure DevOps PAT using a platform secure store (OS keychain / credential manager).
 */
interface PatStorage {
    fun savePat(pat: String): Result<Unit>

    fun loadPat(): String?

    /** Persists the organization name for API calls and re-login after an expired token. */
    fun saveOrganization(name: String): Result<Unit>

    fun loadOrganization(): String?

    /** Full sign out: removes PAT and organization. */
    fun clearCredentials()

    /** Removes only the PAT (e.g. expired token); keeps organization when possible. */
    fun clearPatOnly()
}
