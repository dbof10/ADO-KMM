package dev.azure.desktop.domain.pr

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetActivePullRequestsUseCase(
    private val repository: PullRequestRepository,
) {
    suspend operator fun invoke(organization: String, projectName: String?): Result<List<PullRequestSummary>> =
        withContext(Dispatchers.Default) {
            repository.getActivePullRequests(organization.trim(), projectName?.trim()?.takeIf { it.isNotBlank() })
        }
}

