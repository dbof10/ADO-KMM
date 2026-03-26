package dev.azure.desktop.data.pr

import android.content.Context
import dev.azure.desktop.domain.pr.ProjectSelectionCountStorage

private const val PREFS_NAME = "ado_desktop_pr"
private const val KEY_PREFIX = "project_selection_count."

class SharedPreferencesProjectSelectionCountStorage(
    context: Context,
) : ProjectSelectionCountStorage {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getSelectionCounts(organization: String): Map<String, Int> {
        val normalizedOrg = normalizeOrganization(organization)
        val prefix = "$KEY_PREFIX$normalizedOrg."
        val allEntries = prefs.all
        return buildMap {
            allEntries.forEach { (key, value) ->
                if (!key.startsWith(prefix)) return@forEach
                val project = key.removePrefix(prefix)
                val count = (value as? Int) ?: return@forEach
                if (project.isNotBlank() && count > 0) {
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
            val next = prefs.getInt(key, 0) + 1
            prefs.edit().putInt(key, next).apply()
        }

    override fun clearAllSelections(): Result<Unit> =
        runCatching {
            val keysToRemove = prefs.all.keys.filter { it.startsWith(KEY_PREFIX) }
            if (keysToRemove.isEmpty()) return@runCatching
            prefs.edit().apply {
                keysToRemove.forEach(::remove)
            }.apply()
        }

    private fun normalizeOrganization(organization: String): String =
        organization.trim().lowercase()
}
