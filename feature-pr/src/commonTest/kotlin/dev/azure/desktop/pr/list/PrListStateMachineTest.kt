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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PrListStateMachineTest {
    @Test
    fun loadsProjectsAndPullRequestsIntoReady() {
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
    }

    @Test
    fun projectsFailureThenRetryRecovers() {
        runBlocking {
            val repo = StubPullRequestRepository()
            repo.projectsResult = Result.failure(Exception("offline"))
            val machine = createMachine(repo, InMemoryProjectSelectionStorage())

            var dispatched = false
            val ready =
                machine.state
                    .onEach { s ->
                        if (s is PrListState.ProjectsError) {
                            assertEquals("offline", s.message)
                            if (!dispatched) {
                                dispatched = true
                                repo.projectsResult =
                                    Result.success(listOf(DevOpsProject("1", "Solo")))
                                repo.myPrsResult = Result.success(emptyList())
                                launch { machine.dispatch(PrListAction.RetryProjects) }
                            }
                        }
                    }.first { it is PrListState.Ready }
            assertIs<PrListState.Ready>(ready)
        }
    }

    @Test
    fun openPullRequestByIdSetsPendingWhenIdInCurrentList() {
        runBlocking {
            val pr = samplePullRequestSummary(id = 99)
            val repo =
                StubPullRequestRepository().apply {
                    projectsResult = Result.success(listOf(DevOpsProject("1", "P")))
                    myPrsResult = Result.success(listOf(pr))
                }
            val machine = createMachine(repo, InMemoryProjectSelectionStorage())
            var dispatched = false
            val withPending =
                machine.state
                    .onEach { s ->
                        if (s is PrListState.Ready) {
                            val r = s as PrListState.Ready
                            if (!dispatched && r.pendingOpenPullRequest == null) {
                                dispatched = true
                                launch { machine.dispatch(PrListAction.OpenPullRequestById(99)) }
                            }
                        }
                    }.first {
                        it is PrListState.Ready &&
                            (it as PrListState.Ready).pendingOpenPullRequest != null
                    }
            val snap = assertIs<PrListState.Ready>(withPending)
            assertEquals(99, snap.pendingOpenPullRequest?.id)
            assertNull(snap.openPullRequestError)
        }
    }

    @Test
    fun selectTabSwitchesToActivePullRequests() {
        runBlocking {
            val repo =
                StubPullRequestRepository().apply {
                    projectsResult = Result.success(listOf(DevOpsProject("1", "P")))
                    myPrsResult = Result.success(emptyList())
                    activePrsResult =
                        Result.success(listOf(samplePullRequestSummary(id = 2, projectName = "P")))
                }
            val machine = createMachine(repo, InMemoryProjectSelectionStorage())
            var dispatched = false
            val ready =
                machine.state
                    .onEach { s ->
                        if (s is PrListState.Ready) {
                            val r = s as PrListState.Ready
                            if (!dispatched && r.tab == PrListTab.Mine) {
                                dispatched = true
                                launch { machine.dispatch(PrListAction.SelectTab(PrListTab.Active)) }
                            }
                        }
                    }.first {
                        it is PrListState.Ready && (it as PrListState.Ready).tab == PrListTab.Active
                    }
            assertEquals(2, assertIs<PrListState.Ready>(ready).items.single().id)
        }
    }

    @Test
    fun openPullRequestByIdUsesGetByIdWhenNotInList() {
        runBlocking {
            val fetched = samplePullRequestSummary(id = 5, projectName = "P")
            val repo =
                StubPullRequestRepository().apply {
                    projectsResult = Result.success(listOf(DevOpsProject("1", "P")))
                    myPrsResult = Result.success(emptyList())
                    summaryByIdResult = Result.success(fetched)
                }
            val storage = InMemoryProjectSelectionStorage()
            IncrementProjectSelectionUseCase(storage)("org", "P")
            val machine = createMachine(repo, storage)
            var dispatched = false
            val snap =
                assertIs<PrListState.Ready>(
                    machine.state
                        .onEach { s ->
                            if (s is PrListState.Ready) {
                                val r = s as PrListState.Ready
                                if (!dispatched) {
                                    dispatched = true
                                    launch { machine.dispatch(PrListAction.OpenPullRequestById(5)) }
                                }
                            }
                        }.first {
                            it is PrListState.Ready &&
                                (it as PrListState.Ready).pendingOpenPullRequest?.id == 5
                        },
                )
            assertNotNull(snap.pendingOpenPullRequest)
        }
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
