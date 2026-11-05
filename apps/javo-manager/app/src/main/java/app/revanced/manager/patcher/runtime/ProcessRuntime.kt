package app.revanced.manager.patcher.runtime

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import app.revanced.manager.BuildConfig
import app.revanced.manager.patcher.runtime.process.IPatcherEvents
import app.revanced.manager.patcher.runtime.process.IPatcherProcess
import app.revanced.manager.patcher.LibraryResolver
import app.revanced.manager.patcher.ProgressEvent
import app.revanced.manager.patcher.ProgressEventParcel
import app.revanced.manager.patcher.logger.Logger
import app.revanced.manager.patcher.runtime.process.Parameters
import app.revanced.manager.patcher.runtime.process.PatchConfiguration
import app.revanced.manager.patcher.runtime.process.PatcherProcess
import app.revanced.manager.patcher.toEvent
import app.revanced.manager.util.Options
import app.revanced.manager.util.PM
import app.revanced.manager.util.PatchSelection
import app.revanced.manager.util.tag
import com.github.pgreze.process.Redirect
import com.github.pgreze.process.process
import java.io.IOException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.koin.core.component.inject

/**
 * Runs the patcher in another process by using the app_process binary and IPC.
 */
class ProcessRuntime(private val context: Context) : Runtime(context) {
    private val pm: PM by inject()

