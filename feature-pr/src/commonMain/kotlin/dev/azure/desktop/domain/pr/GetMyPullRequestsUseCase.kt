package dev.azure.desktop.domain.pr

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetMyPullRequestsUseCase(
    private val repository: PullRequestRepository,
) {
    suspend operator fun invoke(organization: String): Result<List<PullRequestSummary>> =
        withContext(Dispatchers.Default) {
            repository.getMyPullRequests(organization.trim())
        }
}
