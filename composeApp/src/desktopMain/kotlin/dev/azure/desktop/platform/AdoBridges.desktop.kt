package dev.azure.desktop.platform

import dev.azure.desktop.data.auth.JvmAuthServices
import dev.azure.desktop.data.pr.JvmPullRequestServices
import dev.azure.desktop.data.release.JvmReleaseServices
import javax.swing.SwingUtilities

actual val authBridge: AuthBridge =
    object : AuthBridge {
        override val patStorage get() = JvmAuthServices.patStorage
        override val verifyAndStorePat get() = JvmAuthServices.verifyAndStorePat
        override fun setOnSessionUnauthorized(handler: () -> Unit) {
            JvmAuthServices.setOnSessionUnauthorized(handler)
        }

        override fun runOnMainThread(block: () -> Unit) {
            SwingUtilities.invokeLater(block)
        }
    }

actual val pullRequestBridge: PullRequestBridge =
    object : PullRequestBridge {
        override val listProjectsUseCase get() = JvmPullRequestServices.listProjectsUseCase
        override val getMyPullRequestsUseCase get() = JvmPullRequestServices.getMyPullRequestsUseCase
        override val getActivePullRequestsUseCase get() = JvmPullRequestServices.getActivePullRequestsUseCase
        override val findPullRequestSummaryByIdUseCase get() = JvmPullRequestServices.findPullRequestSummaryByIdUseCase
        override val getPullRequestSummaryByIdUseCase get() = JvmPullRequestServices.getPullRequestSummaryByIdUseCase
        override val getMostSelectedProjectUseCase get() = JvmPullRequestServices.getMostSelectedProjectUseCase
        override val incrementProjectSelectionUseCase get() = JvmPullRequestServices.incrementProjectSelectionUseCase
        override val clearProjectSelectionCountsUseCase get() = JvmPullRequestServices.clearProjectSelectionCountsUseCase
        override val findCreatePullRequestSuggestionUseCase get() = JvmPullRequestServices.findCreatePullRequestSuggestionUseCase
        override val listPullRequestRepositoriesUseCase get() = JvmPullRequestServices.listPullRequestRepositoriesUseCase
        override val listPullRequestBranchesUseCase get() = JvmPullRequestServices.listPullRequestBranchesUseCase
        override val createPullRequestUseCase get() = JvmPullRequestServices.createPullRequestUseCase
        override val getPullRequestDetailUseCase get() = JvmPullRequestServices.getPullRequestDetailUseCase
        override val setMyPullRequestVoteUseCase get() = JvmPullRequestServices.setMyPullRequestVoteUseCase
        override val abandonPullRequestUseCase get() = JvmPullRequestServices.abandonPullRequestUseCase
        override val enablePullRequestAutoCompleteUseCase get() = JvmPullRequestServices.enablePullRequestAutoCompleteUseCase
        override val getPullRequestFileDiffUseCase get() = JvmPullRequestServices.getPullRequestFileDiffUseCase

        override suspend fun getPullRequestChanges(
            organization: String,
            projectName: String,
            repositoryId: String,
            pullRequestId: Int,
            baseCommitId: String,
            targetCommitId: String,
        ) = JvmPullRequestServices.getPullRequestChanges(
            organization = organization,
            projectName = projectName,
            repositoryId = repositoryId,
            pullRequestId = pullRequestId,
            baseCommitId = baseCommitId,
            targetCommitId = targetCommitId,
        )

        override suspend fun fetchAuthenticatedDevOpsResource(url: String) =
            JvmPullRequestServices.fetchAuthenticatedDevOpsResource(url)
    }

actual val releaseBridge: ReleaseBridge =
    object : ReleaseBridge {
        override val listReleaseDefinitionsUseCase get() = JvmReleaseServices.listReleaseDefinitionsUseCase
        override val listReleasesForDefinitionUseCase get() = JvmReleaseServices.listReleasesForDefinitionUseCase
        override val getReleaseDetailUseCase get() = JvmReleaseServices.getReleaseDetailUseCase
        override val getReleaseDefinitionUseCase get() = JvmReleaseServices.getReleaseDefinitionUseCase
        override val createReleaseUseCase get() = JvmReleaseServices.createReleaseUseCase
        override val deployReleaseEnvironmentUseCase get() = JvmReleaseServices.deployReleaseEnvironmentUseCase
    }
