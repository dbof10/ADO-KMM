package dev.azure.desktop.pr.list

import com.freeletics.flowredux.dsl.FlowReduxStateMachine
import dev.azure.desktop.domain.pr.DevOpsProject
import dev.azure.desktop.domain.pr.GetDefaultProjectNameUseCase
import dev.azure.desktop.domain.pr.GetActivePullRequestsUseCase
import dev.azure.desktop.domain.pr.GetMyPullRequestsUseCase
import dev.azure.desktop.domain.pr.ListProjectsUseCase
import dev.azure.desktop.domain.pr.PullRequestSummary
import dev.azure.desktop.domain.pr.RecordProjectSelectedUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi

enum class PrListTab {
    Mine,
    Active,
}

sealed class PrListState {
    data object LoadingProjects : PrListState()

    data class ProjectsError(val message: String) : PrListState()

    data class LoadingPullRequests(
        val projects: List<DevOpsProject>,
        val selectedProjectName: String?,
        val tab: PrListTab,
    ) : PrListState()

    data class Ready(
        val projects: List<DevOpsProject>,
        val selectedProjectName: String?,
        val tab: PrListTab,
        val items: List<PullRequestSummary>,
    ) : PrListState()

    data class PullRequestsError(
        val projects: List<DevOpsProject>,
        val selectedProjectName: String?,
        val tab: PrListTab,
        val message: String,
    ) : PrListState()
}

sealed class PrListAction {
    data object RetryProjects : PrListAction()

    data object RefreshPullRequests : PrListAction()

    data class SelectTab(val tab: PrListTab) : PrListAction()

    /** `null` = all projects in the organization. */
    data class SelectProject(val projectName: String?) : PrListAction()
}

