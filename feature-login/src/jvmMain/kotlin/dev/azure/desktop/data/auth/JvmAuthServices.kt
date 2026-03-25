package dev.azure.desktop.data.auth

import dev.azure.desktop.domain.auth.PatStorage
import dev.azure.desktop.domain.auth.VerifyAndStorePatUseCase
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import java.util.concurrent.atomic.AtomicReference

private val networkLogger =
    object : Logger {
        override fun log(message: String) {
            println("[ADO-HTTP] $message")
        }
    }

private fun baseHttpClient(): HttpClient =
    HttpClient(CIO) {
        followRedirects = true
        install(Logging) {
            logger = networkLogger
            level = LogLevel.INFO
            sanitizeHeader { name -> name.equals(HttpHeaders.Authorization, ignoreCase = true) }
        }
    }

object JvmAuthServices {
    private val onSessionUnauthorizedRef = AtomicReference<() -> Unit>({})

    fun setOnSessionUnauthorized(handler: () -> Unit) {
        onSessionUnauthorizedRef.set(handler)
    }

    /** PAT entry flow only; 401 here must not trigger global sign-out. */
    private val patVerificationHttpClient: HttpClient = baseHttpClient()

    /** All authenticated ADO calls; 401 runs [setOnSessionUnauthorized] (post to UI thread there), then throws. */
    val sessionHttpClient: HttpClient by lazy {
        baseHttpClient().also { client ->
            client.installAdoUnauthorizedMiddleware {
                onSessionUnauthorizedRef.get().invoke()
            }
        }
    }

    val patStorage: PatStorage = OsCredentialPatStorage()

    val verifyAndStorePat: VerifyAndStorePatUseCase =
        VerifyAndStorePatUseCase(AdoRestPatVerifier(patVerificationHttpClient), patStorage)
}
