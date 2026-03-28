package dev.azure.desktop.pr.list

import dev.azure.desktop.domain.pr.DevOpsProject
import dev.azure.desktop.domain.pr.FindPullRequestSummaryByIdUseCase
import dev.azure.desktop.domain.pr.GetActivePullRequestsUseCase
import dev.azure.desktop.domain.pr.GetMostSelectedProjectUseCase
import dev.azure.desktop.domain.pr.GetMyPullRequestsUseCase
import dev.azure.desktop.domain.pr.GetPullRequestSummaryByIdUseCase
import dev.azure.desktop.domain.pr.IncrementProjectSelectionUseCase
import dev.azure.desktop.domain.pr.ListProjectsUseCase
import dev.azure.desktop.pr.InMemoryProjectSelectionStorage
import dev.azure.desktop.pr.StubPullRequestRepository
import dev.azure.desktop.pr.samplePullRequestSummary
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PrListStateMachineTest {
    @Test
    fun loadsProjectsAndPullRequestsIntoReady() =
        runBlocking {
            val selection = InMemoryProjectSelectionStorage()
            IncrementProjectSelectionUseCase(selection)("org", "Alpha")
            val repo =
                StubPullRequestRepository().apply {
                    projectsResult =
                        Result.success(
                            listOf(
                                DevOpsProject("p1", "Beta"),
                                DevOpsProject("p2", "Alpha"),
                            ),
                        )
                    myPrsResult =
                        Result.success(
                            listOf(
                                samplePullRequestSummary(id = 7, projectName = "Alpha"),
                            ),
                        )
                }
            val machine = createMachine(repo, selection)

            val ready = machine.state.first { it is PrListState.Ready }
            val r = assertIs<PrListState.Ready>(ready)
            assertEquals(PrListTab.Mine, r.tab)
            assertEquals(listOf("Alpha", "Beta"), r.projects.map { it.name })
            assertEquals("Alpha", r.selectedProjectName)
            assertEquals(7, r.items.single().id)
        }

    @Test
    fun projectsFailureThenRetryRecovers() =
        runBlocking {
            val repo = StubPullRequestRepository()
            repo.projectsResult = Result.failure(Exception("offline"))
            val machine = createMachine(repo, InMemoryProjectSelectionStorage())

            val err = machine.state.first { it is PrListState.ProjectsError }
            assertEquals("offline", assertIs<PrListState.ProjectsError>(err).message)

            repo.projectsResult =
                Result.success(listOf(DevOpsProject("1", "Solo")))
            repo.myPrsResult = Result.success(emptyList())
            launch { machine.dispatch(PrListAction.RetryProjects) }

            val ready = machine.state.first { it is PrListState.Ready }
            assertIs<PrListState.Ready>(ready)
        }

    @Test
    fun openPullRequestByIdSetsPendingWhenIdInCurrentList() =
        runBlocking {
            val pr = samplePullRequestSummary(id = 99)
            val repo =
                StubPullRequestRepository().apply {
                    projectsResult = Result.success(listOf(DevOpsProject("1", "P")))
                    myPrsResult = Result.success(listOf(pr))
                }
            val machine = createMachine(repo, InMemoryProjectSelectionStorage())
            machine.state.first { it is PrListState.Ready }

            launch { machine.dispatch(PrListAction.OpenPullRequestById(99)) }

            val withPending =
                machine.state
                    .filter { it is PrListState.Ready && (it as PrListState.Ready).pendingOpenPullRequest != null }
                    .first()
            val snap = assertIs<PrListState.Ready>(withPending)
            assertEquals(99, snap.pendingOpenPullRequest?.id)
            assertNull(snap.openPullRequestError)
        }

    @Test
    fun selectTabSwitchesToActivePullRequests() =
        runBlocking {
            val repo =
                StubPullRequestRepository().apply {
                    projectsResult = Result.success(listOf(DevOpsProject("1", "P")))
                    myPrsResult = Result.success(emptyList())
                    activePrsResult =
                        Result.success(listOf(samplePullRequestSummary(id = 2, projectName = "P")))
                }
            val machine = createMachine(repo, InMemoryProjectSelectionStorage())
            machine.state.first { it is PrListState.Ready }

            launch { machine.dispatch(PrListAction.SelectTab(PrListTab.Active)) }

            val ready =
                machine.state
                    .filter { it is PrListState.Ready && (it as PrListState.Ready).tab == PrListTab.Active }
                    .first()
            assertEquals(2, assertIs<PrListState.Ready>(ready).items.single().id)
        }

    @Test
    fun openPullRequestByIdUsesGetByIdWhenNotInList() =
        runBlocking {
            val fetched = samplePullRequestSummary(id = 5, projectName = "P")
            val repo =
                StubPullRequestRepository().apply {
                    projectsResult = Result.success(listOf(DevOpsProject("1", "P")))
                    myPrsResult = Result.success(emptyList())
                    summaryByIdResult = Result.success(fetched)
                }
            val machine = createMachine(repo, InMemoryProjectSelectionStorage())
            machine.state.first { it is PrListState.Ready }

            launch { machine.dispatch(PrListAction.OpenPullRequestById(5)) }

            val snap =
                assertIs<PrListState.Ready>(
                    machine.state
                        .filter { it is PrListState.Ready && (it as PrListState.Ready).pendingOpenPullRequest?.id == 5 }
                        .first(),
                )
            assertNotNull(snap.pendingOpenPullRequest)
        }

    private fun createMachine(
        repo: StubPullRequestRepository,
        projectStorage: InMemoryProjectSelectionStorage,
    ): PrListStateMachine {
        val listProjects = ListProjectsUseCase(repo)
        val getMy = GetMyPullRequestsUseCase(repo)
        val getActive = GetActivePullRequestsUseCase(repo)
        return PrListStateMachine(
            organization = "org",
            listProjectsUseCase = listProjects,
            getMyPullRequestsUseCase = getMy,
            getActivePullRequestsUseCase = getActive,
            findPullRequestSummaryByIdUseCase =
                FindPullRequestSummaryByIdUseCase(
                    listProjects,
                    getMy,
                    getActive,
                ),
            getPullRequestSummaryByIdUseCase = GetPullRequestSummaryByIdUseCase(repo),
            getMostSelectedProjectUseCase = GetMostSelectedProjectUseCase(projectStorage),
            incrementProjectSelectionUseCase = IncrementProjectSelectionUseCase(projectStorage),
        )
    }
}
