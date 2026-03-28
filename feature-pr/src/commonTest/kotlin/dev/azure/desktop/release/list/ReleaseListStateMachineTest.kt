package dev.azure.desktop.release.list

import dev.azure.desktop.domain.pr.DevOpsProject
import dev.azure.desktop.domain.pr.GetMostSelectedProjectUseCase
import dev.azure.desktop.domain.pr.IncrementProjectSelectionUseCase
import dev.azure.desktop.domain.pr.ListProjectsUseCase
import dev.azure.desktop.domain.release.ListReleaseDefinitionsUseCase
import dev.azure.desktop.domain.release.ListReleasesForDefinitionUseCase
import dev.azure.desktop.domain.release.ReleaseDefinitionSummary
import dev.azure.desktop.domain.release.ReleaseRepository
import dev.azure.desktop.domain.release.ReleaseSummary
import dev.azure.desktop.pr.InMemoryProjectSelectionStorage
import dev.azure.desktop.pr.StubPullRequestRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ReleaseListStateMachineTest {
    @Test
    fun loadsProjectsDefinitionsAndReleases() =
        runBlocking {
            val prRepo =
                StubPullRequestRepository().apply {
                    projectsResult =
                        Result.success(listOf(DevOpsProject("1", "MyProject")))
                }
            val relRepo =
                StubReleaseRepository().apply {
                    definitionsResult =
                        Result.success(
                            listOf(
                                ReleaseDefinitionSummary(id = 9, name = "Nightly", subtitle = "Prod"),
                            ),
                        )
                    releasesResult =
                        Result.success(
                            listOf(
                                ReleaseSummary(
                                    id = 100,
                                    name = "Release 1",
                                    status = "active",
                                    definitionId = 9,
                                    definitionName = "Nightly",
                                    projectName = "MyProject",
                                    createdOnIso = "2024-01-01T00:00:00Z",
                                    commitShort = null,
                                    branchLabel = null,
                                    stages = emptyList(),
                                ),
                            ),
                        )
                }
            val storage = InMemoryProjectSelectionStorage()
            val machine =
                ReleaseListStateMachine(
                    organization = "fab",
                    listProjectsUseCase = ListProjectsUseCase(prRepo),
                    listReleaseDefinitionsUseCase = ListReleaseDefinitionsUseCase(relRepo),
                    listReleasesForDefinitionUseCase = ListReleasesForDefinitionUseCase(relRepo),
                    getMostSelectedProjectUseCase = GetMostSelectedProjectUseCase(storage),
                    incrementProjectSelectionUseCase = IncrementProjectSelectionUseCase(storage),
                )

            val ready = machine.state.first { it is ReleaseListState.Ready }
            val r = assertIs<ReleaseListState.Ready>(ready)
            assertEquals("MyProject", r.selectedProjectName)
            assertEquals(9, r.selectedDefinitionId)
            assertEquals(100, r.releases.single().id)
        }

    @Test
    fun projectsErrorRetryProjectsReloads() =
        runBlocking {
            val prRepo = StubPullRequestRepository()
            prRepo.projectsResult = Result.failure(Exception("net"))
            val relRepo = StubReleaseRepository()
            val storage = InMemoryProjectSelectionStorage()
            val machine =
                ReleaseListStateMachine(
                    organization = "fab",
                    listProjectsUseCase = ListProjectsUseCase(prRepo),
                    listReleaseDefinitionsUseCase = ListReleaseDefinitionsUseCase(relRepo),
                    listReleasesForDefinitionUseCase = ListReleasesForDefinitionUseCase(relRepo),
                    getMostSelectedProjectUseCase = GetMostSelectedProjectUseCase(storage),
                    incrementProjectSelectionUseCase = IncrementProjectSelectionUseCase(storage),
                )

            machine.state.first { it is ReleaseListState.ProjectsError }

            prRepo.projectsResult = Result.success(listOf(DevOpsProject("1", "P")))
            relRepo.definitionsResult =
                Result.success(listOf(ReleaseDefinitionSummary(1, "D", null)))
            relRepo.releasesResult =
                Result.success(
                    listOf(
                        ReleaseSummary(
                            id = 1,
                            name = "R",
                            status = null,
                            definitionId = 1,
                            definitionName = "D",
                            projectName = "P",
                            createdOnIso = null,
                            commitShort = null,
                            branchLabel = null,
                            stages = emptyList(),
                        ),
                    ),
                )
            launch { machine.dispatch(ReleaseListAction.RetryProjects) }

            assertIs<ReleaseListState.Ready>(machine.state.first { it is ReleaseListState.Ready })
        }
}

private class StubReleaseRepository : ReleaseRepository {
    var definitionsResult: Result<List<ReleaseDefinitionSummary>> = Result.success(emptyList())
    var releasesResult: Result<List<ReleaseSummary>> = Result.success(emptyList())
    var detailResult: Result<dev.azure.desktop.domain.release.ReleaseDetail> =
        Result.failure(NotImplementedError())

    override suspend fun listReleaseDefinitions(
        organization: String,
        projectName: String,
    ): Result<List<ReleaseDefinitionSummary>> = definitionsResult

    override suspend fun listReleasesForDefinition(
        organization: String,
        projectName: String,
        definitionId: Int,
        top: Int,
    ): Result<List<ReleaseSummary>> = releasesResult

    override suspend fun getRelease(
        organization: String,
        projectName: String,
        releaseId: Int,
    ): Result<dev.azure.desktop.domain.release.ReleaseDetail> = detailResult

    override suspend fun getReleaseDefinition(
        organization: String,
        projectName: String,
        definitionId: Int,
    ): Result<dev.azure.desktop.domain.release.ReleaseDefinitionDetail> =
        Result.failure(NotImplementedError())

    override suspend fun createRelease(
        params: dev.azure.desktop.domain.release.CreateReleaseParams,
    ): Result<dev.azure.desktop.domain.release.CreatedRelease> = Result.failure(NotImplementedError())

    override suspend fun deployReleaseEnvironment(
        organization: String,
        projectName: String,
        releaseId: Int,
        environmentId: Int,
    ): Result<Unit> = Result.failure(NotImplementedError())
}
