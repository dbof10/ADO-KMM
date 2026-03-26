package dev.azure.desktop

import platform.UIKit.UIDevice

actual fun platformName(): String {
    val system = UIDevice.currentDevice.systemName
    val version = UIDevice.currentDevice.systemVersion
    return "$system $version"
}
