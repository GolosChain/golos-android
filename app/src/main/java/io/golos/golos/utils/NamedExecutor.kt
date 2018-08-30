package io.golos.golos.utils

import com.google.common.util.concurrent.ThreadFactoryBuilder
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class NamedExecutor(val name: String) : ThreadPoolExecutor(1, 1,
        Long.MAX_VALUE,
        TimeUnit.MILLISECONDS,
        PriorityBlockingQueue(20),
        ThreadFactoryBuilder().setNameFormat("$name executor thread -%d").build())