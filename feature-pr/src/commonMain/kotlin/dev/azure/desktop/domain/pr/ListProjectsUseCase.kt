package dev.azure.desktop.domain.pr

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ListProjectsUseCase(
    private val repository: PullRequestRepository,
) {
    suspend operator fun invoke(organization: String): Result<List<DevOpsProject>> =
        withContext(Dispatchers.Default) {
            repository.listProjects(organization.trim())
        }
}
