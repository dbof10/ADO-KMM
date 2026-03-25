package dev.azure.desktop.domain.pr

interface ProjectSelectionRepository {
    /**
     * Returns the most-selected project name for [organization], restricted to [availableProjectNames].
     * Returns null when no historical selection exists or nothing matches availability.
     */
    suspend fun getDefaultProjectName(
        organization: String,
        availableProjectNames: List<String>,
    ): Result<String?>

    /**
     * Records a selection (increments its usage count).
     */
    suspend fun recordProjectSelected(
        organization: String,
        projectName: String,
    ): Result<Unit>
}

