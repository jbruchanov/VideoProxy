package com.scurab.videoproxy

import java.io.IOException
import java.net.HttpURLConnection
import java.net.Socket
import java.net.SocketException
import java.net.URL

private const val BUFFER_SIZE = (16 * 1024).toLong()

class SocketHandler(private val videoStreamUrl: String) : Runnable {

    private val socketsLock = Object()
    private val videoStreamLock = Object()
    private val videoStreamBuffer = ByteArray(BUFFER_SIZE.toInt())
    private var videoStreamConnection: HttpURLConnection? = null
    private var videoUrlPath: String? = null
    private val sockets = SocketSet()

    fun attachSocket(socket: Socket) {
        try {
            logMsg { "Socket attaching ${socket.remoteSocketAddress}" }
            var urlPathToOpen = socket
                    .readUrlPath()
                    .takeIf { it.isNotEmpty() } ?: videoUrlPath


            if (urlPathToOpen == null) {
                isa("Unknown url to open")
            }

            videoUrlPath = openVideoUrl(urlPathToOpen)

            socket.sendHttpHeader()
            sockets.add(socket)
            synchronized(socketsLock) {
                socketsLock.notifyAll()
            }

            logMsg { "Socket attached ${socket.remoteSocketAddress}" }
        } catch (e: Throwable) {
            logMsg { "Removed socket ${socket.remoteSocketAddress}" }
        }
    }

    private fun openVideoUrl(urlPath: String): String {
        if (urlPath != videoUrlPath || videoStreamConnection == null) {
            sockets.closeClients()
            synchronized(videoStreamLock) {
                try {
                    videoStreamConnection?.let { closeVideoStream(it) }
                    val fullPathUrl = videoStreamUrl + urlPath
                    logMsg { "Opening video stream:'$fullPathUrl'" }
                    videoStreamConnection = (URL(fullPathUrl).openConnection() as HttpURLConnection)
                            .apply {
                                setRequestProperty("User-Agent", "VLC/3.0.4 LibVLC/3.0.4")
                            }

                } catch (e: IOException) {
                    throw IllegalStateException(e)
                }
            }
        }
        return urlPath
    }

    override fun run() {
        while (true) {
            while (sockets.size == 0) {
                synchronized(socketsLock) {
                    socketsLock.wait()
                }
            }
            sending()
        }
    }

    private fun sending() {
        var read = 0
        var error = false
        var clients = 0
        while (true) {
            videoStreamConnection?.let {
                try {
                    read = it.inputStream.read(videoStreamBuffer, 0, BUFFER_SIZE.toInt())
                    if (read <= 0) {
                        return
                    }
                } catch (e: SocketException) {
                    closeVideoStream(it)
                    sockets.closeClients()
                    return
                }

                if (read >= 0) {
                    error = false
                    sockets.forEach { socket ->
                        try {
                            socket.getOutputStream().write(videoStreamBuffer, 0, read)
                        } catch (e: Throwable) {
                            sockets.remove(socket)
                            logMsg { "Removed socket ${socket.remoteSocketAddress}" }
                            error = true
                        }
                    }
                }

                if (sockets.sizeIncludingPending == 0) {
                    closeVideoStream(it)
                }

                if (clients != sockets.size) {
                    clients = sockets.size
                    logMsg { "Clients $clients" }
                }
            } ?: return
        }
    }

    private fun closeVideoStream(connection: HttpURLConnection) {
        synchronized(videoStreamLock) {
            videoStreamConnection
                    ?.takeIf { connection == videoStreamConnection }
                    ?.let {
                        logMsg { "Closing video stream" }
                        it.disconnect()
                        videoStreamConnection = null
                    }
        }
    }
}