package dev.azure.desktop.data.auth

import android.content.Context

object AndroidContextHolder {
    private lateinit var applicationContext: Context

    fun init(context: Context) {
        applicationContext = context.applicationContext
    }

    fun get(): Context {
        check(::applicationContext.isInitialized) {
            "AndroidContextHolder.init must be called from Application or Activity.onCreate."
        }
        return applicationContext
    }
}
