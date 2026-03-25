package dev.azure.desktop.domain.release

class ListReleaseDefinitionsUseCase(
    private val repository: ReleaseRepository,
) {
    suspend operator fun invoke(
        organization: String,
        projectName: String,
    ): Result<List<ReleaseDefinitionSummary>> =
        repository.listReleaseDefinitions(organization, projectName)
}
