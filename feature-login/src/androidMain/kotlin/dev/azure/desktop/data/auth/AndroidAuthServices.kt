package dev.azure.desktop.data.auth

import dev.azure.desktop.domain.auth.PatStorage
import dev.azure.desktop.domain.auth.VerifyAndStorePatUseCase
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

private fun baseHttpClient(): HttpClient =
    HttpClient(OkHttp) {
        followRedirects = true
    }

object AndroidAuthServices {
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

    val patStorage: PatStorage by lazy { SharedPreferencesPatStorage(AndroidContextHolder.get()) }

    val verifyAndStorePat: VerifyAndStorePatUseCase by lazy {
        VerifyAndStorePatUseCase(AdoRestPatVerifier(patVerificationHttpClient), patStorage)
    }
}
