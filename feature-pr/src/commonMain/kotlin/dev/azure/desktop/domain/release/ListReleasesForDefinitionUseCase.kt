package dev.azure.desktop.domain.release

class ListReleasesForDefinitionUseCase(
    private val repository: ReleaseRepository,
) {
    suspend operator fun invoke(
        organization: String,
        projectName: String,
        definitionId: Int,
    ): Result<List<ReleaseSummary>> =
        repository.listReleasesForDefinition(organization, projectName, definitionId)
}
