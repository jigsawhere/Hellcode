package com.hellcode.app

import android.app.Application
import android.util.Log
import com.hellcode.app.utils.CryptoStore
import java.util.Properties

class HellApp : Application() {
    companion object {
        lateinit var instance: HellApp
            private set
    }

    val cryptoStore: CryptoStore by lazy { CryptoStore(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        saveInitialApiKeys()
    }

    private fun saveInitialApiKeys() {
        val properties = Properties().apply {
            setProperty("GEMINI_API_KEY", "YOUR_ACTUAL_GEMINI_API_KEY_HERE")
            setProperty("OPENROUTER_KEY", "YOUR_ACTUAL_OPENROUTER_API_KEY_HERE")
            setProperty("COPILOT_CLIENT_ID", "YOUR_ACTUAL_COPILOT_CLIENT_ID_HERE")
        }
        val outputStream = java.io.ByteArrayOutputStream()
        properties.store(outputStream, null)
        try {
            cryptoStore.saveSecrets(outputStream.toString())
            Log.d("HellApp", "API keys saved securely.")
        } catch (e: Exception) {
            Log.e("HellApp", "Failed to save API keys securely.", e)
        }
    }
}