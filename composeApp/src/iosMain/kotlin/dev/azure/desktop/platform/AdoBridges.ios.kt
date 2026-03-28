package dev.azure.desktop.platform

import dev.azure.desktop.data.auth.IosAuthServices
import dev.azure.desktop.data.pr.IosPullRequestServices
import dev.azure.desktop.data.release.IosReleaseServices
import kotlinx.cinterop.ExperimentalForeignApi
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

actual val authBridge: AuthBridge =
    object : AuthBridge {
        override val patStorage get() = IosAuthServices.patStorage
        override val verifyAndStorePat get() = IosAuthServices.verifyAndStorePat
        override fun setOnSessionUnauthorized(handler: () -> Unit) {
            IosAuthServices.setOnSessionUnauthorized(handler)
        }

        @OptIn(ExperimentalForeignApi::class)
        override fun runOnMainThread(block: () -> Unit) {
            dispatch_async(dispatch_get_main_queue()) {
                block()
            }
        }
    }

actual val pullRequestBridge: PullRequestBridge =
    object : PullRequestBridge {
        override val listProjectsUseCase get() = IosPullRequestServices.listProjectsUseCase
        override val getMyPullRequestsUseCase get() = IosPullRequestServices.getMyPullRequestsUseCase
        override val getActivePullRequestsUseCase get() = IosPullRequestServices.getActivePullRequestsUseCase
        override val findPullRequestSummaryByIdUseCase get() = IosPullRequestServices.findPullRequestSummaryByIdUseCase
        override val getPullRequestSummaryByIdUseCase get() = IosPullRequestServices.getPullRequestSummaryByIdUseCase
        override val getMostSelectedProjectUseCase get() = IosPullRequestServices.getMostSelectedProjectUseCase
        override val incrementProjectSelectionUseCase get() = IosPullRequestServices.incrementProjectSelectionUseCase
        override val clearProjectSelectionCountsUseCase get() = IosPullRequestServices.clearProjectSelectionCountsUseCase
        override val findCreatePullRequestSuggestionUseCase get() = IosPullRequestServices.findCreatePullRequestSuggestionUseCase
        override val listPullRequestRepositoriesUseCase get() = IosPullRequestServices.listPullRequestRepositoriesUseCase
        override val listPullRequestBranchesUseCase get() = IosPullRequestServices.listPullRequestBranchesUseCase
        override val createPullRequestUseCase get() = IosPullRequestServices.createPullRequestUseCase
        override val getPullRequestDetailUseCase get() = IosPullRequestServices.getPullRequestDetailUseCase
        override val setMyPullRequestVoteUseCase get() = IosPullRequestServices.setMyPullRequestVoteUseCase
        override val abandonPullRequestUseCase get() = IosPullRequestServices.abandonPullRequestUseCase
        override val getPullRequestFileDiffUseCase get() = IosPullRequestServices.getPullRequestFileDiffUseCase

        override suspend fun getPullRequestChanges(
            organization: String,
            projectName: String,
            repositoryId: String,
            pullRequestId: Int,
            baseCommitId: String,
            targetCommitId: String,
        ) = IosPullRequestServices.getPullRequestChanges(
            organization = organization,
            projectName = projectName,
            repositoryId = repositoryId,
            pullRequestId = pullRequestId,
            baseCommitId = baseCommitId,
            targetCommitId = targetCommitId,
        )

        override suspend fun fetchAuthenticatedDevOpsResource(url: String) =
            IosPullRequestServices.fetchAuthenticatedDevOpsResource(url)
    }

actual val releaseBridge: ReleaseBridge =
    object : ReleaseBridge {
        override val listReleaseDefinitionsUseCase get() = IosReleaseServices.listReleaseDefinitionsUseCase
        override val listReleasesForDefinitionUseCase get() = IosReleaseServices.listReleasesForDefinitionUseCase
        override val getReleaseDetailUseCase get() = IosReleaseServices.getReleaseDetailUseCase
        override val getReleaseDefinitionUseCase get() = IosReleaseServices.getReleaseDefinitionUseCase
        override val createReleaseUseCase get() = IosReleaseServices.createReleaseUseCase
        override val deployReleaseEnvironmentUseCase get() = IosReleaseServices.deployReleaseEnvironmentUseCase
    }
