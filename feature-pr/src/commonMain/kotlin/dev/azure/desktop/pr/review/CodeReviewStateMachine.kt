package dev.azure.desktop.pr.review

import com.freeletics.flowredux.dsl.ExecutionPolicy
import com.freeletics.flowredux.dsl.FlowReduxStateMachine
import dev.azure.desktop.domain.pr.GetPullRequestDetailUseCase
import dev.azure.desktop.domain.pr.GetPullRequestFileDiffUseCase
import dev.azure.desktop.domain.pr.PullRequestChange
import dev.azure.desktop.domain.pr.PullRequestCheckStatus
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
        val checks: List<PullRequestCheckStatus>,
        /** True while the diff for [selectedPath] is being loaded after a file change. */
        val isDiffLoading: Boolean = false,
    ) : CodeReviewState()

    data class Error(val message: String) : CodeReviewState()
}

sealed class CodeReviewAction {
    data object Refresh : CodeReviewAction()

    data class SelectFile(val path: String) : CodeReviewAction()

    internal data class FileDiffLoaded(val path: String, val lines: List<PullRequestDiffLine>) : CodeReviewAction()

    internal data class FileDiffLoadFailed(val path: String, val message: String) : CodeReviewAction()
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
    /**
     * In-memory diff lines per file path for this review session only.
     * The [CodeReviewStateMachine] instance is scoped in Compose to the open PR, so this map
     * is discarded when the user navigates away or selects another PR.
     */
    private val fileDiffCache = mutableMapOf<String, List<PullRequestDiffLine>>()

    init {
        spec {
            inState<CodeReviewState.Loading> {
                onEnter { state -> loadEverythingAndSelectFirst(state) }
                on<CodeReviewAction.Refresh> { _, state -> loadEverythingAndSelectFirst(state) }
                on<CodeReviewAction.FileDiffLoaded> { _, state -> state.noChange() }
                on<CodeReviewAction.FileDiffLoadFailed> { _, state -> state.noChange() }
            }

            inState<CodeReviewState.Content> {
                on<CodeReviewAction.Refresh> { _, state -> state.override { CodeReviewState.Loading } }

                // Single synchronous transition so the tree + toolbar show the new file immediately,
                // then the diff pane shows loading until FileDiffLoaded arrives (see onActionEffect).
                on<CodeReviewAction.SelectFile> { action, state ->
                    val current = state.snapshot
                    val path = action.path
                    val cached = fileDiffCache[path]
                    return@on if (cached != null) {
                        state.override {
                            current.copy(
                                selectedPath = path,
                                diffLines = cached,
                                isDiffLoading = false,
                            )
                        }
                    } else {
                        state.override {
                            current.copy(
                                selectedPath = path,
                                diffLines = emptyList(),
                                isDiffLoading = true,
                            )
                        }
                    }
                }

                on<CodeReviewAction.FileDiffLoaded> { action, state ->
                    val current = state.snapshot
                    if (current.selectedPath != action.path) return@on state.noChange()
                    return@on state.override {
                        current.copy(diffLines = action.lines, isDiffLoading = false)
                    }
                }

                on<CodeReviewAction.FileDiffLoadFailed> { action, state ->
                    val current = state.snapshot
                    if (current.selectedPath != action.path) return@on state.noChange()
                    return@on state.override { CodeReviewState.Error(action.message) }
                }

                // Use BaseBuilderBlock overload (ExecutionPolicy-only); KClass + policy overload is internal in FlowRedux.
                onActionEffect(ExecutionPolicy.CANCEL_PREVIOUS) { action: CodeReviewAction.SelectFile, content: CodeReviewState.Content ->
                    val path = action.path
                    if (fileDiffCache.containsKey(path)) {
                        return@onActionEffect
                    }
                    getPullRequestFileDiffUseCase(
                        organization = organization,
                        projectName = summary.projectName,
                        repositoryId = summary.repositoryId,
                        baseCommitId = content.baseCommitId,
                        targetCommitId = content.targetCommitId,
                        path = path,
                    ).fold(
                        onSuccess = { lines ->
                            fileDiffCache[path] = lines
                            this@CodeReviewStateMachine.dispatch(CodeReviewAction.FileDiffLoaded(path, lines))
                        },
                        onFailure = { err ->
                            this@CodeReviewStateMachine.dispatch(
                                CodeReviewAction.FileDiffLoadFailed(
                                    path,
                                    err.message ?: "Failed to load diff.",
                                ),
                            )
                        },
                    )
                }
            }

            inState<CodeReviewState.Error> {
                on<CodeReviewAction.Refresh> { _, state -> state.override { CodeReviewState.Loading } }
                on<CodeReviewAction.FileDiffLoaded> { _, state -> state.noChange() }
                on<CodeReviewAction.FileDiffLoadFailed> { _, state -> state.noChange() }
            }
        }
    }

    private suspend fun loadEverythingAndSelectFirst(
        state: com.freeletics.flowredux.dsl.State<CodeReviewState.Loading>,
    ): com.freeletics.flowredux.dsl.ChangedState<CodeReviewState> {
        fileDiffCache.clear()

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

        val changesWithDiff =
            changes.filter { change ->
                // Only drop items we are sure show an empty diff. If fetching diff fails,
                // keep the item so the user can still select it and see the error.
                val diffResult =
                    getPullRequestFileDiffUseCase(
                        organization = organization,
                        projectName = summary.projectName,
                        repositoryId = summary.repositoryId,
                        baseCommitId = baseCommit,
                        targetCommitId = targetCommit,
                        path = change.path,
                    )
                diffResult.fold(
                    onSuccess = { lines -> lines.isNotEmpty() },
                    onFailure = { true },
                )
            }

        val selected = changesWithDiff.firstOrNull()?.path
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
                ).fold(
                    onSuccess = { lines ->
                        fileDiffCache[selected] = lines
                        lines
                    },
                    onFailure = { emptyList() },
                )
            }

        return state.override {
            CodeReviewState.Content(
                pullRequest = summary,
                baseCommitId = baseCommit,
                targetCommitId = targetCommit,
                changes = changesWithDiff,
                selectedPath = selected,
                diffLines = diffLines,
                checks = detail.checks,
                isDiffLoading = false,
            )
        }
    }
}

