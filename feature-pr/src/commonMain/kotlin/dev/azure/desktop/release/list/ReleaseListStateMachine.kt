package dev.azure.desktop.release.list

import com.freeletics.flowredux.dsl.FlowReduxStateMachine
import dev.azure.desktop.domain.pr.DevOpsProject
import dev.azure.desktop.domain.release.ListReleaseDefinitionsUseCase
import dev.azure.desktop.domain.release.ListReleasesForDefinitionUseCase
import dev.azure.desktop.domain.release.ReleaseDefinitionSummary
import dev.azure.desktop.domain.release.ReleaseSummary
import dev.azure.desktop.domain.pr.ListProjectsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi

sealed class ReleaseListState {
    data object LoadingProjects : ReleaseListState()

    data class ProjectsError(val message: String) : ReleaseListState()

    data class LoadingDefinitions(
        val projects: List<DevOpsProject>,
        val selectedProjectName: String,
    ) : ReleaseListState()

    data class DefinitionsError(
        val projects: List<DevOpsProject>,
        val selectedProjectName: String,
        val message: String,
    ) : ReleaseListState()

    data class LoadingReleases(
        val projects: List<DevOpsProject>,
        val selectedProjectName: String,
        val definitions: List<ReleaseDefinitionSummary>,
        val selectedDefinitionId: Int,
    ) : ReleaseListState()

    data class ReleasesError(
        val projects: List<DevOpsProject>,
        val selectedProjectName: String,
        val definitions: List<ReleaseDefinitionSummary>,
        val selectedDefinitionId: Int,
        val message: String,
    ) : ReleaseListState()

    data class Ready(
        val projects: List<DevOpsProject>,
        val selectedProjectName: String,
        val definitions: List<ReleaseDefinitionSummary>,
        val selectedDefinitionId: Int,
        val releases: List<ReleaseSummary>,
    ) : ReleaseListState()
}

sealed class ReleaseListAction {
    data object RetryProjects : ReleaseListAction()

    data object Refresh : ReleaseListAction()

    /** `null` is ignored; use a concrete project name from the list. */
    data class SelectProject(val projectName: String?) : ReleaseListAction()

    data class SelectDefinition(val definitionId: Int) : ReleaseListAction()
}

