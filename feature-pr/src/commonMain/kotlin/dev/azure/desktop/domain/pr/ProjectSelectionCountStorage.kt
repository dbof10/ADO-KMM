package dev.azure.desktop.domain.pr

interface ProjectSelectionCountStorage {
    fun getSelectionCounts(organization: String): Map<String, Int>

    fun incrementSelection(organization: String, projectName: String): Result<Unit>

    fun clearAllSelections(): Result<Unit>
}
