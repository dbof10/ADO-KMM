package dev.azure.desktop.data.pr

import dev.azure.desktop.domain.pr.ProjectSelectionCountStorage
import platform.Foundation.NSUserDefaults

private const val KEY_PREFIX = "project_selection_count."

class IosProjectSelectionCountStorage : ProjectSelectionCountStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun getSelectionCounts(organization: String): Map<String, Int> {
        val normalizedOrg = normalizeOrganization(organization)
        val prefix = "$KEY_PREFIX$normalizedOrg."
        val keys = defaults.dictionaryRepresentation().keys
        return buildMap {
            keys.forEach { anyKey ->
                val key = anyKey as? String ?: return@forEach
                if (!key.startsWith(prefix)) return@forEach
                val project = key.removePrefix(prefix)
                if (project.isBlank()) return@forEach
                val count = defaults.integerForKey(key).toInt()
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
            val next = defaults.integerForKey(key).toInt() + 1
            defaults.setInteger(next.toLong(), key)
        }

    override fun clearAllSelections(): Result<Unit> =
        runCatching {
            val keys = defaults.dictionaryRepresentation().keys
            keys.forEach { anyKey ->
                val key = anyKey as? String ?: return@forEach
                if (key.startsWith(KEY_PREFIX)) {
                    defaults.removeObjectForKey(key)
                }
            }
        }

    private fun normalizeOrganization(organization: String): String =
        organization.trim().lowercase()
}
