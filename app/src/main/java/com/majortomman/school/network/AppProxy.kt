package com.majortomman.school.network

import android.content.Context
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI
import java.net.URL
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ProxyRoute {
    UPDATES,
    AI,
}

data class AppProxySettings(
    val proxyUrl: String = "",
    val useForUpdates: Boolean = false,
    val useForAi: Boolean = false,
) {
    fun proxyFor(route: ProxyRoute): Proxy? {
        val enabled = when (route) {
            ProxyRoute.UPDATES -> useForUpdates
            ProxyRoute.AI -> useForAi
        }
        if (!enabled) return null

        val raw = proxyUrl.trim()
        require(raw.isNotBlank()) { "已启用代理，但尚未填写代理地址。" }
        val normalized = if ("://" in raw) raw else "http://$raw"
        val uri = URI(normalized)
        val scheme = uri.scheme.orEmpty().lowercase()
        require(scheme in setOf("http", "https", "socks", "socks5")) {
            "代理地址仅支持 http、https、socks 或 socks5。"
        }
        require(!uri.host.isNullOrBlank()) { "代理地址缺少主机名或 IP。" }
        val defaultPort = if (scheme.startsWith("socks")) 1080 else 8080
        val port = if (uri.port > 0) uri.port else defaultPort
        require(port in 1..65535) { "代理端口必须在 1 到 65535 之间。" }
        val type = if (scheme.startsWith("socks")) Proxy.Type.SOCKS else Proxy.Type.HTTP
        return Proxy(type, InetSocketAddress(uri.host, port))
    }
}

object AppProxy {
    private const val PREFS_NAME = "school_network_proxy"
    private const val KEY_URL = "proxy_url"
    private const val KEY_UPDATES = "proxy_updates"
    private const val KEY_AI = "proxy_ai"

    @Volatile
    private var appContext: Context? = null

    private val mutableSettings = MutableStateFlow(AppProxySettings())
    val settings = mutableSettings.asStateFlow()

    fun initialize(context: Context) {
        val applicationContext = context.applicationContext
        appContext = applicationContext
        mutableSettings.value = read(applicationContext)
    }

    fun save(context: Context, settings: AppProxySettings) {
        val normalized = settings.copy(proxyUrl = settings.proxyUrl.trim())
        if (normalized.useForUpdates) normalized.proxyFor(ProxyRoute.UPDATES)
        if (normalized.useForAi) normalized.proxyFor(ProxyRoute.AI)
        val applicationContext = context.applicationContext
        applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_URL, normalized.proxyUrl)
            .putBoolean(KEY_UPDATES, normalized.useForUpdates)
            .putBoolean(KEY_AI, normalized.useForAi)
            .apply()
        appContext = applicationContext
        mutableSettings.value = normalized
    }

    fun openConnection(url: String, route: ProxyRoute): HttpURLConnection {
        val context = appContext
        val current = if (context == null) {
            mutableSettings.value
        } else {
            read(context).also { stored ->
                if (stored != mutableSettings.value) mutableSettings.value = stored
            }
        }
        val proxy = current.proxyFor(route)
        val connection = if (proxy == null) URL(url).openConnection() else URL(url).openConnection(proxy)
        return connection as HttpURLConnection
    }

    fun openConnection(context: Context, url: String, route: ProxyRoute): HttpURLConnection {
        initialize(context)
        return openConnection(url, route)
    }

    private fun read(context: Context): AppProxySettings {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return AppProxySettings(
            proxyUrl = preferences.getString(KEY_URL, "").orEmpty(),
            useForUpdates = preferences.getBoolean(KEY_UPDATES, false),
            useForAi = preferences.getBoolean(KEY_AI, false),
        )
    }
}
