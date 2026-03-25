package dev.azure.desktop.domain.pr

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecordProjectSelectedUseCase(
    private val repository: ProjectSelectionRepository,
) {
    suspend operator fun invoke(
        organization: String,
        projectName: String,
    ): Result<Unit> =
        withContext(Dispatchers.Default) {
            val org = organization.trim()
            val project = projectName.trim()
            if (org.isBlank() || project.isBlank()) {
                return@withContext Result.success(Unit)
            }
            repository.recordProjectSelected(org, project)
        }
}

