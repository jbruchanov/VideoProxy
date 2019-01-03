package com.scurab.videoproxy

import java.net.InetSocketAddress
import java.net.ServerSocket

fun main(args: Array<String>) {
    val handler = SocketHandler("http://192.168.168.20:9981")
    Thread(handler).start()
    val socket = ServerSocket()
    socket.bind(InetSocketAddress("0.0.0.0", 8080))
    while (true) {
        val clientSocket = socket.accept()
        try {
            handler.attachSocket(clientSocket)
        } catch (e: Throwable) {
            clientSocket.close()
            e.printStackTrace()
        }
    }
}