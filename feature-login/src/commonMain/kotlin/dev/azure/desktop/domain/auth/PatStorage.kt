package dev.azure.desktop.domain.auth

/**
 * Persists the Azure DevOps PAT using a platform secure store (OS keychain / credential manager).
 */
interface PatStorage {
    fun savePat(pat: String): Result<Unit>

    fun loadPat(): String?

    fun clearPat()
}
