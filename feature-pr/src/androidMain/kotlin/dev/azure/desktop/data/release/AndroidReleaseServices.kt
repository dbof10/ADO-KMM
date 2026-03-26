package dev.azure.desktop.data.release

import dev.azure.desktop.data.auth.AndroidAuthServices
import dev.azure.desktop.domain.release.CreateReleaseUseCase
import dev.azure.desktop.domain.release.DeployReleaseEnvironmentUseCase
import dev.azure.desktop.domain.release.GetReleaseDefinitionUseCase
import dev.azure.desktop.domain.release.GetReleaseDetailUseCase
import dev.azure.desktop.domain.release.ListReleaseDefinitionsUseCase
import dev.azure.desktop.domain.release.ListReleasesForDefinitionUseCase

object AndroidReleaseServices {
    private val repository by lazy {
        AdoReleaseRepository(
            httpClient = AndroidAuthServices.sessionHttpClient,
            patProvider = { AndroidAuthServices.patStorage.loadPat() },
        )
    }

    val listReleaseDefinitionsUseCase by lazy { ListReleaseDefinitionsUseCase(repository) }

    val listReleasesForDefinitionUseCase by lazy { ListReleasesForDefinitionUseCase(repository) }

    val getReleaseDetailUseCase by lazy { GetReleaseDetailUseCase(repository) }

    val getReleaseDefinitionUseCase by lazy { GetReleaseDefinitionUseCase(repository) }

    val createReleaseUseCase by lazy { CreateReleaseUseCase(repository) }

    val deployReleaseEnvironmentUseCase by lazy { DeployReleaseEnvironmentUseCase(repository) }
}
