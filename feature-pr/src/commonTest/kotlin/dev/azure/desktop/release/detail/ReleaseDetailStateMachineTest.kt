package dev.azure.desktop.release.detail

import dev.azure.desktop.domain.release.DeployReleaseEnvironmentUseCase
import dev.azure.desktop.domain.release.GetReleaseDetailUseCase
import dev.azure.desktop.domain.release.ReleaseDeploymentStatus
import dev.azure.desktop.domain.release.ReleaseDetail
import dev.azure.desktop.domain.release.ReleaseEnvironmentInfo
import dev.azure.desktop.domain.release.ReleaseRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ReleaseDetailStateMachineTest {
    @Test
    fun initialLoadShowsContent() {
        runBlocking {
            val detail = sampleReleaseDetail()
            val repo =
                StubReleaseDetailRepository().apply {
                    getReleaseResult = Result.success(detail)
                }
            val machine = createMachine(repo)

            val content = machine.state.first { it is ReleaseDetailState.Content }
            assertEquals(detail, assertIs<ReleaseDetailState.Content>(content).detail)
        }
    }

    @Test
    fun deployEnvironmentRefreshesDetail() {
        runBlocking {
            val initial =
                sampleReleaseDetail(
                    environments =
                        listOf(
                            ReleaseEnvironmentInfo(
                                id = 5,
                                name = "Dev",
                                rank = 1,
                                status = ReleaseDeploymentStatus.NotStarted,
                                statusLabel = "Not started",
                                detailLine = null,
                            ),
                        ),
                )
            val refreshed =
                sampleReleaseDetail(
                    environments =
                        listOf(
                            ReleaseEnvironmentInfo(
                                id = 5,
                                name = "Dev",
                                rank = 1,
                                status = ReleaseDeploymentStatus.InProgress,
                                statusLabel = "In progress",
                                detailLine = null,
                            ),
                        ),
                )
            val repo =
                StubReleaseDetailRepository().apply {
                    getReleaseResult = Result.success(initial)
                    deployResult = Result.success(Unit)
                }
            val machine = createMachine(repo)
            var dispatched = false
            val end =
                machine.state
                    .onEach { s ->
                        if (s is ReleaseDetailState.Content) {
                            val c = s as ReleaseDetailState.Content
                            if (!dispatched && !c.isDeploying &&
                                c.detail.environments.single().status == ReleaseDeploymentStatus.NotStarted
                            ) {
                                dispatched = true
                                repo.getReleaseResult = Result.success(refreshed)
                                launch { machine.dispatch(ReleaseDetailAction.DeployEnvironment(5)) }
                            }
                        }
                    }.first {
                        it is ReleaseDetailState.Content &&
                            !(it as ReleaseDetailState.Content).isDeploying &&
                            (it as ReleaseDetailState.Content).detail.environments.single().status ==
                            ReleaseDeploymentStatus.InProgress
                    }
            assertNull(assertIs<ReleaseDetailState.Content>(end).deployError)
        }
    }

    @Test
    fun deployFailureSurfacesMessage() {
        runBlocking {
            val detail = sampleReleaseDetail()
            val repo =
                StubReleaseDetailRepository().apply {
                    getReleaseResult = Result.success(detail)
                    deployResult = Result.failure(Exception("blocked"))
                }
            val machine = createMachine(repo)
            var dispatched = false
            val withErr =
                machine.state
                    .onEach { s ->
                        if (s is ReleaseDetailState.Content) {
                            val c = s as ReleaseDetailState.Content
                            if (!dispatched && !c.isDeploying && c.deployError == null) {
                                dispatched = true
                                launch { machine.dispatch(ReleaseDetailAction.DeployEnvironment(5)) }
                            }
                        }
                    }.first {
                        it is ReleaseDetailState.Content &&
                            (it as ReleaseDetailState.Content).deployError != null
                    }
            assertEquals("blocked", assertIs<ReleaseDetailState.Content>(withErr).deployError)
        }
    }

    private fun createMachine(repo: ReleaseRepository): ReleaseDetailStateMachine =
        ReleaseDetailStateMachine(
            organization = "org",
            projectName = "Proj",
            releaseId = 42,
            getReleaseDetailUseCase = GetReleaseDetailUseCase(repo),
            deployReleaseEnvironmentUseCase = DeployReleaseEnvironmentUseCase(repo),
        )

    private fun sampleReleaseDetail(
        environments: List<ReleaseEnvironmentInfo> =
            listOf(
                ReleaseEnvironmentInfo(
                    id = 5,
                    name = "Dev",
                    rank = 1,
                    status = ReleaseDeploymentStatus.NotStarted,
                    statusLabel = "Not started",
                    detailLine = null,
                ),
            ),
    ) = ReleaseDetail(
        id = 42,
        name = "Rel",
        definitionName = "Def",
        projectName = "Proj",
        createdOnIso = null,
        triggerDescription = null,
        requestedForDisplay = null,
        artifacts = emptyList(),
        environments = environments,
        variables = emptyList(),
        timeline = emptyList(),
    )
}

private class StubReleaseDetailRepository : ReleaseRepository {
    var getReleaseResult: Result<ReleaseDetail> =
        Result.failure(IllegalStateException("unset"))
    var deployResult: Result<Unit> = Result.success(Unit)

    override suspend fun listReleaseDefinitions(
        organization: String,
        projectName: String,
    ): Result<List<dev.azure.desktop.domain.release.ReleaseDefinitionSummary>> =
        Result.failure(NotImplementedError())

    override suspend fun listReleasesForDefinition(
        organization: String,
        projectName: String,
        definitionId: Int,
        top: Int,
    ): Result<List<dev.azure.desktop.domain.release.ReleaseSummary>> =
        Result.failure(NotImplementedError())

    override suspend fun getRelease(
        organization: String,
        projectName: String,
        releaseId: Int,
    ): Result<ReleaseDetail> = getReleaseResult

    override suspend fun getReleaseDefinition(
        organization: String,
        projectName: String,
        definitionId: Int,
    ): Result<dev.azure.desktop.domain.release.ReleaseDefinitionDetail> =
        Result.failure(NotImplementedError())

    override suspend fun createRelease(
        params: dev.azure.desktop.domain.release.CreateReleaseParams,
    ): Result<dev.azure.desktop.domain.release.CreatedRelease> =
        Result.failure(NotImplementedError())

    override suspend fun deployReleaseEnvironment(
        organization: String,
        projectName: String,
        releaseId: Int,
        environmentId: Int,
    ): Result<Unit> = deployResult
}
