package dev.azure.desktop.domain.pr

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetDefaultProjectNameUseCase(
    private val repository: ProjectSelectionRepository,
) {
    suspend operator fun invoke(
        organization: String,
        availableProjectNames: List<String>,
    ): Result<String?> =
        withContext(Dispatchers.Default) {
            repository.getDefaultProjectName(
                organization = organization.trim(),
                availableProjectNames = availableProjectNames,
            )
        }
}

