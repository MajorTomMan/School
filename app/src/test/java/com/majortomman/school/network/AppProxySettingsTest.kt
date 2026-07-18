package com.majortomman.school.network

import java.net.InetSocketAddress
import java.net.Proxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppProxySettingsTest {
    @Test
    fun disabledRouteUsesDirectConnection() {
        val settings = AppProxySettings(
            proxyUrl = "http://127.0.0.1:7890",
            useForUpdates = true,
            useForAi = false,
        )

        assertNull(settings.proxyFor(ProxyRoute.AI))
        assertEquals(Proxy.Type.HTTP, settings.proxyFor(ProxyRoute.UPDATES)?.type())
    }

    @Test
    fun parsesHttpProxyAndDefaultScheme() {
        val proxy = AppProxySettings(
            proxyUrl = "192.168.1.2:7890",
            useForUpdates = true,
        ).proxyFor(ProxyRoute.UPDATES)!!
        val address = proxy.address() as InetSocketAddress

        assertEquals(Proxy.Type.HTTP, proxy.type())
        assertEquals("192.168.1.2", address.hostString)
        assertEquals(7890, address.port)
    }

    @Test
    fun parsesSocks5Proxy() {
        val proxy = AppProxySettings(
            proxyUrl = "socks5://127.0.0.1:1088",
            useForAi = true,
        ).proxyFor(ProxyRoute.AI)!!
        val address = proxy.address() as InetSocketAddress

        assertEquals(Proxy.Type.SOCKS, proxy.type())
        assertEquals("127.0.0.1", address.hostString)
        assertEquals(1088, address.port)
    }

    @Test
    fun enabledRouteRequiresUsableAddress() {
        val result = runCatching {
            AppProxySettings(useForUpdates = true).proxyFor(ProxyRoute.UPDATES)
        }

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("代理地址"))
    }
}
