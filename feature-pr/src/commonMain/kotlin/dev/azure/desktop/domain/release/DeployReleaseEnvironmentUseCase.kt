package dev.azure.desktop.domain.release

class DeployReleaseEnvironmentUseCase(
    private val repository: ReleaseRepository,
) {
    suspend operator fun invoke(
        organization: String,
        projectName: String,
        releaseId: Int,
        environmentId: Int,
    ): Result<Unit> =
        repository.deployReleaseEnvironment(
            organization = organization,
            projectName = projectName,
            releaseId = releaseId,
            environmentId = environmentId,
        )
}
