package dev.azure.desktop.data.pr

import dev.azure.desktop.data.auth.IosAuthServices
import dev.azure.desktop.domain.pr.FindPullRequestSummaryByIdUseCase
import dev.azure.desktop.domain.pr.GetActivePullRequestsUseCase
import dev.azure.desktop.domain.pr.GetMyPullRequestsUseCase
import dev.azure.desktop.domain.pr.GetPullRequestDetailUseCase
import dev.azure.desktop.domain.pr.GetPullRequestFileDiffUseCase
import dev.azure.desktop.domain.pr.GetPullRequestLineStatsUseCase
import dev.azure.desktop.domain.pr.GetPullRequestSummaryByIdUseCase
import dev.azure.desktop.domain.pr.ListProjectsUseCase
import dev.azure.desktop.domain.pr.SetMyPullRequestVoteUseCase

object IosPullRequestServices {
    private val repository by lazy {
        AdoPullRequestRepository(
            httpClient = IosAuthServices.sessionHttpClient,
            patProvider = { IosAuthServices.patStorage.loadPat() },
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

    val findPullRequestSummaryByIdUseCase by lazy {
        FindPullRequestSummaryByIdUseCase(
            listProjectsUseCase = listProjectsUseCase,
            getMyPullRequestsUseCase = getMyPullRequestsUseCase,
            getActivePullRequestsUseCase = getActivePullRequestsUseCase,
        )
    }

    val getPullRequestSummaryByIdUseCase by lazy { GetPullRequestSummaryByIdUseCase(repository) }

    val setMyPullRequestVoteUseCase by lazy { SetMyPullRequestVoteUseCase(repository) }

    suspend fun getPullRequestChanges(
        organization: String,
        projectName: String,
        repositoryId: String,
        pullRequestId: Int,
        baseCommitId: String,
        targetCommitId: String,
    ) = repository.getPullRequestChanges(organization, projectName, repositoryId, pullRequestId, baseCommitId, targetCommitId)
}
