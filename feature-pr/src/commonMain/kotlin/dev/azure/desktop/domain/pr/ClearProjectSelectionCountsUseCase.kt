package dev.azure.desktop.domain.pr

class ClearProjectSelectionCountsUseCase(
    private val storage: ProjectSelectionCountStorage,
) {
    operator fun invoke(): Result<Unit> =
        storage.clearAllSelections()
}
