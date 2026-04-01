package dev.azure.desktop.platform

import android.os.Handler
import android.os.Looper
import dev.azure.desktop.data.auth.AndroidAuthServices
import dev.azure.desktop.data.pr.AndroidPullRequestServices
import dev.azure.desktop.data.release.AndroidReleaseServices

actual val authBridge: AuthBridge =
    object : AuthBridge {
        override val patStorage get() = AndroidAuthServices.patStorage
        override val verifyAndStorePat get() = AndroidAuthServices.verifyAndStorePat
        override fun setOnSessionUnauthorized(handler: () -> Unit) {
            AndroidAuthServices.setOnSessionUnauthorized(handler)
        }

        override fun runOnMainThread(block: () -> Unit) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                block()
            } else {
                Handler(Looper.getMainLooper()).post(block)
            }
        }
    }

actual val pullRequestBridge: PullRequestBridge =
    object : PullRequestBridge {
        override val listProjectsUseCase get() = AndroidPullRequestServices.listProjectsUseCase
        override val getMyPullRequestsUseCase get() = AndroidPullRequestServices.getMyPullRequestsUseCase
        override val getActivePullRequestsUseCase get() = AndroidPullRequestServices.getActivePullRequestsUseCase
        override val findPullRequestSummaryByIdUseCase get() = AndroidPullRequestServices.findPullRequestSummaryByIdUseCase
        override val getPullRequestSummaryByIdUseCase get() = AndroidPullRequestServices.getPullRequestSummaryByIdUseCase
        override val getMostSelectedProjectUseCase get() = AndroidPullRequestServices.getMostSelectedProjectUseCase
        override val incrementProjectSelectionUseCase get() = AndroidPullRequestServices.incrementProjectSelectionUseCase
        override val clearProjectSelectionCountsUseCase get() = AndroidPullRequestServices.clearProjectSelectionCountsUseCase
        override val findCreatePullRequestSuggestionUseCase get() = AndroidPullRequestServices.findCreatePullRequestSuggestionUseCase
        override val listPullRequestRepositoriesUseCase get() = AndroidPullRequestServices.listPullRequestRepositoriesUseCase
        override val listPullRequestBranchesUseCase get() = AndroidPullRequestServices.listPullRequestBranchesUseCase
        override val createPullRequestUseCase get() = AndroidPullRequestServices.createPullRequestUseCase
        override val getPullRequestDetailUseCase get() = AndroidPullRequestServices.getPullRequestDetailUseCase
        override val setMyPullRequestVoteUseCase get() = AndroidPullRequestServices.setMyPullRequestVoteUseCase
        override val abandonPullRequestUseCase get() = AndroidPullRequestServices.abandonPullRequestUseCase
        override val enablePullRequestAutoCompleteUseCase get() = AndroidPullRequestServices.enablePullRequestAutoCompleteUseCase
        override val getPullRequestFileDiffUseCase get() = AndroidPullRequestServices.getPullRequestFileDiffUseCase

        override suspend fun getPullRequestChanges(
            organization: String,
            projectName: String,
            repositoryId: String,
            pullRequestId: Int,
            baseCommitId: String,
            targetCommitId: String,
        ) = AndroidPullRequestServices.getPullRequestChanges(
            organization = organization,
            projectName = projectName,
            repositoryId = repositoryId,
            pullRequestId = pullRequestId,
            baseCommitId = baseCommitId,
            targetCommitId = targetCommitId,
        )

        override suspend fun fetchAuthenticatedDevOpsResource(url: String) =
            AndroidPullRequestServices.fetchAuthenticatedDevOpsResource(url)
    }

actual val releaseBridge: ReleaseBridge =
    object : ReleaseBridge {
        override val listReleaseDefinitionsUseCase get() = AndroidReleaseServices.listReleaseDefinitionsUseCase
        override val listReleasesForDefinitionUseCase get() = AndroidReleaseServices.listReleasesForDefinitionUseCase
        override val getReleaseDetailUseCase get() = AndroidReleaseServices.getReleaseDetailUseCase
        override val getReleaseDefinitionUseCase get() = AndroidReleaseServices.getReleaseDefinitionUseCase
        override val createReleaseUseCase get() = AndroidReleaseServices.createReleaseUseCase
        override val deployReleaseEnvironmentUseCase get() = AndroidReleaseServices.deployReleaseEnvironmentUseCase
    }
