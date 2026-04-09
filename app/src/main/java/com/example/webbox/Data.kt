package com.example.webbox

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class WebSite(val name: String, val url: String)
data class WebDavConfig(val url: String, val user: String, val pass: String, val autoBackup: Boolean = false)

class PrefsHelper(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("web_manager_v2", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val securePrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            context,
            "secure_webdav_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun loadSites(): List<WebSite> {
        val json = prefs.getString("sites_json", null) ?: return emptyList()
        val type = TypeToken.getParameterized(List::class.java, WebSite::class.java).type
        return try { gson.fromJson(json, type) } catch (e: Exception) { emptyList() }
    }

    fun saveSites(sites: List<WebSite>) =
        prefs.edit().putString("sites_json", gson.toJson(sites)).apply()

    fun getWebDavConfig() = WebDavConfig(
        url = securePrefs.getString("webdav_url", "") ?: "",
        user = securePrefs.getString("webdav_user", "") ?: "",
        pass = securePrefs.getString("webdav_pass", "") ?: "",
        autoBackup = securePrefs.getBoolean("webdav_auto_backup", false)
    )

    fun saveWebDavConfig(config: WebDavConfig) {
        securePrefs.edit()
            .putString("webdav_url", config.url)
            .putString("webdav_user", config.user)
            .putString("webdav_pass", config.pass)
            .putBoolean("webdav_auto_backup", config.autoBackup)
            .apply()
    }

    fun getLruEnabled(): Boolean = prefs.getBoolean("lru_enabled", false)
    fun getLruSize(): Int = prefs.getInt("lru_size", 3)
    fun saveLruSettings(enabled: Boolean, size: Int) {
        prefs.edit().putBoolean("lru_enabled", enabled).putInt("lru_size", size).apply()
    }
}
