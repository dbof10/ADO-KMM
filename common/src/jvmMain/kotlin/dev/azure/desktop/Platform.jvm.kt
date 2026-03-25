package dev.azure.desktop

actual fun platformName(): String {
    val name = System.getProperty("os.name") ?: "JVM"
    val version = System.getProperty("os.version") ?: ""
    val jvm = Runtime.version().toString()
    return "$name $version ($jvm)"
}
