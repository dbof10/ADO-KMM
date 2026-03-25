package dev.azure.desktop.data.pr

import dev.azure.desktop.data.auth.JvmAuthServices
import dev.azure.desktop.domain.pr.GetMyPullRequestsUseCase
import dev.azure.desktop.domain.pr.GetPullRequestDetailUseCase

object JvmPullRequestServices {
    private val repository by lazy {
        AdoPullRequestRepository(
            httpClient = JvmAuthServices.sessionHttpClient,
            patProvider = { JvmAuthServices.patStorage.loadPat() },
        )
    }

    val getMyPullRequestsUseCase by lazy { GetMyPullRequestsUseCase(repository) }

    val getPullRequestDetailUseCase by lazy { GetPullRequestDetailUseCase(repository) }
}
