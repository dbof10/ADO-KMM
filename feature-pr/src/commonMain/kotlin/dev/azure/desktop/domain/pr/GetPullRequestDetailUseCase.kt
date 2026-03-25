package dev.azure.desktop.domain.pr

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetPullRequestDetailUseCase(
    private val repository: PullRequestRepository,
) {
    suspend operator fun invoke(
        organization: String,
        projectName: String,
        repositoryId: String,
        pullRequestId: Int,
    ): Result<PullRequestDetail> =
        withContext(Dispatchers.Default) {
            repository.getPullRequestDetail(
                organization = organization.trim(),
                projectName = projectName,
                repositoryId = repositoryId,
                pullRequestId = pullRequestId,
            )
        }
}
