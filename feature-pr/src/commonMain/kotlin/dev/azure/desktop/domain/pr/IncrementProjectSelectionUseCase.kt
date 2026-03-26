package dev.azure.desktop.domain.pr

class IncrementProjectSelectionUseCase(
    private val storage: ProjectSelectionCountStorage,
) {
    operator fun invoke(organization: String, projectName: String): Result<Unit> =
        storage.incrementSelection(organization, projectName)
}
