package dev.azure.desktop.domain.pr

/**
 * Debug logging for PR suggestion flow. Uses [println] so it works on all KMM targets.
 */
object PrSuggestionLog {
    private const val PREFIX = "[PRSuggestion]"

    fun d(message: String) {
        println("$PREFIX $message")
    }
}
