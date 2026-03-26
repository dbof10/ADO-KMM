package dev.azure.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.azure.desktop.data.auth.JvmAuthServices
import dev.azure.desktop.data.pr.JvmPullRequestServices
import dev.azure.desktop.data.release.JvmReleaseServices
import dev.azure.desktop.domain.pr.PullRequestSummary
import dev.azure.desktop.domain.release.ReleaseSummary
import dev.azure.desktop.login.LoginStateMachine
import dev.azure.desktop.release.list.ReleaseListState
import dev.azure.desktop.navigation.AppScreen
import dev.azure.desktop.pr.detail.PrDetailState
import dev.azure.desktop.pr.detail.PrDetailAction
import dev.azure.desktop.pr.detail.PrDetailStateMachine
import dev.azure.desktop.pr.review.CodeReviewStateMachine
import dev.azure.desktop.pr.list.PrListStateMachine
import dev.azure.desktop.release.detail.ReleaseDetailStateMachine
import dev.azure.desktop.release.list.ReleaseListStateMachine
import dev.azure.desktop.theme.EditorialTheme
import dev.azure.desktop.ui.screens.CodeReviewScreen
import dev.azure.desktop.ui.screens.DesignSystemScreen
import dev.azure.desktop.ui.screens.LoginScreen
import dev.azure.desktop.ui.screens.PrListScreen
import dev.azure.desktop.ui.screens.PrOverviewScreen
import dev.azure.desktop.ui.screens.ReleaseDetailScreen
import dev.azure.desktop.ui.screens.ReleaseListScreen
import dev.azure.desktop.ui.shell.MainShell
import javax.swing.SwingUtilities
import kotlinx.coroutines.launch

