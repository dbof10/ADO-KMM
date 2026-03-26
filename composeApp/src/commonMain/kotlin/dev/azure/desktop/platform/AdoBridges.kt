package dev.azure.desktop.platform

import dev.azure.desktop.domain.auth.PatStorage
import dev.azure.desktop.domain.auth.VerifyAndStorePatUseCase
import dev.azure.desktop.domain.pr.FindPullRequestSummaryByIdUseCase
import dev.azure.desktop.domain.pr.GetActivePullRequestsUseCase
import dev.azure.desktop.domain.pr.GetMyPullRequestsUseCase
import dev.azure.desktop.domain.pr.GetPullRequestDetailUseCase
import dev.azure.desktop.domain.pr.GetPullRequestFileDiffUseCase
import dev.azure.desktop.domain.pr.GetPullRequestSummaryByIdUseCase
import dev.azure.desktop.domain.pr.ListProjectsUseCase
import dev.azure.desktop.domain.pr.PullRequestChange
import dev.azure.desktop.domain.pr.SetMyPullRequestVoteUseCase
import dev.azure.desktop.domain.release.CreateReleaseUseCase
import dev.azure.desktop.domain.release.DeployReleaseEnvironmentUseCase
import dev.azure.desktop.domain.release.GetReleaseDefinitionUseCase
import dev.azure.desktop.domain.release.GetReleaseDetailUseCase
import dev.azure.desktop.domain.release.ListReleaseDefinitionsUseCase
import dev.azure.desktop.domain.release.ListReleasesForDefinitionUseCase

interface AuthBridge {
    val patStorage: PatStorage
    val verifyAndStorePat: VerifyAndStorePatUseCase
    fun setOnSessionUnauthorized(handler: () -> Unit)
    fun runOnMainThread(block: () -> Unit)
}

interface PullRequestBridge {
    val listProjectsUseCase: ListProjectsUseCase
    val getMyPullRequestsUseCase: GetMyPullRequestsUseCase
    val getActivePullRequestsUseCase: GetActivePullRequestsUseCase
    val findPullRequestSummaryByIdUseCase: FindPullRequestSummaryByIdUseCase
    val getPullRequestSummaryByIdUseCase: GetPullRequestSummaryByIdUseCase
    val getPullRequestDetailUseCase: GetPullRequestDetailUseCase
    val setMyPullRequestVoteUseCase: SetMyPullRequestVoteUseCase
    val getPullRequestFileDiffUseCase: GetPullRequestFileDiffUseCase

    suspend fun getPullRequestChanges(
        organization: String,
        projectName: String,
        repositoryId: String,
        pullRequestId: Int,
        baseCommitId: String,
        targetCommitId: String,
    ): Result<List<PullRequestChange>>
}

interface ReleaseBridge {
    val listReleaseDefinitionsUseCase: ListReleaseDefinitionsUseCase
    val listReleasesForDefinitionUseCase: ListReleasesForDefinitionUseCase
    val getReleaseDetailUseCase: GetReleaseDetailUseCase
    val getReleaseDefinitionUseCase: GetReleaseDefinitionUseCase
    val createReleaseUseCase: CreateReleaseUseCase
    val deployReleaseEnvironmentUseCase: DeployReleaseEnvironmentUseCase
}

expect val authBridge: AuthBridge

expect val pullRequestBridge: PullRequestBridge

expect val releaseBridge: ReleaseBridge
