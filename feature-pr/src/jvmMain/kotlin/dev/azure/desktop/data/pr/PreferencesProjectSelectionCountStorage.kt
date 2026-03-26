package dev.azure.desktop.data.pr

import dev.azure.desktop.domain.pr.ProjectSelectionCountStorage
import java.util.prefs.Preferences

private const val NODE_NAME = "dev.azure.desktop.pr"
private const val KEY_PREFIX = "project_selection_count."

class PreferencesProjectSelectionCountStorage : ProjectSelectionCountStorage {
    private val preferences: Preferences = Preferences.userRoot().node(NODE_NAME)

    override fun getSelectionCounts(organization: String): Map<String, Int> {
        val normalizedOrg = normalizeOrganization(organization)
        val prefix = "$KEY_PREFIX$normalizedOrg."
        return buildMap {
            preferences.keys().forEach { key ->
                if (!key.startsWith(prefix)) return@forEach
                val project = key.removePrefix(prefix)
                if (project.isBlank()) return@forEach
                val count = preferences.getInt(key, 0)
                if (count > 0) {
                    put(project, count)
                }
            }
        }
    }

    override fun incrementSelection(organization: String, projectName: String): Result<Unit> =
        runCatching {
            val normalizedOrg = normalizeOrganization(organization)
            val normalizedProject = projectName.trim()
            if (normalizedProject.isEmpty()) return@runCatching
            val key = "$KEY_PREFIX$normalizedOrg.$normalizedProject"
            val next = preferences.getInt(key, 0) + 1
            preferences.putInt(key, next)
            preferences.flush()
        }

    override fun clearAllSelections(): Result<Unit> =
        runCatching {
            preferences.keys().forEach { key ->
                if (key.startsWith(KEY_PREFIX)) {
                    preferences.remove(key)
                }
            }
            preferences.flush()
        }

    private fun normalizeOrganization(organization: String): String =
        organization.trim().lowercase()
}
