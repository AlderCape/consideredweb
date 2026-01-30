package com.consideredweb.utils

inline fun <T> Any.traced(block: () -> T): T {
    val (className,methodName) = getCurrent()
    println("TRACE Entering $className.$methodName")

    try {
        return block()
    } finally {
        println("TRACE Exiting $className.$methodName")
    }
}

inline fun getCurrent() = StackWalker.getInstance().walk { stream ->
    stream.findFirst()
        .map { Pair(it.className, it.methodName) }
        .orElse(Pair("Unknown class", "Unknown method"))
}
