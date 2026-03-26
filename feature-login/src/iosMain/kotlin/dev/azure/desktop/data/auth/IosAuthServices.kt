package dev.azure.desktop.data.auth

import dev.azure.desktop.domain.auth.PatStorage
import dev.azure.desktop.domain.auth.VerifyAndStorePatUseCase
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

private fun baseHttpClient(): HttpClient =
    HttpClient(Darwin) {
        followRedirects = true
    }

object IosAuthServices {
    private var onSessionUnauthorized: () -> Unit = {}

    fun setOnSessionUnauthorized(handler: () -> Unit) {
        onSessionUnauthorized = handler
    }

    private val patVerificationHttpClient: HttpClient = baseHttpClient()

    val sessionHttpClient: HttpClient by lazy {
        baseHttpClient().also { client ->
            client.installAdoUnauthorizedMiddleware { onSessionUnauthorized() }
        }
    }

    val patStorage: PatStorage = IosPatStorage()

    val verifyAndStorePat: VerifyAndStorePatUseCase =
        VerifyAndStorePatUseCase(AdoRestPatVerifier(patVerificationHttpClient), patStorage)
}
