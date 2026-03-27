package dev.azure.desktop.domain.pr

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ListPullRequestRepositoriesUseCase(
    private val repository: PullRequestRepository,
) {
    suspend operator fun invoke(organization: String, projectName: String): Result<List<PullRequestRepositoryRef>> =
        withContext(Dispatchers.Default) {
            repository.listRepositories(
                organization = organization.trim(),
                projectName = projectName.trim(),
            )
        }
}