@OptIn(ExperimentalCoroutinesApi::class)
class PrListStateMachine(
    private val organization: String,
    private val listProjectsUseCase: ListProjectsUseCase,
    private val getMyPullRequestsUseCase: GetMyPullRequestsUseCase,
    private val getActivePullRequestsUseCase: GetActivePullRequestsUseCase,
    private val getDefaultProjectNameUseCase: GetDefaultProjectNameUseCase,
    private val recordProjectSelectedUseCase: RecordProjectSelectedUseCase,
) : FlowReduxStateMachine<PrListState, PrListAction>(PrListState.LoadingProjects) {
    init {
        spec {
            inState<PrListState.LoadingProjects> {
                onEnter { state ->
                    listProjectsUseCase(organization).fold(
                        onSuccess = { projects ->
                            val sorted = projects.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
                            val defaultProject =
                                getDefaultProjectNameUseCase(organization, sorted.map { it.name })
                                    .getOrNull()
                                    ?: sorted.firstOrNull()?.name
                            state.override {
                                PrListState.LoadingPullRequests(
                                    projects = sorted,
                                    selectedProjectName = defaultProject,
                                    tab = PrListTab.Mine,
                                )
                            }
                        },
                        onFailure = {
                            state.override {
                                PrListState.ProjectsError(it.message ?: "Failed to load projects.")
                            }
                        },
                    )
                }
            }

            inState<PrListState.LoadingPullRequests> {
                onEnter { state ->
                    val snap = state.snapshot
                    loadPullRequests(snap.tab, snap.selectedProjectName).fold(
                        onSuccess = { items ->
                            state.override {
                                PrListState.Ready(
                                    projects = snap.projects,
                                    selectedProjectName = snap.selectedProjectName,
                                    tab = snap.tab,
                                    items = items,
                                )
                            }
                        },
                        onFailure = {
                            state.override {
                                PrListState.PullRequestsError(
                                    projects = snap.projects,
                                    selectedProjectName = snap.selectedProjectName,
                                    tab = snap.tab,
                                    message = it.message ?: "Failed to load pull requests.",
                                )
                            }
                        },
                    )
                }
                on<PrListAction.SelectTab> { action, state ->
                    val snap = state.snapshot
                    if (action.tab == snap.tab) {
                        state.noChange()
                    } else {
                        state.override {
                            PrListState.LoadingPullRequests(
                                projects = snap.projects,
                                selectedProjectName = snap.selectedProjectName,
                                tab = action.tab,
                            )
                        }
                    }
                }
                on<PrListAction.SelectProject> { action, state ->
                    val snap = state.snapshot
                    if (action.projectName == snap.selectedProjectName) {
                        state.noChange()
                    } else {
                        action.projectName?.let { recordProjectSelectedUseCase(organization, it) }
                        state.override {
                            PrListState.LoadingPullRequests(
                                projects = snap.projects,
                                selectedProjectName = action.projectName,
                                tab = snap.tab,
                            )
                        }
                    }
                }
                on<PrListAction.RefreshPullRequests> { _, state ->
                    val snap = state.snapshot
                    state.override {
                        PrListState.LoadingPullRequests(
                            projects = snap.projects,
                            selectedProjectName = snap.selectedProjectName,
                            tab = snap.tab,
                        )
                    }
                }
            }

            inState<PrListState.Ready> {
                on<PrListAction.SelectTab> { action, state ->
                    val snap = state.snapshot
                    if (action.tab == snap.tab) {
                        state.noChange()
                    } else {
                        state.override {
                            PrListState.LoadingPullRequests(
                                projects = snap.projects,
                                selectedProjectName = snap.selectedProjectName,
                                tab = action.tab,
                            )
                        }
                    }
                }
                on<PrListAction.SelectProject> { action, state ->
                    val snap = state.snapshot
                    if (action.projectName == snap.selectedProjectName) {
                        state.noChange()
                    } else {
                        action.projectName?.let { recordProjectSelectedUseCase(organization, it) }
                        state.override {
                            PrListState.LoadingPullRequests(
                                projects = snap.projects,
                                selectedProjectName = action.projectName,
                                tab = snap.tab,
                            )
                        }
                    }
                }
                on<PrListAction.RefreshPullRequests> { _, state ->
                    val snap = state.snapshot
                    state.override {
                        PrListState.LoadingPullRequests(
                            projects = snap.projects,
                            selectedProjectName = snap.selectedProjectName,
                            tab = snap.tab,
                        )
                    }
                }
            }

            inState<PrListState.PullRequestsError> {
                on<PrListAction.SelectTab> { action, state ->
                    val snap = state.snapshot
                    if (action.tab == snap.tab) {
                        state.noChange()
                    } else {
                        state.override {
                            PrListState.LoadingPullRequests(
                                projects = snap.projects,
                                selectedProjectName = snap.selectedProjectName,
                                tab = action.tab,
                            )
                        }
                    }
                }
                on<PrListAction.SelectProject> { action, state ->
                    val snap = state.snapshot
                    if (action.projectName == snap.selectedProjectName) {
                        state.noChange()
                    } else {
                        action.projectName?.let { recordProjectSelectedUseCase(organization, it) }
                        state.override {
                            PrListState.LoadingPullRequests(
                                projects = snap.projects,
                                selectedProjectName = action.projectName,
                                tab = snap.tab,
                            )
                        }
                    }
                }
                on<PrListAction.RefreshPullRequests> { _, state ->
                    val snap = state.snapshot
                    state.override {
                        PrListState.LoadingPullRequests(
                            projects = snap.projects,
                            selectedProjectName = snap.selectedProjectName,
                            tab = snap.tab,
                        )
                    }
                }
            }

            inState<PrListState.ProjectsError> {
                on<PrListAction.RetryProjects> { _, state ->
                    state.override { PrListState.LoadingProjects }
                }
            }
        }
    }

    private suspend fun loadPullRequests(tab: PrListTab, projectName: String?): Result<List<PullRequestSummary>> =
        when (tab) {
            PrListTab.Mine -> getMyPullRequestsUseCase(organization, projectName)
            PrListTab.Active -> getActivePullRequestsUseCase(organization, projectName)
        }
}
