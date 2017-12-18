package io.golos.golos

import java.util.concurrent.Executor

/**
 * Created by yuri on 15.12.17.
 */
object MainThreadExecutor : Executor{
    override fun execute(p0: Runnable?) {
        p0?.run()
    }
}