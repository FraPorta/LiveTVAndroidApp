package com.example.livetv.data.network

/** Builds and re-addresses the Acestream engine's HTTP proxy links. */
object AcestreamLinks {
    private const val PROXY_PATH = "/ace/getstream?id="
    private const val DEFAULT_PORT = 6878

    /** `ip` may also carry a port ("192.168.1.5:6879"); without one the standard 6878 is used. */
    fun proxyUrl(ip: String, id: String): String {
        val host = ip.trim()
        val hostAndPort = if (host.contains(":")) host else "$host:$DEFAULT_PORT"
        return "http://$hostAndPort$PROXY_PATH$id"
    }

    fun isProxyLink(link: String) = link.contains(PROXY_PATH)

    /**
     * Points a stored proxy link at the engine address that is set now, so changing the IP in
     * Settings applies to links that were scraped earlier. Other links are returned unchanged.
     */
    fun withIp(link: String, ip: String): String =
        if (isProxyLink(link) && ip.isNotBlank()) proxyUrl(ip, link.substringAfter(PROXY_PATH)) else link
}
