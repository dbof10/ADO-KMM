package dev.azure.desktop.domain.pr

internal const val MaxPullRequestDiffLineCount = 4000

fun computePullRequestDiffLines(oldLines: List<String>, newLines: List<String>): List<PullRequestDiffLine> {
    val dp = buildLcsTable(oldLines, newLines)
    if (dp == null) {
        return listOf(
            PullRequestDiffLine.Context(
                text = "Diff too large to render (${oldLines.size} → ${newLines.size} lines).",
                oldLineNumber = null,
                newLineNumber = null,
            ),
        )
    }
    return backtrackDiffLines(dp, oldLines, newLines)
}

data class PullRequestFileLineDiffStats(
    val additions: Int,
    val removals: Int,
    val truncated: Boolean,
)

fun computePullRequestFileLineDiffStats(oldLines: List<String>, newLines: List<String>): PullRequestFileLineDiffStats {
    val dp = buildLcsTable(oldLines, newLines)
        ?: return PullRequestFileLineDiffStats(0, 0, truncated = true)
    val (additions, removals) = backtrackCounts(dp, oldLines, newLines)
    return PullRequestFileLineDiffStats(additions, removals, truncated = false)
}

private fun buildLcsTable(oldLines: List<String>, newLines: List<String>): Array<IntArray>? {
    if (oldLines.size + newLines.size > MaxPullRequestDiffLineCount) return null
    val n = oldLines.size
    val m = newLines.size
    val dp = Array(n + 1) { IntArray(m + 1) }
    for (i in n - 1 downTo 0) {
        for (j in m - 1 downTo 0) {
            dp[i][j] =
                if (oldLines[i] == newLines[j]) {
                    1 + dp[i + 1][j + 1]
                } else {
                    maxOf(dp[i + 1][j], dp[i][j + 1])
                }
        }
    }
    return dp
}

private fun backtrackDiffLines(
    dp: Array<IntArray>,
    oldLines: List<String>,
    newLines: List<String>,
): List<PullRequestDiffLine> {
    val n = oldLines.size
    val m = newLines.size
    val out = ArrayList<PullRequestDiffLine>(n + m)
    var i = 0
    var j = 0
    var oldNo = 1
    var newNo = 1
    while (i < n && j < m) {
        if (oldLines[i] == newLines[j]) {
            out += PullRequestDiffLine.Context(oldLines[i], oldLineNumber = oldNo, newLineNumber = newNo)
            i++
            j++
            oldNo++
            newNo++
        } else if (dp[i + 1][j] >= dp[i][j + 1]) {
            out += PullRequestDiffLine.Removed(oldLines[i], oldLineNumber = oldNo)
            i++
            oldNo++
        } else {
            out += PullRequestDiffLine.Added(newLines[j], newLineNumber = newNo)
            j++
            newNo++
        }
    }
    while (i < n) {
        out += PullRequestDiffLine.Removed(oldLines[i], oldLineNumber = oldNo)
        i++
        oldNo++
    }
    while (j < m) {
        out += PullRequestDiffLine.Added(newLines[j], newLineNumber = newNo)
        j++
        newNo++
    }
    return out
}

private fun backtrackCounts(dp: Array<IntArray>, oldLines: List<String>, newLines: List<String>): Pair<Int, Int> {
    val n = oldLines.size
    val m = newLines.size
    var i = 0
    var j = 0
    var additions = 0
    var removals = 0
    while (i < n && j < m) {
        if (oldLines[i] == newLines[j]) {
            i++
            j++
        } else if (dp[i + 1][j] >= dp[i][j + 1]) {
            removals++
            i++
        } else {
            additions++
            j++
        }
    }
    removals += n - i
    additions += m - j
    return additions to removals
}
