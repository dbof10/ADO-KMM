package dev.azure.desktop.data.auth

import dev.azure.desktop.domain.auth.PatStorage
import platform.Foundation.NSUserDefaults

private const val PAT_KEY = "ado_pat"
private const val ORG_KEY = "ado_org"

class IosPatStorage : PatStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun savePat(pat: String): Result<Unit> = runCatching {
        defaults.setObject(pat, PAT_KEY)
    }

    override fun loadPat(): String? =
        defaults.stringForKey(PAT_KEY)?.trim()?.takeIf { it.isNotEmpty() }

    override fun saveOrganization(name: String): Result<Unit> = runCatching {
        defaults.setObject(name.trim(), ORG_KEY)
    }

    override fun loadOrganization(): String? =
        defaults.stringForKey(ORG_KEY)?.trim()?.takeIf { it.isNotEmpty() }

    override fun clearCredentials() {
        defaults.removeObjectForKey(PAT_KEY)
        defaults.removeObjectForKey(ORG_KEY)
    }

    override fun clearPatOnly() {
        defaults.removeObjectForKey(PAT_KEY)
    }
}
