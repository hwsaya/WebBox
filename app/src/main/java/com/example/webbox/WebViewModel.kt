WebViewModel.kt
package com.example.webbox

import android.app.Application
import android.webkit.CookieManager
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@Stable
class WebViewModel(application: Application) : AndroidViewModel(application) {
    private val prefsHelper = PrefsHelper(application)

    var webSites by mutableStateOf(prefsHelper.loadSites())
        private set
    var lruEnabled by mutableStateOf(prefsHelper.getLruEnabled())
        private set
    var lruSize by mutableStateOf(prefsHelper.getLruSize())
        private set
    var siteCredentials by mutableStateOf(prefsHelper.getSiteCredentials())
        private set

    private val _uiEvent = Channel<String>()
    val uiEvent = _uiEvent.receiveAsFlow()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private var lastBackupHash = 0
    private var debounceBackupJob: Job? = null
    private var isBackingUp = false

    init {
        WebViewPool.updateConfig(lruEnabled, lruSize)
        val config = prefsHelper.getWebDavConfig()
        if (config.autoBackup && config.url.isNotBlank() && config.pass.isNotBlank()) {
            enqueueAutoBackup()
        }
    }

    fun enqueueAutoBackup() {
        if (!prefsHelper.getWebDavConfig().autoBackup) return
        debounceBackupJob?.cancel()
        debounceBackupJob = viewModelScope.launch {
            delay(2500)
            backupToWebDav(isAutoBackup = true)
        }
    }

    fun updateLruSettings(enabled: Boolean, size: Int) {
        lruEnabled = enabled
        lruSize = size
        prefsHelper.saveLruSettings(enabled, size)
        WebViewPool.updateConfig(enabled, size)
    }

    fun addSite(name: String, url: String) = updateAndSave(webSites + WebSite(name, formatUrl(url)))

    fun updateSite(old: WebSite, name: String, url: String) =
        updateAndSave(webSites.map { if (it == old) WebSite(name, formatUrl(url)) else it })

    fun deleteSite(site: WebSite) {
        updateAndSave(webSites.filter { it != site })
        val newCreds = siteCredentials.filter { it.url != site.url }
        siteCredentials = newCreds
        prefsHelper.saveSiteCredentials(newCreds)
        WebViewPool.remove(site.url)
    }

    fun swapSites(fromIndex: Int, toIndex: Int) {
        if (fromIndex in webSites.indices && toIndex in webSites.indices) {
            val list = webSites.toMutableList()
            list.add(toIndex, list.removeAt(fromIndex))
            updateAndSave(list)
        }
    }

    fun saveCredential(url: String, user: String, pass: String) {
        val existing = siteCredentials.toMutableList()
        val index = existing.indexOfFirst { it.url == url }
        if (index >= 0) {
            existing[index] = SiteCredential(url, user, pass)
        } else {
            existing.add(SiteCredential(url, user, pass))
        }
        siteCredentials = existing
        prefsHelper.saveSiteCredentials(existing)
        enqueueAutoBackup()
    }

    fun getCredentialForUrl(url: String): SiteCredential? = siteCredentials.find { it.url == url }

    private fun formatUrl(url: String): String =
        if (!url.startsWith("http://") && !url.startsWith("https://")) "http://$url" else url

    private fun updateAndSave(newList: List<WebSite>) {
        webSites = newList
        prefsHelper.saveSites(newList)
        enqueueAutoBackup()
    }

    fun loadWebDavConfig() = prefsHelper.getWebDavConfig()
    fun saveWebDavConfig(config: WebDavConfig) = prefsHelper.saveWebDavConfig(config)

