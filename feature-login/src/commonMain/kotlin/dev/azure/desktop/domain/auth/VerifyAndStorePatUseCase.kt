package dev.azure.desktop.domain.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VerifyAndStorePatUseCase(
    private val verifier: PatVerifier,
    private val storage: PatStorage,
) {
    suspend operator fun invoke(organization: String, pat: String): Result<Unit> {
        val trimmedOrg = organization.trim()
        val trimmed = pat.trim()
        if (trimmedOrg.isEmpty()) {
            return Result.failure(IllegalArgumentException("Enter an Azure DevOps organization."))
        }
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Enter a personal access token."))
        }
        // Default (not IO): Dispatchers.IO is JVM-only; Native/iOS cannot access it.
        return withContext(Dispatchers.Default) {
            verifier.verify(trimmedOrg, trimmed).fold(
                onSuccess = {
                    storage.savePat(trimmed).fold(
                        onSuccess = { storage.saveOrganization(trimmedOrg) },
                        onFailure = { Result.failure(it) },
                    )
                },
                onFailure = { Result.failure(it) },
            )
        }
    }
}
