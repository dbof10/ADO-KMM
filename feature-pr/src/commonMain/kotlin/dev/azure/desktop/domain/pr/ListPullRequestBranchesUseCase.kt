package dev.azure.desktop.domain.pr

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ListPullRequestBranchesUseCase(
    private val repository: PullRequestRepository,
) {
    suspend operator fun invoke(
        organization: String,
        projectName: String,
        repositoryId: String,
    ): Result<List<PullRequestBranchRef>> =
        withContext(Dispatchers.Default) {
            repository.listBranches(
                organization = organization.trim(),
                projectName = projectName.trim(),
                repositoryId = repositoryId.trim(),
            )
        }
}
