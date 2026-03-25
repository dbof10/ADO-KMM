package dev.azure.desktop.data.auth

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.plugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.HttpStatusCode

/**
 * Registers a Ktor [HttpSend] interceptor (request/response middleware): after each request,
 * a 401 invokes [onUnauthorized], then fails the call with [ResponseException].
 *
 * Must be called on a fully constructed [HttpClient] (after the `HttpClient { }` block), because
 * [HttpSend] interceptors are added to the plugin instance via [plugin].
 */
internal fun HttpClient.installAdoUnauthorizedMiddleware(
    onUnauthorized: () -> Unit,
) {
    plugin(HttpSend).intercept { request: HttpRequestBuilder ->
        val call = execute(request)
        if (call.response.status == HttpStatusCode.Unauthorized) {
            onUnauthorized()
            throw ResponseException(call.response, "Session expired or token rejected.")
        }
        call
    }
}
