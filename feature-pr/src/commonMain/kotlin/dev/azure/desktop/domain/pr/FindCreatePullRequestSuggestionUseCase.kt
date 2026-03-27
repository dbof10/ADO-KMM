package dev.azure.desktop.domain.pr

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FindCreatePullRequestSuggestionUseCase(
    private val repository: PullRequestRepository,
) {
    suspend operator fun invoke(organization: String, projectName: String?): Result<PullRequestSuggestion?> =
        withContext(Dispatchers.Default) {
            val project = projectName?.trim()?.takeIf { it.isNotBlank() }
            PrSuggestionLog.d("useCase: start org=${organization.trim()} project=${project ?: "(all, capped)"}")
            repository.findCreatePullRequestSuggestion(
                organization = organization.trim(),
                projectName = project,
            ).also { result ->
                result.fold(
                    onSuccess = { suggestion ->
                        if (suggestion == null) {
                            PrSuggestionLog.d("useCase: success, no suggestion (null)")
                        } else {
                            PrSuggestionLog.d(
                                "useCase: success, suggestion branch=${suggestion.sourceBranchName} " +
                                    "repo=${suggestion.repositoryName} project=${suggestion.projectName}",
                            )
                        }
                    },
                    onFailure = { e ->
                        PrSuggestionLog.d("useCase: failure ${e.message ?: e::class.simpleName}")
                    },
                )
            }
        }
}
