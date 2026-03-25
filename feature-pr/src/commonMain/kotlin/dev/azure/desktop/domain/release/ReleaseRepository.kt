package dev.azure.desktop.domain.release

interface ReleaseRepository {
    suspend fun listReleaseDefinitions(
        organization: String,
        projectName: String,
    ): Result<List<ReleaseDefinitionSummary>>

    suspend fun listReleasesForDefinition(
        organization: String,
        projectName: String,
        definitionId: Int,
        top: Int = 50,
    ): Result<List<ReleaseSummary>>

    suspend fun getRelease(
        organization: String,
        projectName: String,
        releaseId: Int,
    ): Result<ReleaseDetail>
}
