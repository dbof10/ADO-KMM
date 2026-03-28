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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import dev.azure.desktop.ui.components.MascotLoadingIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.azure.desktop.platform.authBridge
import dev.azure.desktop.platform.pullRequestBridge
import dev.azure.desktop.platform.releaseBridge
import dev.azure.desktop.domain.pr.PullRequestSummary
import dev.azure.desktop.domain.release.ReleaseSummary
import dev.azure.desktop.login.LoginStateMachine
import dev.azure.desktop.release.list.ReleaseListState
import dev.azure.desktop.navigation.AppScreen
import dev.azure.desktop.pr.detail.PrDetailState
import dev.azure.desktop.pr.detail.PrDetailAction
import dev.azure.desktop.pr.detail.PrDetailStateMachine
import dev.azure.desktop.pr.list.PrListState
import dev.azure.desktop.pr.review.CodeReviewStateMachine
import dev.azure.desktop.pr.list.PrListStateMachine
import dev.azure.desktop.release.detail.ReleaseDetailStateMachine
import dev.azure.desktop.release.list.ReleaseListStateMachine
import dev.azure.desktop.theme.EditorialTheme
import dev.azure.desktop.ui.screens.CodeReviewScreen
import dev.azure.desktop.ui.screens.SettingsScreen
import dev.azure.desktop.ui.screens.LoginScreen
import dev.azure.desktop.ui.screens.PrListScreen
import dev.azure.desktop.ui.screens.PrOverviewScreen
import dev.azure.desktop.ui.screens.ReleaseDetailScreen
import dev.azure.desktop.ui.screens.ReleaseListScreen
import dev.azure.desktop.ui.shell.MainShell
import dev.azure.desktop.deeplink.parseAzureDevOpsPullRequestUrl
import dev.azure.desktop.platform.DeepLinkEffect
import kotlinx.coroutines.launch

