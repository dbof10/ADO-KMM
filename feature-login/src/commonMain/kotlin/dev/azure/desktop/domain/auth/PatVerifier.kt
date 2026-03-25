package dev.azure.desktop.domain.auth

/**
 * Confirms that a PAT is accepted by Azure DevOps before we persist it.
 */
fun interface PatVerifier {
    suspend fun verify(organization: String, pat: String): Result<Unit>
}
