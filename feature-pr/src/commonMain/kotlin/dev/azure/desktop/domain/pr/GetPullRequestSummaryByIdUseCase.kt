package dev.azure.desktop.domain.pr

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetPullRequestSummaryByIdUseCase(
    private val repository: PullRequestRepository,
) {
    suspend operator fun invoke(
        organization: String,
        projectName: String,
        pullRequestId: Int,
    ): Result<PullRequestSummary> =
        withContext(Dispatchers.Default) {
            repository.getPullRequestSummaryById(
                organization = organization.trim(),
                projectName = projectName.trim(),
                pullRequestId = pullRequestId,
            )
        }
}

