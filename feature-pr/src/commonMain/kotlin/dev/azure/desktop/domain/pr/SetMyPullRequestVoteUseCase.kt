package dev.azure.desktop.domain.pr

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SetMyPullRequestVoteUseCase(
    private val repository: PullRequestRepository,
) {
    suspend operator fun invoke(
        organization: String,
        projectName: String,
        repositoryId: String,
        pullRequestId: Int,
        vote: Int,
    ): Result<Unit> =
        withContext(Dispatchers.Default) {
            repository.setMyPullRequestVote(
                organization = organization.trim(),
                projectName = projectName,
                repositoryId = repositoryId,
                pullRequestId = pullRequestId,
                vote = vote,
            )
        }
}

