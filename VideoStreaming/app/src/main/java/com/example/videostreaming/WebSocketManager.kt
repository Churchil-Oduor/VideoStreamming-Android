package com.example.videostreaming

import android.util.Log
import okhttp3.Response
import okhttp3.WebSocket

class WebSocketManager {
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
        })
    }

    fun sendFrame(bytes: ByteArray) {
        webSocket.send(okio.ByteString.of(*bytes))
    }
}