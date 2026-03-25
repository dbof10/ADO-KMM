package dev.azure.desktop.data.auth

import dev.azure.desktop.domain.auth.PatStorage
import dev.azure.desktop.domain.auth.VerifyAndStorePatUseCase
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders

private val networkLogger =
    object : Logger {
        override fun log(message: String) {
            println("[ADO-HTTP] $message")
        }
    }

object JvmAuthServices {
    private val httpClient: HttpClient = HttpClient(CIO) {
        followRedirects = true
        install(Logging) {
            logger = networkLogger
            level = LogLevel.INFO
            sanitizeHeader { name -> name.equals(HttpHeaders.Authorization, ignoreCase = true) }
        }
    }

    val patStorage: PatStorage = OsCredentialPatStorage()

    val verifyAndStorePat: VerifyAndStorePatUseCase =
        VerifyAndStorePatUseCase(AdoRestPatVerifier(httpClient), patStorage)
}
