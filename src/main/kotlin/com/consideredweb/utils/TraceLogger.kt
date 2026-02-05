package com.consideredweb.utils

import org.slf4j.LoggerFactory

@PublishedApi
internal val traceLogger = LoggerFactory.getLogger("com.consideredweb.trace")

inline fun <T> Any.traced(block: () -> T): T {
    val (className, methodName) = getCurrent()
    traceLogger.trace("Entering {}.{}", className, methodName)

    try {
        return block()
    } finally {
        traceLogger.trace("Exiting {}.{}", className, methodName)
    }
}

inline fun getCurrent() = StackWalker.getInstance().walk { stream ->
    stream.findFirst()
        .map { Pair(it.className, it.methodName) }
        .orElse(Pair("Unknown class", "Unknown method"))
}
