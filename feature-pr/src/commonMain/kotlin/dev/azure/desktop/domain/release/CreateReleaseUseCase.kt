package dev.azure.desktop.domain.release

class CreateReleaseUseCase(
    private val repository: ReleaseRepository,
) {
    suspend operator fun invoke(params: CreateReleaseParams): Result<CreatedRelease> =
        repository.createRelease(params)
}
