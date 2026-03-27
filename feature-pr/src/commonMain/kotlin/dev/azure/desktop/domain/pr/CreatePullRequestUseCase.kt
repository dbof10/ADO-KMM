package dev.azure.desktop.domain.pr

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CreatePullRequestUseCase(
    private val repository: PullRequestRepository,
) {
    suspend operator fun invoke(params: CreatePullRequestParams): Result<CreatedPullRequest> =
        withContext(Dispatchers.Default) {
            repository.createPullRequest(
                params.copy(
                    organization = params.organization.trim(),
                    projectName = params.projectName.trim(),
                    repositoryId = params.repositoryId.trim(),
                    sourceBranchName = params.sourceBranchName.trim(),
                    targetBranchName = params.targetBranchName.trim(),
                    title = params.title.trim(),
                    description = params.description.trim(),
                ),
            )
        }
}
