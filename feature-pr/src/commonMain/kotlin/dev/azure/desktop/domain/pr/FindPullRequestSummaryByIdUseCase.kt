package dev.azure.desktop.domain.pr

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FindPullRequestSummaryByIdUseCase(
    private val listProjectsUseCase: ListProjectsUseCase,
    private val getMyPullRequestsUseCase: GetMyPullRequestsUseCase,
    private val getActivePullRequestsUseCase: GetActivePullRequestsUseCase,
) {
    suspend operator fun invoke(
        organization: String,
        projectName: String?,
        pullRequestId: Int,
    ): Result<PullRequestSummary> =
        withContext(Dispatchers.Default) {
            val org = organization.trim()
            val normalizedProject = projectName?.trim()?.takeIf { it.isNotBlank() }

            fun findIn(items: List<PullRequestSummary>): PullRequestSummary? = items.firstOrNull { it.id == pullRequestId }

            suspend fun findInProject(project: String): Result<PullRequestSummary?> {
                val active = getActivePullRequestsUseCase(org, project).getOrElse { return Result.failure(it) }
                findIn(active)?.let { return Result.success(it) }
                val mine = getMyPullRequestsUseCase(org, project).getOrElse { return Result.failure(it) }
                return Result.success(findIn(mine))
            }

            if (normalizedProject != null) {
                val found = findInProject(normalizedProject).getOrElse { return@withContext Result.failure(it) }
                return@withContext found?.let { Result.success(it) }
                    ?: Result.failure(IllegalArgumentException("Pull request #$pullRequestId not found in project $normalizedProject."))
            }

            val projects = listProjectsUseCase(org).getOrElse { return@withContext Result.failure(it) }
            for (p in projects) {
                val found = findInProject(p.name).getOrElse { return@withContext Result.failure(it) }
                if (found != null) return@withContext Result.success(found)
            }
            return@withContext Result.failure(IllegalArgumentException("Pull request #$pullRequestId not found."))
        }
}
