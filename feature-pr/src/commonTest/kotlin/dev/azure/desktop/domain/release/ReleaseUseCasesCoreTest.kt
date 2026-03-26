package dev.azure.desktop.domain.release

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReleaseUseCasesCoreTest {
    @Test
    fun delegatesToRepository() = runBlocking {
        val repository = RecordingReleaseRepository()

        assertTrue(ListReleaseDefinitionsUseCase(repository)("org", "project").isSuccess)
        assertEquals("org", repository.organization)

        assertTrue(ListReleasesForDefinitionUseCase(repository)("org", "project", 3).isSuccess)
        assertEquals(3, repository.definitionId)

        assertTrue(GetReleaseDetailUseCase(repository)("org", "project", 5).isFailure)
        assertEquals(5, repository.releaseId)

        assertTrue(GetReleaseDefinitionUseCase(repository)("org", "project", 6).isFailure)
        assertEquals(6, repository.definitionId)

        val params =
            CreateReleaseParams(
                organization = "org",
                projectName = "project",
                definitionId = 9,
                description = "desc",
                manualEnvironmentNames = listOf("prod"),
            )
        assertTrue(CreateReleaseUseCase(repository)(params).isSuccess)
        assertEquals(params, repository.createParams)

        assertTrue(DeployReleaseEnvironmentUseCase(repository)("org", "project", 7, 8).isSuccess)
        assertEquals(7, repository.releaseId)
        assertEquals(8, repository.environmentId)
    }
}

private class RecordingReleaseRepository : ReleaseRepository {
    var organization: String? = null
    var projectName: String? = null
    var definitionId: Int? = null
    var releaseId: Int? = null
    var environmentId: Int? = null
    var createParams: CreateReleaseParams? = null

    override suspend fun listReleaseDefinitions(organization: String, projectName: String): Result<List<ReleaseDefinitionSummary>> {
        this.organization = organization
        this.projectName = projectName
        return Result.success(emptyList())
    }

    override suspend fun listReleasesForDefinition(
        organization: String,
        projectName: String,
        definitionId: Int,
        top: Int,
    ): Result<List<ReleaseSummary>> {
        this.organization = organization
        this.projectName = projectName
        this.definitionId = definitionId
        return Result.success(emptyList())
    }

    override suspend fun getRelease(organization: String, projectName: String, releaseId: Int): Result<ReleaseDetail> {
        this.organization = organization
        this.projectName = projectName
        this.releaseId = releaseId
        return Result.failure(NotImplementedError("unused"))
    }

    override suspend fun getReleaseDefinition(
        organization: String,
        projectName: String,
        definitionId: Int,
    ): Result<ReleaseDefinitionDetail> {
        this.organization = organization
        this.projectName = projectName
        this.definitionId = definitionId
        return Result.failure(NotImplementedError("unused"))
    }

    override suspend fun createRelease(params: CreateReleaseParams): Result<CreatedRelease> {
        createParams = params
        return Result.success(CreatedRelease(1, "release"))
    }

    override suspend fun deployReleaseEnvironment(
        organization: String,
        projectName: String,
        releaseId: Int,
        environmentId: Int,
    ): Result<Unit> {
        this.organization = organization
        this.projectName = projectName
        this.releaseId = releaseId
        this.environmentId = environmentId
        return Result.success(Unit)
    }
}