@OptIn(ExperimentalCoroutinesApi::class)
class ReleaseListStateMachine(
    private val organization: String,
    private val listProjectsUseCase: ListProjectsUseCase,
    private val listReleaseDefinitionsUseCase: ListReleaseDefinitionsUseCase,
    private val listReleasesForDefinitionUseCase: ListReleasesForDefinitionUseCase,
) : FlowReduxStateMachine<ReleaseListState, ReleaseListAction>(ReleaseListState.LoadingProjects) {
    init {
        spec {
            inState<ReleaseListState.LoadingProjects> {
                onEnter { state ->
                    listProjectsUseCase(organization).fold(
                        onSuccess = { projects ->
                            val sorted =
                                projects.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
                            val first = sorted.firstOrNull()?.name
                            if (first.isNullOrBlank()) {
                                state.override {
                                    ReleaseListState.ProjectsError("No projects found in this organization.")
                                }
                            } else {
                                state.override {
                                    ReleaseListState.LoadingDefinitions(
                                        projects = sorted,
                                        selectedProjectName = first,
                                    )
                                }
                            }
                        },
                        onFailure = {
                            state.override {
                                ReleaseListState.ProjectsError(it.message ?: "Failed to load projects.")
                            }
                        },
                    )
                }
            }

            inState<ReleaseListState.LoadingDefinitions> {
                onEnter { state ->
                    val snap = state.snapshot
                    loadDefinitions(snap.selectedProjectName).fold(
                        onSuccess = { definitions ->
                            val sorted =
                                definitions.sortedWith(
                                    compareBy(String.CASE_INSENSITIVE_ORDER) { it.name },
                                )
                            val firstId = sorted.firstOrNull()?.id
                            if (firstId == null) {
                                state.override {
                                    ReleaseListState.DefinitionsError(
                                        projects = snap.projects,
                                        selectedProjectName = snap.selectedProjectName,
                                        message = "No release definitions in this project.",
                                    )
                                }
                            } else {
                                state.override {
                                    ReleaseListState.LoadingReleases(
                                        projects = snap.projects,
                                        selectedProjectName = snap.selectedProjectName,
                                        definitions = sorted,
                                        selectedDefinitionId = firstId,
                                    )
                                }
                            }
                        },
                        onFailure = {
                            state.override {
                                ReleaseListState.DefinitionsError(
                                    projects = snap.projects,
                                    selectedProjectName = snap.selectedProjectName,
                                    message = it.message ?: "Failed to load release definitions.",
                                )
                            }
                        },
                    )
                }
                on<ReleaseListAction.SelectProject> { action, state ->
                    val snap = state.snapshot
                    val name = action.projectName?.trim().orEmpty()
                    if (name.isBlank()) {
                        state.noChange()
                    } else if (name == snap.selectedProjectName) {
                        state.noChange()
                    } else {
                        state.override {
                            ReleaseListState.LoadingDefinitions(
                                projects = snap.projects,
                                selectedProjectName = name,
                            )
                        }
                    }
                }
                on<ReleaseListAction.Refresh> { _, state ->
                    val snap = state.snapshot
                    state.override {
                        ReleaseListState.LoadingDefinitions(
                            projects = snap.projects,
                            selectedProjectName = snap.selectedProjectName,
                        )
                    }
                }
            }

            inState<ReleaseListState.LoadingReleases> {
                onEnter { state ->
                    val snap = state.snapshot
                    loadReleases(snap.selectedProjectName, snap.selectedDefinitionId).fold(
                        onSuccess = { releases ->
                            state.override {
                                ReleaseListState.Ready(
                                    projects = snap.projects,
                                    selectedProjectName = snap.selectedProjectName,
                                    definitions = snap.definitions,
                                    selectedDefinitionId = snap.selectedDefinitionId,
                                    releases = releases,
                                )
                            }
                        },
                        onFailure = {
                            state.override {
                                ReleaseListState.ReleasesError(
                                    projects = snap.projects,
                                    selectedProjectName = snap.selectedProjectName,
                                    definitions = snap.definitions,
                                    selectedDefinitionId = snap.selectedDefinitionId,
                                    message = it.message ?: "Failed to load releases.",
                                )
                            }
                        },
                    )
                }
                on<ReleaseListAction.SelectProject> { action, state ->
                    val snap = state.snapshot
                    val name = action.projectName?.trim().orEmpty()
                    if (name.isBlank() || name == snap.selectedProjectName) {
                        state.noChange()
                    } else {
                        state.override {
                            ReleaseListState.LoadingDefinitions(
                                projects = snap.projects,
                                selectedProjectName = name,
                            )
                        }
                    }
                }
                on<ReleaseListAction.SelectDefinition> { action, state ->
                    val snap = state.snapshot
                    if (action.definitionId == snap.selectedDefinitionId) {
                        state.noChange()
                    } else {
                        state.override {
                            ReleaseListState.LoadingReleases(
                                projects = snap.projects,
                                selectedProjectName = snap.selectedProjectName,
                                definitions = snap.definitions,
                                selectedDefinitionId = action.definitionId,
                            )
                        }
                    }
                }
                on<ReleaseListAction.Refresh> { _, state ->
                    val snap = state.snapshot
                    state.override {
                        ReleaseListState.LoadingReleases(
                            projects = snap.projects,
                            selectedProjectName = snap.selectedProjectName,
                            definitions = snap.definitions,
                            selectedDefinitionId = snap.selectedDefinitionId,
                        )
                    }
                }
            }

            inState<ReleaseListState.Ready> {
                on<ReleaseListAction.SelectProject> { action, state ->
                    val snap = state.snapshot
                    val name = action.projectName?.trim().orEmpty()
                    if (name.isBlank() || name == snap.selectedProjectName) {
                        state.noChange()
                    } else {
                        state.override {
                            ReleaseListState.LoadingDefinitions(
                                projects = snap.projects,
                                selectedProjectName = name,
                            )
                        }
                    }
                }
                on<ReleaseListAction.SelectDefinition> { action, state ->
                    val snap = state.snapshot
                    if (action.definitionId == snap.selectedDefinitionId) {
                        state.noChange()
                    } else {
                        state.override {
                            ReleaseListState.LoadingReleases(
                                projects = snap.projects,
                                selectedProjectName = snap.selectedProjectName,
                                definitions = snap.definitions,
                                selectedDefinitionId = action.definitionId,
                            )
                        }
                    }
                }
                on<ReleaseListAction.Refresh> { _, state ->
                    val snap = state.snapshot
                    state.override {
                        ReleaseListState.LoadingReleases(
                            projects = snap.projects,
                            selectedProjectName = snap.selectedProjectName,
                            definitions = snap.definitions,
                            selectedDefinitionId = snap.selectedDefinitionId,
                        )
                    }
                }
            }

            inState<ReleaseListState.DefinitionsError> {
                on<ReleaseListAction.SelectProject> { action, state ->
                    val snap = state.snapshot
                    val name = action.projectName?.trim().orEmpty()
                    if (name.isBlank() || name == snap.selectedProjectName) {
                        state.noChange()
                    } else {
                        state.override {
                            ReleaseListState.LoadingDefinitions(
                                projects = snap.projects,
                                selectedProjectName = name,
                            )
                        }
                    }
                }
                on<ReleaseListAction.Refresh> { _, state ->
                    val snap = state.snapshot
                    state.override {
                        ReleaseListState.LoadingDefinitions(
                            projects = snap.projects,
                            selectedProjectName = snap.selectedProjectName,
                        )
                    }
                }
                on<ReleaseListAction.RetryProjects> { _, state ->
                    state.override { ReleaseListState.LoadingProjects }
                }
            }

            inState<ReleaseListState.ReleasesError> {
                on<ReleaseListAction.SelectProject> { action, state ->
                    val snap = state.snapshot
                    val name = action.projectName?.trim().orEmpty()
                    if (name.isBlank() || name == snap.selectedProjectName) {
                        state.noChange()
                    } else {
                        state.override {
                            ReleaseListState.LoadingDefinitions(
                                projects = snap.projects,
                                selectedProjectName = name,
                            )
                        }
                    }
                }
                on<ReleaseListAction.SelectDefinition> { action, state ->
                    val snap = state.snapshot
                    if (action.definitionId == snap.selectedDefinitionId) {
                        state.noChange()
                    } else {
                        state.override {
                            ReleaseListState.LoadingReleases(
                                projects = snap.projects,
                                selectedProjectName = snap.selectedProjectName,
                                definitions = snap.definitions,
                                selectedDefinitionId = action.definitionId,
                            )
                        }
                    }
                }
                on<ReleaseListAction.Refresh> { _, state ->
                    val snap = state.snapshot
                    state.override {
                        ReleaseListState.LoadingReleases(
                            projects = snap.projects,
                            selectedProjectName = snap.selectedProjectName,
                            definitions = snap.definitions,
                            selectedDefinitionId = snap.selectedDefinitionId,
                        )
                    }
                }
            }

            inState<ReleaseListState.ProjectsError> {
                on<ReleaseListAction.RetryProjects> { _, state ->
                    state.override { ReleaseListState.LoadingProjects }
                }
            }
        }
    }

    private suspend fun loadDefinitions(projectName: String): Result<List<ReleaseDefinitionSummary>> =
        listReleaseDefinitionsUseCase(organization, projectName)

    private suspend fun loadReleases(
        projectName: String,
        definitionId: Int,
    ): Result<List<ReleaseSummary>> =
        listReleasesForDefinitionUseCase(organization, projectName, definitionId)
}
