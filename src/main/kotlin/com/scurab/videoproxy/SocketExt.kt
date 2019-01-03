package com.scurab.videoproxy

import java.io.*
import java.net.Socket
import java.nio.charset.Charset

fun Socket.readUrlPath(): String {
    val buffer = ByteArray(1024)
    val inputStream = BufferedInputStream(getInputStream())
    val read = inputStream.read(buffer, 0, buffer.size)
    val headers = String(buffer, 0, read, Charset.forName("utf-8"))
    try {
        val url = headers.split("\\n".toRegex())
                .dropLastWhile { it.isEmpty() }
                .first()
                .split("\\s".toRegex())
                .dropLastWhile { it.isEmpty() }[1]
        return if (url == "/") "" else url
    } catch (e: Exception) {
        logErr { "Unable to find url from headers\n$headers" }
        throw e
    }
}

private val HEADER = "HTTP/1.1 OK \r\nContent-Type: video/mp2t\r\n\r\n".toByteArray(Charset.forName("utf-8"))

fun Socket.sendHttpHeader() {
    getOutputStream().write(HEADER)
}