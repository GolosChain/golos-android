package io.golos.golos.repository

import io.golos.golos.repository.persistence.Persister
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Created by yuri on 05.03.18.
 */
class NotificationsRepository(val workerExecutor: Executor = Executors.newSingleThreadExecutor(),
                              val persister: Persister) {

}