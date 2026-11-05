package app.revanced.manager.patcher

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Reports measured elapsed time and heap usage while a synchronous patcher stage runs. */
internal suspend fun <T> withPatcherActivity(
    step: StepId,
    title: String,
    onEvent: (ProgressEvent) -> Unit,
    block: suspend () -> T,
): T = coroutineScope {
    val started = System.nanoTime()
    val emitStatus = {
        val memory = java.lang.Runtime.getRuntime()
        val used = (memory.totalMemory() - memory.freeMemory()) / (1024 * 1024)
        val max = memory.maxMemory() / (1024 * 1024)
        val seconds = (System.nanoTime() - started) / 1_000_000_000L
        onEvent(ProgressEvent.Progress(step, message = "$title · ${seconds}s · memory $used/$max MB"))
    }
    emitStatus()
    val status = launch(Dispatchers.IO) {
        while (isActive) {
            delay(1000)
            emitStatus()
        }
    }
    try { withContext(Dispatchers.Default) { block() } }
    finally { status.cancelAndJoin() }
}
