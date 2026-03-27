package dev.azure.desktop

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.azure.desktop.data.auth.AndroidContextHolder
import dev.azure.desktop.deeplink.AppDeepLinkBus

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AndroidContextHolder.init(applicationContext)
        deliverDeepLinkFromIntent(intent)
        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deliverDeepLinkFromIntent(intent)
    }

    private fun deliverDeepLinkFromIntent(intent: Intent?) {
        val uri = intent?.dataString ?: return
        AppDeepLinkBus.emit(uri)
    }
}
