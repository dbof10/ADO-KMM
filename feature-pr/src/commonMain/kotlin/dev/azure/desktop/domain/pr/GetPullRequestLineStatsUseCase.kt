package dev.azure.desktop.domain.pr

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

private const val FileFetchConcurrency = 4

data class PullRequestLineStats(
    val additions: Int,
    val removals: Int,
    val truncated: Boolean,
)

/**
 * Aggregates added/removed line counts across changed files (REST: change list + items API per path),
 * using the same line diff as [GetPullRequestFileDiffUseCase].
 */
class GetPullRequestLineStatsUseCase(
    private val repository: PullRequestRepository,
) {
    suspend operator fun invoke(
        organization: String,
        projectName: String,
        repositoryId: String,
        pullRequestId: Int,
        baseCommitId: String,
        targetCommitId: String,
    ): Result<PullRequestLineStats> =
        runCatching {
            val org = organization.trim()
            val changes =
                repository.getPullRequestChanges(
                    organization = org,
                    projectName = projectName,
                    repositoryId = repositoryId,
                    pullRequestId = pullRequestId,
                    baseCommitId = baseCommitId,
                    targetCommitId = targetCommitId,
                ).getOrThrow()

            val paths =
                changes
                    .asSequence()
                    .filter { !it.isFolder }
                    .map { it.path }
                    .distinct()
                    .toList()

            if (paths.isEmpty()) {
                return@runCatching PullRequestLineStats(0, 0, truncated = false)
            }

            val perFile =
                coroutineScope {
                    paths
                        .chunked(FileFetchConcurrency)
                        .flatMap { chunk ->
                            chunk
                                .map { path ->
                                    async {
                                        val baseText =
                                            repository
                                                .getFileContentAtCommit(
                                                    organization = org,
                                                    projectName = projectName,
                                                    repositoryId = repositoryId,
                                                    path = path,
                                                    commitId = baseCommitId,
                                                ).getOrThrow()
                                        val targetText =
                                            repository
                                                .getFileContentAtCommit(
                                                    organization = org,
                                                    projectName = projectName,
                                                    repositoryId = repositoryId,
                                                    path = path,
                                                    commitId = targetCommitId,
                                                ).getOrThrow()
                                        val oldLines = baseText?.split("\n") ?: emptyList()
                                        val newLines = targetText?.split("\n") ?: emptyList()
                                        computePullRequestFileLineDiffStats(oldLines, newLines)
                                    }
                                }.awaitAll()
                        }
                }

            if (perFile.any { it.truncated }) {
                return@runCatching PullRequestLineStats(0, 0, truncated = true)
            }

            PullRequestLineStats(
                additions = perFile.sumOf { it.additions },
                removals = perFile.sumOf { it.removals },
                truncated = false,
            )
        }
}
