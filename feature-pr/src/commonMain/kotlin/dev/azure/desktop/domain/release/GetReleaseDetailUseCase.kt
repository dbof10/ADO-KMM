package dev.azure.desktop.domain.release

class GetReleaseDetailUseCase(
    private val repository: ReleaseRepository,
) {
    suspend operator fun invoke(
        organization: String,
        projectName: String,
        releaseId: Int,
    ): Result<ReleaseDetail> =
        repository.getRelease(organization, projectName, releaseId)
}