    private suspend fun awaitBinderConnection(): IPatcherProcess {
        val binderFuture = CompletableDeferred<IPatcherProcess>()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val binder =
                    intent.getBundleExtra(INTENT_BUNDLE_KEY)?.getBinder(BUNDLE_BINDER_KEY)!!

                binderFuture.complete(IPatcherProcess.Stub.asInterface(binder))
            }
        }

        ContextCompat.registerReceiver(context, receiver, IntentFilter().apply {
            addAction(CONNECT_TO_APP_ACTION)
        }, ContextCompat.RECEIVER_NOT_EXPORTED)

        return try {
            withTimeout(10000L) {
                binderFuture.await()
            }
        } catch (error: TimeoutCancellationException) {
            currentCoroutineContext().ensureActive()
            throw IOException("Patcher process did not connect within 10 seconds. Export the logs and check the runtime setting.", error)
        } finally {
            context.unregisterReceiver(receiver)
        }
    }

    override suspend fun execute(
        inputFile: String,
        outputFile: String,
        packageName: String,
        selectedPatches: PatchSelection,
        options: Options,
        logger: Logger,
        onEvent: (ProgressEvent) -> Unit,
    ) = coroutineScope {
        // Get the location of our own Apk.
        val managerBaseApk = pm.getPackageInfo(context.packageName)!!.applicationInfo!!.sourceDir

        val requested = prefs.patcherProcessMemoryLimit.get().coerceIn(128, 4096)
        val memory = ActivityManager.MemoryInfo().also {
            context.getSystemService(ActivityManager::class.java).getMemoryInfo(it)
        }
        // ART's region collector can reserve twice the configured maximum heap.
        val deviceCap = (memory.totalMem / (4 * 1024 * 1024)).toInt().coerceAtLeast(128)
        // Older ART reserves a contiguous low-address region twice the heap size.
        // A 985 MB heap failed before Java initialization on Android 10; 700 MB started.
        val addressSpaceCap = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) 700 else 4096
        val limitMb = minOf(requested, deviceCap, addressSpaceCap)
        val limit = "${limitMb}M"
        logger.info("Separate patcher heap limit $limitMb MB (requested $requested MB, device RAM ${memory.totalMem / (1024 * 1024)} MB)")
        val propOverride = resolvePropOverride(context)?.absolutePath
            ?: throw Exception("Couldn't find prop override library")

        val env =
            System.getenv().toMutableMap().apply {
                putAll(
                    mapOf(
                        "CLASSPATH" to managerBaseApk,
                        // Override the props used by ART to set the memory limit.
                        "LD_PRELOAD" to propOverride,
                        "PROP_dalvik.vm.heapgrowthlimit" to limit,
                        "PROP_dalvik.vm.heapsize" to limit,
                    )
                )
            }

        val connection = async(start = CoroutineStart.UNDISPATCHED) { awaitBinderConnection() }
        val patching = CompletableDeferred<Unit>()
        launch(Dispatchers.IO) {
            val result = process(
                APP_PROCESS_BIN_PATH,
                "-Djava.io.tmpdir=$cacheDir", // The process will use /tmp if this isn't set, which is a problem because that folder is not accessible on Android.
                "/", // The unused cmd-dir parameter
                "--nice-name=${context.packageName}:Patcher",
                PatcherProcess::class.java.name, // The class with the main function.
                context.packageName,
                env = env,
                stdout = Redirect.CAPTURE,
                stderr = Redirect.CAPTURE,
            ) { line ->
                // The process shouldn't generally be writing to stdio. Log any lines we get as warnings.
                logger.warn("[STDIO]: $line")
            }

            Log.d(tag, "Process finished with exit code ${result.resultCode}")

            if (!patching.isCompleted) {
                patching.completeExceptionally(Exception("Patcher process stopped before completion (exit ${result.resultCode}). Check available memory and export the logs.\n" + result.output.takeLast(20).joinToString("\n").takeLast(4096)))
            }
        }

        launch(Dispatchers.IO) {
            val binder = connection.await()

            // Android Studio's fast deployment feature causes an issue where the other process will be running older code compared to the main process.
            // The patcher process might be running outdated code if the fast deployment feature is used.
            // To fix it, clear the cache in the Android settings or disable fast deployment (Run configurations -> Edit Configurations -> app -> Enable "always deploy with package manager").
            if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                logger.warn("External patcher process: app_process could be running outdated code. To resolve stale code, clear the app cache or disable Android 11 deployment optimizations in your IDE.")
            }

            val eventHandler = object : IPatcherEvents.Stub() {
                override fun log(level: String, msg: String) = logger.log(enumValueOf(level), msg)

                override fun event(event: ProgressEventParcel?) {
                    event?.let { onEvent(it.toEvent()) }
                }

                override fun finished(exceptionStackTrace: String?) {
                    exceptionStackTrace?.let {
                        patching.completeExceptionally(RemoteFailureException(it))
                        binder.exit()
                        return
                    }
                    patching.complete(Unit)
                    binder.exit()
                }
            }

            val parameters = Parameters(
                aaptPath = aaptPath,
                frameworkDir = frameworkPath,
                cacheDir = cacheDir,
                packageName = packageName,
                inputFile = inputFile,
                outputFile = outputFile,
                configurations = bundles().filterKeys { it in selectedPatches }.map { (uid, bundle) ->
                    PatchConfiguration(
                        bundle,
                        selectedPatches[uid].orEmpty(),
                        options[uid].orEmpty()
                    )
                },
                minLogLevel = prefs.minPatcherLogLevel.get()
            )

            binder.start(parameters, eventHandler)
        }

        // Wait until patching finishes.
        withTimeout(20 * 60 * 1000L) { patching.await() }
    }

    companion object : LibraryResolver() {
        private const val APP_PROCESS_BIN_PATH = "/system/bin/app_process"

        const val CONNECT_TO_APP_ACTION = "CONNECT_TO_APP_ACTION"
        const val INTENT_BUNDLE_KEY = "BUNDLE"
        const val BUNDLE_BINDER_KEY = "BINDER"

        private fun resolvePropOverride(context: Context) = findLibrary(context, "prop_override")
    }

    /**
     * An [Exception] occurred in the remote process while patching.
     *
     * @param originalStackTrace The stack trace of the original [Exception].
     */
    class RemoteFailureException(val originalStackTrace: String) : Exception()
}


