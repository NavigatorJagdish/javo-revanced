package app.revanced.manager.domain.installer

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import app.javo.security.RootArguments.packageName as validatedPackageName
import app.javo.security.RootArguments.propertyValue
import app.javo.security.RootArguments.shellLiteral
import app.revanced.manager.IRootSystemService
import app.revanced.manager.service.ManagerRootService
import app.revanced.manager.util.PM
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import com.topjohnwu.superuser.nio.FileSystemManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.time.withTimeoutOrNull
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Duration

class RootInstaller(
    private val app: Application,
    private val pm: PM
) : ServiceConnection {
    private var remoteFS = CompletableDeferred<FileSystemManager>()

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val ipc = IRootSystemService.Stub.asInterface(service)
        val binder = ipc.fileSystemService

        remoteFS.complete(FileSystemManager.getRemote(binder))
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        remoteFS = CompletableDeferred()
    }

    private suspend fun awaitRemoteFS(): FileSystemManager {
        if (remoteFS.isActive) {
            withContext(Dispatchers.Main) {
                val intent = Intent(app, ManagerRootService::class.java)
                RootService.bind(intent, this@RootInstaller)
            }
        }

        return withTimeoutOrNull(Duration.ofSeconds(20L)) {
            remoteFS.await()
        } ?: throw RootServiceException()
    }

    private suspend fun getShell() = with(CompletableDeferred<Shell>()) {
        Shell.getShell(::complete)

        await()
    }

    private suspend fun execute(vararg commands: String): Shell.Result {
        val stdout = mutableListOf<String>()
        val stderr = mutableListOf<String>()
        return getShell().newJob().add(*commands).to(stdout, stderr).exec()
    }

    fun hasRootAccess() = Shell.isAppGrantedRoot() ?: false

    fun isDeviceRooted() = System.getenv("PATH")?.split(":")?.any { path ->
        File(path, "su").canExecute()
    } ?: false

    suspend fun isAppInstalled(packageName: String) =
        moduleDirectory(packageName).let { awaitRemoteFS().getFile(it).exists() }

    suspend fun isAppMounted(packageName: String) = withContext(Dispatchers.IO) {
        validatedPackageName(packageName)
        pm.getPackageInfo(packageName)?.applicationInfo?.sourceDir?.let {
            execute("mount | grep -F -- ${shellLiteral(it)}").isSuccess
        } ?: false
    }

    suspend fun mount(packageName: String) {
        if (isAppMounted(packageName)) return

        withContext(Dispatchers.IO) {
            val stockAPK = pm.getPackageInfo(packageName)?.applicationInfo?.sourceDir
                ?: throw Exception("Failed to load application info")
            val patchedAPK = "${moduleDirectory(packageName)}/$packageName.apk"

            execute("mount -o bind ${shellLiteral(patchedAPK)} ${shellLiteral(stockAPK)}").assertSuccess("Failed to mount APK")
        }
    }

    suspend fun unmount(packageName: String) {
        if (!isAppMounted(packageName)) return

        withContext(Dispatchers.IO) {
            val stockAPK = pm.getPackageInfo(packageName)?.applicationInfo?.sourceDir
                ?: throw Exception("Failed to load application info")

            execute("umount -l ${shellLiteral(stockAPK)}").assertSuccess("Failed to unmount APK")
        }
    }

    suspend fun install(
        patchedAPK: File,
        stockAPK: File?,
        packageName: String,
        version: String,
        label: String
    ) = withContext(Dispatchers.IO) {
        val modulePath = moduleDirectory(packageName)
        // Render before changing the installed app. Untrusted metadata stays in data fields.
        val service = app.assets.open("root/service.sh").bufferedReader().use { reader ->
            renderTemplate(reader.readText(), mapOf(
                "__PKG_NAME__" to shellLiteral(packageName),
                "__VERSION__" to shellLiteral(version),
            ))
        }
        val properties = app.assets.open("root/module.prop").bufferedReader().use { reader ->
            renderTemplate(reader.readText(), mapOf(
                "__PKG_NAME__" to packageName,
                "__VERSION__" to propertyValue(version),
                "__LABEL__" to propertyValue(label),
            ))
        }
        val stockPath = stockAPK?.absolutePath?.let(::shellLiteral)
        val remoteFS = awaitRemoteFS()

        unmount(packageName)

        stockAPK?.let { stockApp ->
            // TODO: get user id programmatically
            execute("pm uninstall -k --user 0 ${shellLiteral(packageName)}")

            execute("pm install -r -d --user 0 $stockPath")
                .assertSuccess("Failed to install stock app")

            stockApp.delete()
        }

        remoteFS.getFile(modulePath).apply {
            if (!mkdirs() && !exists()) {
                throw Exception("Failed to create module directory")
            }
        }

        mapOf("service.sh" to service, "module.prop" to properties).forEach { (file, content) ->
            remoteFS.getFile("$modulePath/$file").newOutputStream().use {
                it.write(content.toByteArray(Charsets.UTF_8))
            }
        }

        "$modulePath/$packageName.apk".let { apkPath ->
            remoteFS.getFile(patchedAPK.absolutePath)
                .also { if (!it.exists()) throw Exception("File doesn't exist") }
                .newInputStream().use { inputStream ->
                    remoteFS.getFile(apkPath).newOutputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

            execute(
                "chmod 644 ${shellLiteral(apkPath)}",
                "chown system:system ${shellLiteral(apkPath)}",
                "chcon u:object_r:apk_data_file:s0 ${shellLiteral(apkPath)}",
                "chmod +x ${shellLiteral("$modulePath/service.sh")}"
            ).assertSuccess("Failed to set file permissions")
        }
    }

    suspend fun uninstall(packageName: String) {
        val modulePath = moduleDirectory(packageName)
        val remoteFS = awaitRemoteFS()
        if (isAppMounted(packageName))
            unmount(packageName)

        remoteFS.getFile(modulePath).deleteRecursively()
            .also { if (!it) throw Exception("Failed to delete files") }
    }

    companion object {
        const val modulesPath = "/data/adb/modules"
        private val templateTokens = Regex("__PKG_NAME__|__VERSION__|__LABEL__")

        private fun moduleDirectory(packageName: String) = "$modulesPath/${validatedPackageName(packageName)}-revanced"

        private fun renderTemplate(template: String, values: Map<String, String>) =
            templateTokens.replace(template) { values.getValue(it.value) }

        private fun Shell.Result.assertSuccess(errorMessage: String) {
            if (!isSuccess) {
                throw ShellCommandException(
                    errorMessage,
                    code,
                    out,
                    err
                )
            }
        }
    }
}

class ShellCommandException(
    val userMessage: String,
    val exitCode: Int,
    val stdout: List<String>,
    val stderr: List<String>
) : Exception(format(userMessage, exitCode, stdout, stderr)) {
    companion object {
        private fun format(
            message: String,
            exitCode: Int,
            stdout: List<String>,
            stderr: List<String>
        ): String =
            buildString {
                appendLine(message)
                appendLine("Exit code: $exitCode")

                val output = stdout.filter { it.isNotBlank() }
                val errors = stderr.filter { it.isNotBlank() }

                if (output.isNotEmpty()) {
                    appendLine("stdout:")
                    output.forEach(::appendLine)
                }
                if (errors.isNotEmpty()) {
                    appendLine("stderr:")
                    errors.forEach(::appendLine)
                }
            }
    }
}

class RootServiceException : Exception("Root not available")
