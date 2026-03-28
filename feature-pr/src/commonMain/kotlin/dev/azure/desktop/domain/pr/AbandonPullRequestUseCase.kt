package dev.azure.desktop.domain.pr

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AbandonPullRequestUseCase(
    private val repository: PullRequestRepository,
) {
    suspend operator fun invoke(
        organization: String,
        projectName: String,
        repositoryId: String,
        pullRequestId: Int,
    ): Result<Unit> =
        withContext(Dispatchers.Default) {
            repository.abandonPullRequest(
                organization = organization.trim(),
                projectName = projectName,
                repositoryId = repositoryId,
                pullRequestId = pullRequestId,
            )
        }
}
