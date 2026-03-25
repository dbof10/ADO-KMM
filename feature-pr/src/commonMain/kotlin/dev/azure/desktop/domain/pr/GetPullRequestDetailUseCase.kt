package dev.azure.desktop.domain.pr

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetPullRequestDetailUseCase(
    private val repository: PullRequestRepository,
    private val getPullRequestLineStats: GetPullRequestLineStatsUseCase,
) {
    suspend operator fun invoke(
        organization: String,
        projectName: String,
        repositoryId: String,
        pullRequestId: Int,
    ): Result<PullRequestDetail> =
        withContext(Dispatchers.Default) {
            val org = organization.trim()
            val detail =
                repository
                    .getPullRequestDetail(
                        organization = org,
                        projectName = projectName,
                        repositoryId = repositoryId,
                        pullRequestId = pullRequestId,
                    )
                    .getOrElse { return@withContext Result.failure(it) }

            val base = detail.lastMergeTargetCommitId
            val target = detail.lastMergeSourceCommitId
            if (base.isNullOrBlank() || target.isNullOrBlank()) {
                return@withContext Result.success(detail)
            }

            val stats =
                getPullRequestLineStats(
                    organization = org,
                    projectName = projectName,
                    repositoryId = repositoryId,
                    pullRequestId = pullRequestId,
                    baseCommitId = base,
                    targetCommitId = target,
                ).getOrNull()

            Result.success(
                when {
                    stats == null || stats.truncated ->
                        detail.copy(linesAdded = null, linesRemoved = null)
                    else ->
                        detail.copy(linesAdded = stats.additions, linesRemoved = stats.removals)
                },
            )
        }
}
