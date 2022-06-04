package dev.emortal.backrooms

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

abstract class MinestomRunnable(
    var delay: Duration = Duration.ZERO,
    var repeat: Duration = Duration.ZERO,
    var iterations: Int = -1,
    val coroutineScope: CoroutineScope
) {

    abstract suspend fun run()

    private val keepRunning = AtomicBoolean(true)
    private var job: Job? = null
    private val tryRun = suspend {
        try {
            run()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    var currentIteration = AtomicInteger(0)

    init {

        job = coroutineScope.launch {
            delay(delay.toMillis())
            if (repeat.toMillis() != 0L) {
                while (keepRunning.get()) {
                    tryRun()
                    delay(repeat.toMillis())
                    val currentIter = currentIteration.incrementAndGet()
                    if (iterations != -1 && currentIter >= iterations) {
                        cancel()
                        cancelled()
                        return@launch
                    }
                }
            } else {
                if (keepRunning.get()) {
                    tryRun()
                }
            }
        }
    }

    open fun cancelled() {}

    fun cancel() {
        keepRunning.set(false)
    }

    fun cancelImmediate() {
        cancel()
        job?.cancel()
    }
}