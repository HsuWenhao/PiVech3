package com.example.pivech3.net

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.atomic.AtomicBoolean

/**
 * WebSocket client for Raspberry Pi motion-control service.
 * UI should only call [connect]/[sendMotorSpeeds]/[close].
 */
class RaspberryPiMotionWsClient(
    private val okHttpClient: OkHttpClient = OkHttpClient(),
) {

    interface Logger {
        fun d(msg: String)
        fun w(msg: String)
    }

    @Volatile
    private var webSocket: WebSocket? = null

    @Volatile
    private var currentUrl: String? = null

    private val isOpen = AtomicBoolean(false)

    var logger: Logger? = null

    fun connect(url: String) {
        if (url.isBlank()) return
        if (url == currentUrl && webSocket != null) return

        close()
        currentUrl = url

        val req = Request.Builder().url(url).build()
        webSocket = okHttpClient.newWebSocket(req, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isOpen.set(true)
                logger?.d("motion ws open: $url")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isOpen.set(false)
                logger?.w("motion ws failure: ${t.message}")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isOpen.set(false)
                logger?.d("motion ws closed: code=$code reason=$reason")
            }
        })
    }

    /**
     * Sends a compact JSON payload: {"left":int,"right":int}
     */
    fun sendMotorSpeeds(left: Int, right: Int): Boolean {
        val ws = webSocket ?: return false
        if (!isOpen.get()) return false
        val l = left.coerceIn(-100, 100)
        val r = right.coerceIn(-100, 100)
        val payload = "{\"left\":$l,\"right\":$r}"
        return ws.send(payload)
    }

    fun close() {
        webSocket?.close(1000, "bye")
        webSocket = null
        currentUrl = null
        isOpen.set(false)
    }
}
