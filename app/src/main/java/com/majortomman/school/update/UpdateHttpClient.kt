package com.majortomman.school.update

import android.content.Context
import com.majortomman.school.BuildConfig
import com.majortomman.school.network.AppProxy
import com.majortomman.school.network.ProxyRoute
import java.io.ByteArrayOutputStream

internal class UpdateHttpClient(context: Context) {
    private val appContext = context.applicationContext

    fun get(url: String, maxBytes: Int, accept: String = "application/octet-stream, application/json"): ByteArray {
        val connection = AppProxy.openConnection(appContext, url, ProxyRoute.UPDATES).apply {
            instanceFollowRedirects = true
            connectTimeout = 15_000
            readTimeout = 20_000
            requestMethod = "GET"
            setRequestProperty("Accept", accept)
            setRequestProperty("User-Agent", "School/${BuildConfig.VERSION_NAME}")
            setRequestProperty("Cache-Control", "no-cache")
        }
        try {
            val responseCode = connection.responseCode
            require(responseCode in 200..299) { "更新服务器返回 $responseCode。" }
            val output = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (count == 0) continue
                    total += count
                    require(total <= maxBytes) { "更新响应过大。" }
                    output.write(buffer, 0, count)
                }
            }
            return output.toByteArray()
        } finally {
            connection.disconnect()
        }
    }
}
