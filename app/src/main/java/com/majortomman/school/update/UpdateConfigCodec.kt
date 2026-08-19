package com.majortomman.school.update

import java.net.URI
import org.json.JSONObject

internal object UpdateConfigCodec {
    fun decode(json: String): String = normalize(JSONObject(json).getString("updateUrl"))

    fun normalize(value: String): String {
        val normalized = value.trim().trimEnd('/')
        require(normalized.isNotEmpty()) { "更新地址为空。" }
        val uri = URI(normalized)
        require(uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()) { "更新地址必须使用 HTTPS。" }
        require(uri.userInfo == null && uri.rawQuery == null && uri.rawFragment == null) { "更新地址格式无效。" }
        return normalized
    }
}
