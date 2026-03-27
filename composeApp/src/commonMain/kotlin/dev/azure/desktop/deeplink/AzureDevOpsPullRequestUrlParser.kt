package dev.azure.desktop.deeplink

/** Parsed Azure DevOps web URL for a pull request (dev.azure.com). */
data class AzureDevOpsPullRequestLink(
    val organization: String,
    val project: String,
    val repository: String,
    val pullRequestId: Int,
)

/**
 * Parses URLs like
 * `https://dev.azure.com/{org}/{project}/_git/{repository}/pullrequest/{id}`.
 *
 * Ignores surrounding whitespace, strips a trailing fragment/query from the path,
 * and tolerates accidental prefix characters before `http` in pasted text.
 */
fun parseAzureDevOpsPullRequestUrl(raw: String): AzureDevOpsPullRequestLink? {
    val extracted = extractHttpUrl(raw) ?: return null
    val path = devAzurePathAfterHost(extracted) ?: return null
    val segments =
        path
            .substringBefore('?')
            .substringBefore('#')
            .trim('/')
            .split('/')
            .filter { it.isNotEmpty() }
            .map { percentDecode(it) }
    val gitIdx = segments.indexOf("_git")
    if (gitIdx < 2 || gitIdx + 3 >= segments.size) return null
    val prKey = segments[gitIdx + 2].lowercase()
    if (prKey != "pullrequest" && prKey != "pullrequests") return null
    val id = segments[gitIdx + 3].toIntOrNull() ?: return null
    if (id < 1) return null
    val organization = segments[gitIdx - 2]
    val project = segments[gitIdx - 1]
    val repository = segments[gitIdx + 1]
    if (organization.isBlank() || project.isBlank()) return null
    return AzureDevOpsPullRequestLink(
        organization = organization,
        project = project,
        repository = repository,
        pullRequestId = id,
    )
}

private fun extractHttpUrl(raw: String): String? {
    val trimmed = raw.trim()
    val match = Regex("""(https?://[^\s"'<>]+)""", RegexOption.IGNORE_CASE).find(trimmed)
    val url = match?.value?.trimEnd('/') ?: trimmed
    if (!url.startsWith("http://", ignoreCase = true) &&
        !url.startsWith("https://", ignoreCase = true)
    ) {
        return null
    }
    return url
}

private fun devAzurePathAfterHost(url: String): String? {
    val noFragment = url.substringBefore('#')
    val schemeEnd = noFragment.indexOf("://")
    if (schemeEnd < 0) return null
    val afterScheme = noFragment.substring(schemeEnd + 3)
    val slash = afterScheme.indexOf('/')
    if (slash < 0) return null
    val hostPort = afterScheme.substring(0, slash).lowercase()
    if (!hostPort.startsWith("dev.azure.com")) return null
    return afterScheme.substring(slash + 1)
}

internal fun percentDecode(s: String): String {
    val out = StringBuilder(s.length)
    var i = 0
    while (i < s.length) {
        when {
            s[i] == '%' && i + 2 < s.length -> {
                val hex = s.substring(i + 1, i + 3).toIntOrNull(16)
                if (hex != null && hex in 0..0xFFFF) {
                    out.append(hex.toChar())
                    i += 3
                } else {
                    out.append(s[i])
                    i++
                }
            }
            s[i] == '+' -> {
                out.append(' ')
                i++
            }
            else -> {
                out.append(s[i])
                i++
            }
        }
    }
    return out.toString()
}