@Composable
fun App() {
    EditorialTheme {
        val appScope = rememberCoroutineScope()
        val initialPat = remember { authBridge.patStorage.loadPat() }
        val initialOrganization = remember { authBridge.patStorage.loadOrganization().orEmpty() }
        val hasStoredSession = remember(initialPat, initialOrganization) {
            initialPat != null && initialOrganization.isNotBlank()
        }
        var organization by remember { mutableStateOf(initialOrganization) }
        val organizationForDeepLink = rememberUpdatedState(organization)
        val screen = remember {
            mutableStateOf(if (hasStoredSession) AppScreen.PrList else AppScreen.Login)
        }
        var selectedPullRequest by remember { mutableStateOf<PullRequestSummary?>(null) }
        var selectedRelease by remember { mutableStateOf<ReleaseSummary?>(null) }
        var loginMachineEpoch by remember { mutableStateOf(0) }
        val loginStateMachine = remember(loginMachineEpoch) {
            LoginStateMachine(authBridge.verifyAndStorePat)
        }
        val prListStateMachine = remember(organization, loginMachineEpoch) {
            PrListStateMachine(
                organization = organization,
                listProjectsUseCase = pullRequestBridge.listProjectsUseCase,
                getMyPullRequestsUseCase = pullRequestBridge.getMyPullRequestsUseCase,
                getActivePullRequestsUseCase = pullRequestBridge.getActivePullRequestsUseCase,
                findPullRequestSummaryByIdUseCase = pullRequestBridge.findPullRequestSummaryByIdUseCase,
                getPullRequestSummaryByIdUseCase = pullRequestBridge.getPullRequestSummaryByIdUseCase,
                getMostSelectedProjectUseCase = pullRequestBridge.getMostSelectedProjectUseCase,
                incrementProjectSelectionUseCase = pullRequestBridge.incrementProjectSelectionUseCase,
            )
        }
        val prDetailStateMachine = remember(selectedPullRequest, organization, loginMachineEpoch) {
            selectedPullRequest?.let {
                PrDetailStateMachine(
                    organization = organization,
                    summary = it,
                    getPullRequestDetailUseCase = pullRequestBridge.getPullRequestDetailUseCase,
                    setMyPullRequestVoteUseCase = pullRequestBridge.setMyPullRequestVoteUseCase,
                    abandonPullRequestUseCase = pullRequestBridge.abandonPullRequestUseCase,
                )
            }
        }
        val codeReviewStateMachine = remember(selectedPullRequest, organization, loginMachineEpoch) {
            selectedPullRequest?.let {
                CodeReviewStateMachine(
                    organization = organization,
                    summary = it,
                    getPullRequestDetailUseCase = pullRequestBridge.getPullRequestDetailUseCase,
                    getPullRequestFileDiffUseCase = pullRequestBridge.getPullRequestFileDiffUseCase,
                    getPullRequestChanges = { org, project, repo, prId, base, target ->
                        pullRequestBridge.getPullRequestChanges(
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
                    listProjectsUseCase = pullRequestBridge.listProjectsUseCase,
                    listReleaseDefinitionsUseCase = releaseBridge.listReleaseDefinitionsUseCase,
                    listReleasesForDefinitionUseCase = releaseBridge.listReleasesForDefinitionUseCase,
                    getMostSelectedProjectUseCase = pullRequestBridge.getMostSelectedProjectUseCase,
                    incrementProjectSelectionUseCase = pullRequestBridge.incrementProjectSelectionUseCase,
                )
            } else {
                null
            }
        }
        // FlowRedux allows only one collector and restarts from initial state when collection stops.
        // Keep collecting while on list *or* detail so navigating to PR detail does not reset list state.
        var prListUiState by remember { mutableStateOf<PrListState>(PrListState.LoadingProjects) }
        val collectPrListState =
            screen.value == AppScreen.PrList || screen.value == AppScreen.PrDetail
        LaunchedEffect(prListStateMachine, collectPrListState, loginMachineEpoch) {
            if (!collectPrListState) return@LaunchedEffect
            prListStateMachine.state.collect { prListUiState = it }
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
                    getReleaseDetailUseCase = releaseBridge.getReleaseDetailUseCase,
                    deployReleaseEnvironmentUseCase = releaseBridge.deployReleaseEnvironmentUseCase,
                )
            }
        }

        val authActions = remember { object { lateinit var onSessionExpiredFromHttp: () -> Unit } }
        authActions.onSessionExpiredFromHttp = {
            authBridge.patStorage.clearPatOnly()
            loginMachineEpoch++
            screen.value = AppScreen.Login
            selectedPullRequest = null
            selectedRelease = null
            organization = authBridge.patStorage.loadOrganization().orEmpty()
        }

        DisposableEffect(Unit) {
            authBridge.setOnSessionUnauthorized {
                authBridge.runOnMainThread { authActions.onSessionExpiredFromHttp() }
            }
            onDispose { authBridge.setOnSessionUnauthorized { } }
        }

        DeepLinkEffect { raw ->
            val parsed = parseAzureDevOpsPullRequestUrl(raw) ?: return@DeepLinkEffect
            val org = organizationForDeepLink.value.trim()
            if (org.isEmpty()) return@DeepLinkEffect
            if (!parsed.organization.equals(org, ignoreCase = true)) return@DeepLinkEffect
            appScope.launch {
                pullRequestBridge.getPullRequestSummaryByIdUseCase(
                    organization = org,
                    projectName = parsed.project,
                    pullRequestId = parsed.pullRequestId,
                ).fold(
                    onSuccess = { summary ->
                        selectedPullRequest = summary
                        selectedRelease = null
                        screen.value = AppScreen.PrDetail
                    },
                    onFailure = { },
                )
            }
        }

        val signOut: () -> Unit = {
            authBridge.patStorage.clearCredentials()
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
                        initialOrganization = authBridge.patStorage.loadOrganization().orEmpty(),
                        onLoggedIn = {
                            organization = authBridge.patStorage.loadOrganization().orEmpty()
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
                            organization = organization,
                            stateMachine = prListStateMachine,
                            listState = prListUiState,
                            listPullRequestRepositories = { org, project ->
                                pullRequestBridge.listPullRequestRepositoriesUseCase(org, project)
                            },
                            createPullRequest = { params ->
                                pullRequestBridge.createPullRequestUseCase(params)
                            },
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
                                        MascotLoadingIndicator()
                                    }

                                is PrDetailState.Error -> Text(current.message)

                                is PrDetailState.Content -> {
                                    val reviewMachine = codeReviewStateMachine
                                    if (reviewMachine == null) {
                                        Text("Unable to open pull request.")
                                    } else {
                                        PrOverviewScreen(
                                            organization = organization,
                                            detail = current.detail,
                                            codeReviewStateMachine = reviewMachine,
                                            isVoting = current.isVoting,
                                            voteErrorMessage = current.voteErrorMessage,
                                            isClosing = current.isClosing,
                                            closeErrorMessage = current.closeErrorMessage,
                                            onBack = {
                                                selectedPullRequest = null
                                                screen.value = AppScreen.PrList
                                            },
                                            onApprove = { scope.launch { detailMachine.dispatch(PrDetailAction.Approve) } },
                                            onReject = { scope.launch { detailMachine.dispatch(PrDetailAction.Reject) } },
                                            onClosePr = { scope.launch { detailMachine.dispatch(PrDetailAction.Close) } },
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
                                    releaseBridge.getReleaseDefinitionUseCase(organization, projectName, definitionId)
                                },
                                createRelease = { params ->
                                    releaseBridge.createReleaseUseCase(params)
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

            AppScreen.Settings ->
                MainShell(
                    screen = AppScreen.Settings,
                    onNavigate = { screen.value = it },
                    onSignOut = signOut,
                    content = {
                        SettingsScreen(
                            organization = organization,
                            patToken = authBridge.patStorage.loadPat().orEmpty(),
                            onSignOut = signOut,
                            onClearCache = { pullRequestBridge.clearProjectSelectionCountsUseCase() },
                            modifier = Modifier.fillMaxSize(),
                        )
                    },
                )
        }
    }
}
