package com.scurab.videoproxy

import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

class SocketSet {
    private val pending = mutableListOf<PendingOp>()
    private val items = mutableSetOf<Socket>()
    private val forEachIterating = AtomicBoolean()

    val size: Int get() = items.size

    val sizeIncludingPending: Int get() = items.size + pending.size

    fun add(item: Socket) {
        if (forEachIterating.get()) {
            synchronized(pending) {
                pending.add(Add(item))
            }
        } else {
            items.add(item)
        }
    }

    fun remove(item: Socket) {
        if (forEachIterating.get()) {
            synchronized(pending) {
                pending.add(Remove(item))
            }
        } else {
            items.remove(item)
        }
    }

    fun forEach(func: (java.net.Socket) -> Unit) {
        if (!forEachIterating.compareAndSet(false, true)) {
            isa("Someone is already iterating")
        }
        try {
            items.forEach(func)
        } finally {
            if (!forEachIterating.compareAndSet(true, false)) {
                isa("For Each stop invalid state")
            }
        }
        processPendingOps()
    }

    private fun processPendingOps() {
        synchronized(pending) {
            pending.forEach {
                it.handleOp(items)
            }
            pending.clear()
        }
    }

    fun closeClients() {
        synchronized(pending) {
            pending.add(CloseAndRemove(items.toSet()))
        }
    }
}

private sealed class PendingOp {
    abstract fun handleOp(items: MutableSet<Socket>)
}

private class Add(val socket: Socket) : PendingOp() {
    override fun handleOp(items: MutableSet<Socket>) {
        items.add(socket)
    }
}

private class Remove(val socket: Socket) : PendingOp() {
    override fun handleOp(items: MutableSet<Socket>) {
        items.remove(socket)
    }
}

private class CloseAndRemove(val sockets: Set<Socket>) : PendingOp() {
    override fun handleOp(items: MutableSet<Socket>) {
        sockets.forEach {
            it.close()
            items.remove(it)
        }
    }
}
