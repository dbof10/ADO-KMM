package dev.azure.desktop.data.pr

import dev.azure.desktop.domain.pr.ProjectSelectionRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.prefs.Preferences

private const val PreferencesNode = "dev.azure.desktop"
private const val KeyPrefix = "project_selection_counts_v1:"
private const val DebugProperty = "ado.debug.projectSelection"

@Serializable
private data class ProjectSelectionCounts(
    val counts: Map<String, Int> = emptyMap(),
)

class PreferencesProjectSelectionRepository(
    private val preferences: Preferences = Preferences.userRoot().node(PreferencesNode),
    private val json: Json = Json { ignoreUnknownKeys = true },
) : ProjectSelectionRepository {

    override suspend fun getDefaultProjectName(
        organization: String,
        availableProjectNames: List<String>,
    ): Result<String?> =
        runCatching {
            val org = organization.trim()
            if (org.isBlank()) return@runCatching null

            val available = availableProjectNames.map { it.trim() }.filter { it.isNotBlank() }.toSet()
            if (available.isEmpty()) return@runCatching null

            val data = loadCounts(org)
            val best =
                data.counts
                .asSequence()
                .filter { (name, _) -> name in available }
                .maxByOrNull { (_, count) -> count }
                ?.key
            debugLog(
                "getDefaultProjectName org='$org' available=${available.size} stored=${data.counts.size} -> '$best'",
            )
            best
        }

    override suspend fun recordProjectSelected(
        organization: String,
        projectName: String,
    ): Result<Unit> =
        runCatching {
            val org = organization.trim()
            val project = projectName.trim()
            if (org.isBlank() || project.isBlank()) return@runCatching

            val current = loadCounts(org)
            val nextCounts = current.counts.toMutableMap()
            val next = (nextCounts[project] ?: 0) + 1
            nextCounts[project] = next
            saveCounts(org, ProjectSelectionCounts(counts = nextCounts))
            debugLog("recordProjectSelected org='$org' project='$project' -> count=$next")
        }

    private fun loadCounts(organization: String): ProjectSelectionCounts {
        val raw = preferences.get(keyFor(organization), null) ?: return ProjectSelectionCounts()
        return runCatching { json.decodeFromString<ProjectSelectionCounts>(raw) }
            .getOrElse { ProjectSelectionCounts() }
    }

    private fun saveCounts(organization: String, counts: ProjectSelectionCounts) {
        preferences.put(keyFor(organization), json.encodeToString(counts))
    }

    private fun keyFor(organization: String): String = KeyPrefix + organization.lowercase()

    private fun debugLog(message: String) {
        if (System.getProperty(DebugProperty) == "true") {
            println("[ProjectSelection] $message")
        }
    }
}

