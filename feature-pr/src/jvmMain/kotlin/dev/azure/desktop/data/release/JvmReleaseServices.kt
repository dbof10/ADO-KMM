package dev.azure.desktop.data.release

import dev.azure.desktop.data.auth.JvmAuthServices
import dev.azure.desktop.domain.release.GetReleaseDetailUseCase
import dev.azure.desktop.domain.release.ListReleaseDefinitionsUseCase
import dev.azure.desktop.domain.release.ListReleasesForDefinitionUseCase

object JvmReleaseServices {
    private val repository by lazy {
        AdoReleaseRepository(
            httpClient = JvmAuthServices.sessionHttpClient,
            patProvider = { JvmAuthServices.patStorage.loadPat() },
        )
    }

    val listReleaseDefinitionsUseCase by lazy { ListReleaseDefinitionsUseCase(repository) }

    val listReleasesForDefinitionUseCase by lazy { ListReleasesForDefinitionUseCase(repository) }

    val getReleaseDetailUseCase by lazy { GetReleaseDetailUseCase(repository) }
}
