package com.scurab.videoproxy

import java.lang.IllegalStateException

const val LOG = true
const val LOG_ERR = true

inline fun logMsg(msg: () -> String?) {
    if (LOG) {
        msg()?.let { System.out.println(it) }
    }
}

inline fun logErr(msg: () -> String?) {
    if (LOG_ERR) {
        msg()?.let { System.err.println(it) }
    }
}

inline fun isa(msg: String): Nothing = throw IllegalStateException(msg)