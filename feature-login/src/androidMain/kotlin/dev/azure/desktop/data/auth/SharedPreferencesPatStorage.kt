package dev.azure.desktop.data.auth

import android.content.Context
import dev.azure.desktop.domain.auth.PatStorage

private const val PREFS_NAME = "ado_desktop_auth"
private const val PAT_KEY = "ado_pat"
private const val ORG_KEY = "ado_org"

class SharedPreferencesPatStorage(
    context: Context,
) : PatStorage {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun savePat(pat: String): Result<Unit> = runCatching {
        prefs.edit().putString(PAT_KEY, pat).apply()
    }

    override fun loadPat(): String? = prefs.getString(PAT_KEY, null)?.trim()?.takeIf { it.isNotEmpty() }

    override fun saveOrganization(name: String): Result<Unit> = runCatching {
        prefs.edit().putString(ORG_KEY, name.trim()).apply()
    }

    override fun loadOrganization(): String? =
        prefs.getString(ORG_KEY, null)?.trim()?.takeIf { it.isNotEmpty() }

    override fun clearCredentials() {
        prefs.edit().remove(PAT_KEY).remove(ORG_KEY).apply()
    }

    override fun clearPatOnly() {
        prefs.edit().remove(PAT_KEY).apply()
    }
}
