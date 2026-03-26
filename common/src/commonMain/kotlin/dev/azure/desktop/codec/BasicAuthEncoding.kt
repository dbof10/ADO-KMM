package dev.azure.desktop.codec

private const val B64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

/**
 * RFC 2045 Base64 for HTTP Basic credentials (`Authorization: Basic …`).
 */
fun basicAuthHeader(pat: String): String {
    val trimmed = pat.trim()
    require(trimmed.isNotBlank()) { "Session token is missing. Please sign in again." }
    return "Basic ${":$trimmed".encodeToByteArray().encodeBase64()}"
}

private fun ByteArray.encodeBase64(): String {
    val out = StringBuilder(size * 4 / 3 + 4)
    var i = 0
    while (i < size) {
        val b1 = this[i].toInt() and 0xFF
        val b2 = if (i + 1 < size) this[i + 1].toInt() and 0xFF else 0
        val b3 = if (i + 2 < size) this[i + 2].toInt() and 0xFF else 0
        val bitmap = (b1 shl 16) or (b2 shl 8) or b3
        when (size - i) {
            1 -> {
                out.append(B64[(bitmap shr 18) and 63])
                out.append(B64[(bitmap shr 12) and 63])
                out.append("==")
            }
            2 -> {
                out.append(B64[(bitmap shr 18) and 63])
                out.append(B64[(bitmap shr 12) and 63])
                out.append(B64[(bitmap shr 6) and 63])
                out.append('=')
            }
            else -> {
                out.append(B64[(bitmap shr 18) and 63])
                out.append(B64[(bitmap shr 12) and 63])
                out.append(B64[(bitmap shr 6) and 63])
                out.append(B64[bitmap and 63])
            }
        }
        i += 3
    }
    return out.toString()
}
