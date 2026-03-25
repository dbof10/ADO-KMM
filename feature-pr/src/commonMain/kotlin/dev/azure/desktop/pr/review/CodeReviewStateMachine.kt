package dev.azure.desktop.pr.review

import com.freeletics.flowredux.dsl.FlowReduxStateMachine
import dev.azure.desktop.domain.pr.GetPullRequestDetailUseCase
import dev.azure.desktop.domain.pr.GetPullRequestFileDiffUseCase
import dev.azure.desktop.domain.pr.PullRequestChange
import dev.azure.desktop.domain.pr.PullRequestDiffLine
import dev.azure.desktop.domain.pr.PullRequestSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi

sealed class CodeReviewState {
    data object Loading : CodeReviewState()

    data class Content(
        val pullRequest: PullRequestSummary,
        val baseCommitId: String,
        val targetCommitId: String,
        val changes: List<PullRequestChange>,
        val selectedPath: String?,
        val diffLines: List<PullRequestDiffLine>,
    ) : CodeReviewState()

    data class Error(val message: String) : CodeReviewState()
}

sealed class CodeReviewAction {
    data object Refresh : CodeReviewAction()

    data class SelectFile(val path: String) : CodeReviewAction()
}

@OptIn(ExperimentalCoroutinesApi::class)
class CodeReviewStateMachine(
    private val organization: String,
    private val summary: PullRequestSummary,
    private val getPullRequestDetailUseCase: GetPullRequestDetailUseCase,
    private val getPullRequestFileDiffUseCase: GetPullRequestFileDiffUseCase,
    private val getPullRequestChanges: suspend (
        organization: String,
        projectName: String,
        repositoryId: String,
        pullRequestId: Int,
        baseCommitId: String,
        targetCommitId: String,
    ) -> Result<List<PullRequestChange>>,
) : FlowReduxStateMachine<CodeReviewState, CodeReviewAction>(CodeReviewState.Loading) {
    init {
        spec {
            inState<CodeReviewState.Loading> {
                onEnter { state -> loadEverythingAndSelectFirst(state) }
                on<CodeReviewAction.Refresh> { _, state -> loadEverythingAndSelectFirst(state) }
            }

            inState<CodeReviewState.Content> {
                on<CodeReviewAction.Refresh> { _, state -> state.override { CodeReviewState.Loading } }

                on<CodeReviewAction.SelectFile> { action, state ->
                    val current = state.snapshot
                    val selected = action.path

                    val diff =
                        getPullRequestFileDiffUseCase(
                            organization = organization,
                            projectName = summary.projectName,
                            repositoryId = summary.repositoryId,
                            baseCommitId = current.baseCommitId,
                            targetCommitId = current.targetCommitId,
                            path = selected,
                        ).getOrElse { err ->
                            return@on state.override { CodeReviewState.Error(err.message ?: "Failed to load diff.") }
                        }

                    return@on state.override { current.copy(selectedPath = selected, diffLines = diff) }
                }
            }

            inState<CodeReviewState.Error> {
                on<CodeReviewAction.Refresh> { _, state -> state.override { CodeReviewState.Loading } }
            }
        }
    }

    private suspend fun loadEverythingAndSelectFirst(
        state: com.freeletics.flowredux.dsl.State<CodeReviewState.Loading>,
    ): com.freeletics.flowredux.dsl.ChangedState<CodeReviewState> {
        val detail =
            getPullRequestDetailUseCase(
                organization = organization,
                projectName = summary.projectName,
                repositoryId = summary.repositoryId,
                pullRequestId = summary.id,
            ).getOrElse { err ->
                return state.override { CodeReviewState.Error(err.message ?: "Failed to load pull request.") }
            }

        val baseCommit = detail.lastMergeTargetCommitId
        val targetCommit = detail.lastMergeSourceCommitId
        if (baseCommit.isNullOrBlank() || targetCommit.isNullOrBlank()) {
            return state.override { CodeReviewState.Error("Missing pull request commit information.") }
        }

        val changes =
            getPullRequestChanges(
                organization,
                summary.projectName,
                summary.repositoryId,
                summary.id,
                baseCommit,
                targetCommit,
            ).getOrElse { err ->
                return state.override { CodeReviewState.Error(err.message ?: "Failed to load pull request changes.") }
            }.sortedBy { it.path }

        val selected = changes.firstOrNull()?.path
        val diffLines =
            if (selected == null) {
                emptyList()
            } else {
                getPullRequestFileDiffUseCase(
                    organization = organization,
                    projectName = summary.projectName,
                    repositoryId = summary.repositoryId,
                    baseCommitId = baseCommit,
                    targetCommitId = targetCommit,
                    path = selected,
                ).getOrElse { emptyList() }
            }

        return state.override {
            CodeReviewState.Content(
                pullRequest = summary,
                baseCommitId = baseCommit,
                targetCommitId = targetCommit,
                changes = changes,
                selectedPath = selected,
                diffLines = diffLines,
            )
        }
    }
}

