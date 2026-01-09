package com.example.videostreaming

import android.util.Log
import okhttp3.Response
import okhttp3.WebSocket
import okio.ByteString

class WebSocketManager (private val onFrameReceived: (ByteArray) -> Unit){
    private val client = okhttp3.OkHttpClient()
    private lateinit var webSocket: WebSocket

    fun connect() {
        val request = okhttp3.Request.Builder()
            .url("ws://10.23.121.22:8000/ws/camera")
            .build()

        webSocket = client.newWebSocket(request, object :okhttp3.WebSocketListener()
        {
            override fun onOpen(ws: okhttp3.WebSocket, response: okhttp3.Response) {
                super.onOpen(ws, response)
                Log.d("WS", "Connected")
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                super.onMessage(webSocket, bytes)
                onFrameReceived(bytes.toByteArray())
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                super.onFailure(webSocket, t, response)
            }
        })
    }

    fun sendFrame(bytes: ByteArray) {

        if (::webSocket.isInitialized) {
            webSocket.send(okio.ByteString.of(*bytes))
        }
    }
}