    fun backupToWebDav(isAutoBackup: Boolean = false) {
        val config = prefsHelper.getWebDavConfig()
        if (isAutoBackup && !config.autoBackup) return
        if (isBackingUp) {
            if (!isAutoBackup) viewModelScope.launch { _uiEvent.send("后台正忙，请稍候...") }
            return
        }
        if (config.url.isBlank() || config.user.isBlank() || config.pass.isBlank()) {
            if (!isAutoBackup) viewModelScope.launch { _uiEvent.send("请先完善配置") }
            return
        }

        isBackingUp = true
        if (!isAutoBackup) viewModelScope.launch { _uiEvent.send("开始备份...") }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cookieManager = CookieManager.getInstance()
                val backupData = webSites.map { site ->
                    val cred = siteCredentials.find { it.url == site.url }
                    mapOf("name" to site.name, "url" to site.url,
                        "cookie" to (cookieManager.getCookie(site.url) ?: ""),
                        "loginUser" to (cred?.user ?: ""),
                        "loginPass" to (cred?.pass ?: "")
                    )
                }
                val rawJson = Gson().toJson(backupData)
                val currentHash = rawJson.hashCode()
                if (isAutoBackup && currentHash == lastBackupHash) return@launch

                val encrypted = CryptoUtils.encrypt(rawJson, config.pass)
                val baseUrl = config.url.trimEnd('/')
                val dirUrl = "$baseUrl/WebBox"
                val fileUrl = "$baseUrl/WebBox/WebBox_Secure.dat"
                val credential = Credentials.basic(config.user, config.pass)

                try {
                    httpClient.newCall(
                        Request.Builder().url(dirUrl).method("MKCOL", null)
                            .header("Authorization", credential).build()
                    ).execute().close()
                } catch (_: Exception) {}

                val deadline = System.currentTimeMillis() + 20_000L
                var dirReady = false
                val propfindReq = Request.Builder().url(dirUrl)
                    .method("PROPFIND", ByteArray(0).toRequestBody(null))
                    .header("Depth", "0").header("Authorization", credential).build()

                while (System.currentTimeMillis() < deadline) {
                    try {
                        httpClient.newCall(propfindReq).execute().use { r ->
                            if (r.code == 207 || r.code in 200..299) dirReady = true
                        }
                    } catch (_: Exception) {}
                    if (dirReady) break
                    if (!isAutoBackup && (deadline - System.currentTimeMillis()) > 19_000L) {
                        _uiEvent.send("同步云端映射中...")
                    }
                    delay(2000)
                }

                if (!dirReady) {
                    if (!isAutoBackup) _uiEvent.send("备份取消：云端目录同步超时")
                    return@launch
                }

                httpClient.newCall(
                    Request.Builder().url(fileUrl)
                        .put(encrypted.toRequestBody("application/octet-stream".toMediaType()))
                        .header("Authorization", credential).build()
                ).execute().use { r ->
                    if (r.code in 200..299) {
                        lastBackupHash = currentHash
                        if (!isAutoBackup) _uiEvent.send("备份成功")
                    } else {
                        if (!isAutoBackup) _uiEvent.send("备份失败: HTTP ${r.code}")
                    }
                }
            } catch (e: Exception) {
                if (!isAutoBackup) _uiEvent.send("备份异常: ${e.message}")
            } finally {
                isBackingUp = false
            }
        }
    }

    fun restoreFromWebDav() {
        val config = prefsHelper.getWebDavConfig()
        if (config.url.isBlank() || config.user.isBlank() || config.pass.isBlank()) {
            viewModelScope.launch { _uiEvent.send("请补充配置") }
            return
        }
        viewModelScope.launch { _uiEvent.send("正在从云端恢复...") }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fileUrl = "${config.url.trimEnd('/')}/WebBox/WebBox_Secure.dat"
                val credential = Credentials.basic(config.user, config.pass)
                httpClient.newCall(
                    Request.Builder().url(fileUrl).get().header("Authorization", credential).build()
                ).execute().use { response ->
                    if (response.code !in 200..299) {
                        _uiEvent.send("未发现备份文件")
                        return@use
                    }
                    val content = response.body?.string() ?: ""
                    withContext(Dispatchers.Main) {
                        val json = if (content.trimStart().startsWith("[")) content
                                   else CryptoUtils.decrypt(content, config.pass)
                        if (json.isBlank()) { _uiEvent.send("解密失败，请检查密码"); return@withContext }
                        try {
                            val type = object : TypeToken<List<Map<String, String>>>() {}.type
                            val list: List<Map<String, String>> = Gson().fromJson(json, type) ?: emptyList()
                            val cookieManager = CookieManager.getInstance()
                            val newCreds = mutableListOf<SiteCredential>()
                            val newSites = list.mapNotNull { map ->
                                val name = map["name"].orEmpty()
                                val url = map["url"].orEmpty()
                                if (name.isBlank() || url.isBlank()) return@mapNotNull null
                                map["cookie"]?.takeIf { it.isNotBlank() }
                                    ?.let { cookieManager.setCookie(url, it) }
                                
                                val user = map["loginUser"].orEmpty()
                                val pass = map["loginPass"].orEmpty()
                                if (user.isNotBlank() || pass.isNotBlank()) {
                                    newCreds.add(SiteCredential(url, user, pass))
                                }
                                
                                WebSite(name, url)
                            }
                            cookieManager.flush()
                            webSites = newSites
                            siteCredentials = newCreds
                            prefsHelper.saveSites(newSites)
                            prefsHelper.saveSiteCredentials(newCreds)
                            lastBackupHash = json.hashCode()
                            _uiEvent.send("配置恢复成功")
                        } catch (_: Exception) { _uiEvent.send("解析失败") }
                    }
                }
            } catch (e: Exception) { _uiEvent.send("异常: ${e.message}") }
        }
    }

    override fun onCleared() {
        super.onCleared()
        WebViewPool.updateConfig(false, 0)
    }
}
