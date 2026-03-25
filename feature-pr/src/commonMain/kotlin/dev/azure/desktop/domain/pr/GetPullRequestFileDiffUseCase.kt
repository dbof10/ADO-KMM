package dev.azure.desktop.domain.pr

class GetPullRequestFileDiffUseCase(
    private val repository: PullRequestRepository,
) {
    suspend operator fun invoke(
        organization: String,
        projectName: String,
        repositoryId: String,
        baseCommitId: String,
        targetCommitId: String,
        path: String,
    ): Result<List<PullRequestDiffLine>> =
        runCatching {
            val baseText =
                repository.getFileContentAtCommit(
                    organization = organization,
                    projectName = projectName,
                    repositoryId = repositoryId,
                    path = path,
                    commitId = baseCommitId,
                ).getOrThrow()
            val targetText =
                repository.getFileContentAtCommit(
                    organization = organization,
                    projectName = projectName,
                    repositoryId = repositoryId,
                    path = path,
                    commitId = targetCommitId,
                ).getOrThrow()

            val oldLines = baseText?.split("\n") ?: emptyList()
            val newLines = targetText?.split("\n") ?: emptyList()

            computePullRequestDiffLines(oldLines, newLines)
        }
}
