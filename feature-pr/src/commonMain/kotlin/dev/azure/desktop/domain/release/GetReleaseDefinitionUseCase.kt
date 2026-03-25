package dev.azure.desktop.domain.release

class GetReleaseDefinitionUseCase(
    private val repository: ReleaseRepository,
) {
    suspend operator fun invoke(
        organization: String,
        projectName: String,
        definitionId: Int,
    ): Result<ReleaseDefinitionDetail> =
        repository.getReleaseDefinition(organization, projectName, definitionId)
}
