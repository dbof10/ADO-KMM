package dev.azure.desktop.data.pr

import dev.azure.desktop.data.auth.JvmAuthServices
import dev.azure.desktop.domain.pr.GetActivePullRequestsUseCase
import dev.azure.desktop.domain.pr.GetMyPullRequestsUseCase
import dev.azure.desktop.domain.pr.ListProjectsUseCase
import dev.azure.desktop.domain.pr.GetPullRequestDetailUseCase
import dev.azure.desktop.domain.pr.GetPullRequestFileDiffUseCase
import dev.azure.desktop.domain.pr.GetPullRequestLineStatsUseCase

object JvmPullRequestServices {
    private val repository by lazy {
        AdoPullRequestRepository(
            httpClient = JvmAuthServices.sessionHttpClient,
            patProvider = { JvmAuthServices.patStorage.loadPat() },
        )
    }

    val listProjectsUseCase by lazy { ListProjectsUseCase(repository) }

    val getMyPullRequestsUseCase by lazy { GetMyPullRequestsUseCase(repository) }

    val getActivePullRequestsUseCase by lazy { GetActivePullRequestsUseCase(repository) }

    private val getPullRequestLineStatsUseCase by lazy { GetPullRequestLineStatsUseCase(repository) }

    val getPullRequestDetailUseCase by lazy {
        GetPullRequestDetailUseCase(repository, getPullRequestLineStatsUseCase)
    }

    val getPullRequestFileDiffUseCase by lazy { GetPullRequestFileDiffUseCase(repository) }

    suspend fun getPullRequestChanges(
        organization: String,
        projectName: String,
        repositoryId: String,
        pullRequestId: Int,
        baseCommitId: String,
        targetCommitId: String,
    ) = repository.getPullRequestChanges(organization, projectName, repositoryId, pullRequestId, baseCommitId, targetCommitId)
}
