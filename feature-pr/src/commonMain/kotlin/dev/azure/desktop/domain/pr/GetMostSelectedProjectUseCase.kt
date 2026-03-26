package dev.azure.desktop.domain.pr

class GetMostSelectedProjectUseCase(
    private val storage: ProjectSelectionCountStorage,
) {
    operator fun invoke(organization: String, availableProjectNames: List<String>): String? {
        if (availableProjectNames.isEmpty()) return null
        val counts = storage.getSelectionCounts(organization)
        var bestProject: String? = null
        var bestCount = 0
        availableProjectNames.forEach { name ->
            val count = counts[name] ?: 0
            if (count > bestCount) {
                bestProject = name
                bestCount = count
            }
        }
        return bestProject
    }
}