@Composable
fun App() {
    EditorialTheme {
        val initialPat = remember { JvmAuthServices.patStorage.loadPat() }
        val initialOrganization = remember { JvmAuthServices.patStorage.loadOrganization().orEmpty() }
        val hasStoredSession = remember(initialPat, initialOrganization) {
            initialPat != null && initialOrganization.isNotBlank()
        }
        var organization by remember { mutableStateOf(initialOrganization) }
        val screen = remember {
            mutableStateOf(if (hasStoredSession) AppScreen.PrList else AppScreen.Login)
        }
        var selectedPullRequest by remember { mutableStateOf<PullRequestSummary?>(null) }
        var selectedRelease by remember { mutableStateOf<ReleaseSummary?>(null) }
        var loginMachineEpoch by remember { mutableStateOf(0) }
        val loginStateMachine = remember(loginMachineEpoch) {
            LoginStateMachine(JvmAuthServices.verifyAndStorePat)
        }
        val prListStateMachine = remember(organization, loginMachineEpoch) {
            PrListStateMachine(
                organization = organization,
                listProjectsUseCase = JvmPullRequestServices.listProjectsUseCase,
                getMyPullRequestsUseCase = JvmPullRequestServices.getMyPullRequestsUseCase,
                getActivePullRequestsUseCase = JvmPullRequestServices.getActivePullRequestsUseCase,
                findPullRequestSummaryByIdUseCase = JvmPullRequestServices.findPullRequestSummaryByIdUseCase,
                getPullRequestSummaryByIdUseCase = JvmPullRequestServices.getPullRequestSummaryByIdUseCase,
            )
        }
        val prDetailStateMachine = remember(selectedPullRequest, organization, loginMachineEpoch) {
            selectedPullRequest?.let {
                PrDetailStateMachine(
                    organization = organization,
                    summary = it,
                    getPullRequestDetailUseCase = JvmPullRequestServices.getPullRequestDetailUseCase,
                    setMyPullRequestVoteUseCase = JvmPullRequestServices.setMyPullRequestVoteUseCase,
                )
            }
        }
        val codeReviewStateMachine = remember(selectedPullRequest, organization, loginMachineEpoch) {
            selectedPullRequest?.let {
                CodeReviewStateMachine(
                    organization = organization,
                    summary = it,
                    getPullRequestDetailUseCase = JvmPullRequestServices.getPullRequestDetailUseCase,
                    getPullRequestFileDiffUseCase = JvmPullRequestServices.getPullRequestFileDiffUseCase,
                    getPullRequestChanges = { org, project, repo, prId, base, target ->
                        JvmPullRequestServices.getPullRequestChanges(
                            organization = org,
                            projectName = project,
                            repositoryId = repo,
                            pullRequestId = prId,
                            baseCommitId = base,
                            targetCommitId = target,
                        )
                    },
                )
            }
        }
        val releaseListStateMachine = remember(organization, loginMachineEpoch) {
            if (organization.isNotBlank()) {
                ReleaseListStateMachine(
                    organization = organization,
                    listProjectsUseCase = JvmPullRequestServices.listProjectsUseCase,
                    listReleaseDefinitionsUseCase = JvmReleaseServices.listReleaseDefinitionsUseCase,
                    listReleasesForDefinitionUseCase = JvmReleaseServices.listReleasesForDefinitionUseCase,
                )
            } else {
                null
            }
        }

        // FlowRedux allows only one collector and restarts from initial state when collection stops.
        // Keep collecting while on list *or* detail so navigating to release detail does not reset project/definition.
        var releaseListUiState by remember { mutableStateOf<ReleaseListState>(ReleaseListState.LoadingProjects) }
        val collectReleaseListState =
            screen.value == AppScreen.ReleaseList || screen.value == AppScreen.ReleaseDetail
        LaunchedEffect(releaseListStateMachine, collectReleaseListState, loginMachineEpoch) {
            val machine = releaseListStateMachine
            if (machine == null) {
                releaseListUiState = ReleaseListState.LoadingProjects
                return@LaunchedEffect
            }
            if (!collectReleaseListState) return@LaunchedEffect
            machine.state.collect { releaseListUiState = it }
        }
        val releaseDetailStateMachine = remember(selectedRelease, organization, loginMachineEpoch) {
            selectedRelease?.let { rel ->
                ReleaseDetailStateMachine(
                    organization = organization,
                    projectName = rel.projectName,
                    releaseId = rel.id,
                    getReleaseDetailUseCase = JvmReleaseServices.getReleaseDetailUseCase,
                    deployReleaseEnvironmentUseCase = JvmReleaseServices.deployReleaseEnvironmentUseCase,
                )
            }
        }

        val authActions = remember { object { lateinit var onSessionExpiredFromHttp: () -> Unit } }
        authActions.onSessionExpiredFromHttp = {
            JvmAuthServices.patStorage.clearPatOnly()
            loginMachineEpoch++
            screen.value = AppScreen.Login
            selectedPullRequest = null
            selectedRelease = null
            organization = JvmAuthServices.patStorage.loadOrganization().orEmpty()
        }

        DisposableEffect(Unit) {
            JvmAuthServices.setOnSessionUnauthorized {
                SwingUtilities.invokeLater { authActions.onSessionExpiredFromHttp() }
            }
            onDispose { JvmAuthServices.setOnSessionUnauthorized { } }
        }

        val signOut: () -> Unit = {
            JvmAuthServices.patStorage.clearCredentials()
            loginMachineEpoch++
            screen.value = AppScreen.Login
            selectedPullRequest = null
            selectedRelease = null
            organization = ""
        }

        when (screen.value) {
            AppScreen.Login ->
                key(loginMachineEpoch) {
                    LoginScreen(
                        stateMachine = loginStateMachine,
                        initialOrganization = JvmAuthServices.patStorage.loadOrganization().orEmpty(),
                        onLoggedIn = {
                            organization = JvmAuthServices.patStorage.loadOrganization().orEmpty()
                            screen.value = if (organization.isNotBlank()) AppScreen.PrList else AppScreen.Login
                        },
                    )
                }

            AppScreen.PrList ->
                MainShell(
                    screen = AppScreen.PrList,
                    onNavigate = { screen.value = it },
                    onSignOut = signOut,
                    content = {
                        PrListScreen(
                            stateMachine = prListStateMachine,
                            onOpenPullRequest = {
                                selectedPullRequest = it
                                screen.value = AppScreen.PrDetail
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    },
                )

            AppScreen.PrDetail ->
                MainShell(
                    screen = AppScreen.PrDetail,
                    onNavigate = { screen.value = it },
                    onSignOut = signOut,
                    content = {
                        val detailMachine = prDetailStateMachine
                        if (detailMachine == null) {
                            screen.value = AppScreen.PrList
                        } else {
                            var detailState by remember(detailMachine) {
                                mutableStateOf<PrDetailState>(
                                    PrDetailState.Loading,
                                )
                            }
                            LaunchedEffect(detailMachine) {
                                detailMachine.state.collect { detailState = it }
                            }
                            val scope = rememberCoroutineScope()
                            when (val current = detailState) {
                                PrDetailState.Loading ->
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator()
                                    }

                                is PrDetailState.Error -> Text(current.message)

                                is PrDetailState.Content -> {
                                    val reviewMachine = codeReviewStateMachine
                                    if (reviewMachine == null) {
                                        Text("Unable to open pull request.")
                                    } else {
                                        PrOverviewScreen(
                                            detail = current.detail,
                                            codeReviewStateMachine = reviewMachine,
                                            isVoting = current.isVoting,
                                            voteErrorMessage = current.voteErrorMessage,
                                            onApprove = { scope.launch { detailMachine.dispatch(PrDetailAction.Approve) } },
                                            onReject = { scope.launch { detailMachine.dispatch(PrDetailAction.Reject) } },
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }
                                }
                            }
                        }
                    },
                )

            AppScreen.CodeReview ->
                MainShell(
                    screen = AppScreen.CodeReview,
                    onNavigate = { screen.value = it },
                    onSignOut = signOut,
                    content = {
                        val machine = codeReviewStateMachine
                        if (machine == null) {
                            Text("Select a pull request first.")
                        } else {
                            CodeReviewScreen(
                                stateMachine = machine,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    },
                )

            AppScreen.ReleaseList ->
                MainShell(
                    screen = AppScreen.ReleaseList,
                    onNavigate = { screen.value = it },
                    onSignOut = signOut,
                    content = {
                        val machine = releaseListStateMachine
                        if (machine == null) {
                            Text("Sign in to view releases.")
                        } else {
                            ReleaseListScreen(
                                organization = organization,
                                stateMachine = machine,
                                listState = releaseListUiState,
                                onOpenRelease = {
                                    selectedRelease = it
                                    screen.value = AppScreen.ReleaseDetail
                                },
                                getReleaseDefinition = { projectName, definitionId ->
                                    JvmReleaseServices.getReleaseDefinitionUseCase(organization, projectName, definitionId)
                                },
                                createRelease = { params ->
                                    JvmReleaseServices.createReleaseUseCase(params)
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    },
                )

            AppScreen.ReleaseDetail ->
                MainShell(
                    screen = AppScreen.ReleaseDetail,
                    onNavigate = { screen.value = it },
                    onSignOut = signOut,
                    content = {
                        val machine = releaseDetailStateMachine
                        if (machine == null) {
                            screen.value = AppScreen.ReleaseList
                        } else {
                            ReleaseDetailScreen(
                                stateMachine = machine,
                                onBack = {
                                    selectedRelease = null
                                    screen.value = AppScreen.ReleaseList
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    },
                )

            AppScreen.DesignSystem ->
                MainShell(
                    screen = AppScreen.DesignSystem,
                    onNavigate = { screen.value = it },
                    onSignOut = signOut,
                    content = {
                        DesignSystemScreen(
                            onBack = { screen.value = AppScreen.PrList },
                            modifier = Modifier.fillMaxSize(),
                        )
                    },
                )
        }
    }
}
