package dev.azure.desktop.domain.pr

/**
 * Debug logging for Azure DevOps HTTP calls. Uses [println] so it shows in Logcat (Android), Xcode, and desktop terminals.
 */
object AdoNetworkLog {
    private const val PREFIX = "[ADO-HTTP]"

    fun d(message: String) {
        println("$PREFIX $message")
    }
}
