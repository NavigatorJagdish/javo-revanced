package app.javo.integration

import android.app.Activity
import android.app.Instrumentation
import android.content.pm.PackageManager
import android.os.Bundle
import app.javo.security.UpdateSecurity
import app.revanced.library.ApkUtils
import app.revanced.manager.patcher.Session
import app.revanced.manager.patcher.aapt.Aapt
import app.revanced.manager.patcher.logger.LogLevel
import app.revanced.manager.patcher.logger.Logger
import app.revanced.manager.patcher.patch.PatchBundle
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.json.JSONArray
import java.io.File
import java.security.MessageDigest

/** Exercises the production bundle loader, Session and signer against first-party code only. */
class PatchIntegrationInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val report = JSONObject().put("fixture", "first-party; no YouTube code")
            .put("playback", "not-tested").put("accountLogin", "not-tested")
        val folder = File(targetContext.filesDir, "integration").also {
            check(!it.exists() || it.deleteRecursively())
            check(it.mkdirs())
        }
        try {
            report.put("managerApkSha256",UpdateSecurity.sha256(File(targetContext.applicationInfo.sourceDir)))
            fun asset(name: String) = File(folder, name).also { file ->
                if (file.exists()) { file.setWritable(true); check(file.delete()) }
                context.assets.open(name).use { input -> file.outputStream().use { input.copyTo(it) } }
            }
            report.put("browserChecks", checkBrowser(this))
            val bundle = asset("javo-branding.rvp").also { check(it.setReadOnly()) }
            val videoFixture = asset("fixture-video.apk")
            val apkmFixture = asset("fixture-video.apkm")
            report.put("downloadRegressions", runBlocking { checkDownloads(targetContext, videoFixture, apkmFixture, folder) })
            report.put("httpsTransfer", runBlocking { checkHttpsDownload(targetContext, apkmFixture, videoFixture, folder, context.assets.open("transfer-test.p12").use { it.readBytes() }) })
            report.put("bundleSha256", MessageDigest.getInstance("SHA-256").digest(bundle.readBytes()).joinToString("") { "%02x".format(it) })
            val official = JSONObject().put("patchExecution", "not-tested")
            report.put("officialCatalog", official)
            try {
                val file = asset("official-inspected.rvp").also { check(it.setReadOnly()) }
                val source = PatchBundle(file.absolutePath)
                val metadata = PatchBundle.Loader.metadata(listOf(source)).getValue(source).getOrThrow()
                check(metadata.isNotEmpty())
                official.put("metadataLoadedOnAndroid",true).put("patchCount",metadata.size)
                    .put("sha256",MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) })
            } catch (error: Throwable) {
                official.put("metadataLoadedOnAndroid",false).put("failure",error.stackTraceToString())
            }
            val products = JSONArray()
            report.put("products", products)
            for ((id, packageName, label) in listOf(
                Triple("video", "com.google.android.youtube", "Javo Video"),
                Triple("music", "com.google.android.apps.youtube.music", "Javo Music")
            )) {
            val fixture = if (id == "video") videoFixture else asset("fixture-$id.apk")
            val patches = PatchBundle.Loader.patches(listOf(PatchBundle(bundle.absolutePath)), packageName)
                .values.flatten()
            check(patches.map { it.name }.toSet() == setOf("$label branding")) { "Unexpected compatible patches: $patches" }
            report.put("androidBundleLoading", true)
            val unsigned = File(folder, "$id-patched-unsigned.apk")
            val logger = object : Logger() {
                override fun log(level: LogLevel, message: String) { android.util.Log.i("JavoIntegration", "$level: $message") }
            }
            if (id == "video") {
                runBlocking {
                    val repository = org.koin.core.context.GlobalContext.get().get<app.revanced.manager.domain.repository.PatchBundleRepository>()
                    repository.createLocal { bundle.inputStream() }
                    val imported = kotlinx.coroutines.withTimeout(30_000) {
                        repository.bundles.first { values -> values.values.any { it.manifestAttributes?.name == "Javo resource branding" } }
                    }
                    val uid = imported.entries.first { it.value.manifestAttributes?.name == "Javo resource branding" }.key
                    try {
                        kotlinx.coroutines.withTimeout(20_000) {
                            app.revanced.manager.patcher.runtime.ProcessRuntime(targetContext).execute(
                                fixture.absolutePath, File(folder, "killed-output.apk").absolutePath, packageName,
                                mapOf(uid to setOf("$label branding")), emptyMap(), logger,
                                { event ->
                                    if (event is app.revanced.manager.patcher.ProgressEvent.Started && event.stepId == app.revanced.manager.patcher.StepId.LoadPatches) {
                                        val command = java.lang.Runtime.getRuntime().exec(arrayOf("/system/bin/pidof", "${targetContext.packageName}:Patcher"))
                                        val pid = command.inputStream.bufferedReader().readText().trim().toInt()
                                        check(pid != android.os.Process.myPid())
                                        android.os.Process.killProcess(pid)
                                    }
                                }
                            )
                        }
                        error("Terminated patcher process was accepted")
                    } catch (error: Exception) {
                        check(error.message.orEmpty().contains("stopped before completion")) { error.stackTraceToString() }
                        report.put("terminatedProcessRecovery", true)
                    }
                    val events = java.util.Collections.synchronizedList(mutableListOf<app.revanced.manager.patcher.ProgressEvent>())
                    kotlinx.coroutines.withTimeout(180_000) {
                        app.revanced.manager.patcher.runtime.ProcessRuntime(targetContext).execute(
                            fixture.absolutePath, unsigned.absolutePath, packageName,
                            mapOf(uid to setOf("$label branding")), emptyMap(), logger, { events.add(it) }
                        )
                    }
                    check(events.any { it is app.revanced.manager.patcher.ProgressEvent.Progress && it.message.orEmpty().contains("memory") })
                    report.put("separateProcessPatch", true)
                }
            } else runBlocking {
                Session(File(folder,"cache").absolutePath, File(folder,"framework").also { it.mkdirs() }.absolutePath,
                    checkNotNull(Aapt.binary(targetContext)).absolutePath, logger, fixture, {}, LogLevel.INFO).use {
                    it.run(unsigned, patches.toList())
                }
            }
            report.put("productionSessionPatchedFixture", true)
            val signed = File(folder, "$id-patched-signed.apk")
            val key = File(targetContext.cacheDir,"fixture-test.keystore")
            ApkUtils.signApk(unsigned, signed, "Javo fixture test", ApkUtils.KeyStoreDetails(key, alias="fixture", password="fixture"))
            @Suppress("DEPRECATION")
            val info = checkNotNull(targetContext.packageManager.getPackageArchiveInfo(signed.absolutePath, PackageManager.GET_SIGNATURES))
            check(info.packageName == packageName)
            val app = checkNotNull(info.applicationInfo).apply { sourceDir=signed.absolutePath; publicSourceDir=signed.absolutePath }
            check(app.loadLabel(targetContext.packageManager).toString() == label)
            check(!info.signatures.isNullOrEmpty()) { "Signed fixture has no signer" }
            products.put(JSONObject().put("id",id).put("packageName",packageName).put("sourceApkSha256",UpdateSecurity.sha256(fixture))
                .put("brandingLabelVerified", true).put("signedOutput", true)
                .put("signedApkSha256",MessageDigest.getInstance("SHA-256").digest(signed.readBytes()).joinToString("") { "%02x".format(it) })
                .put("success",true))
            key.delete()
            }
            report.put("success",true)
            File(folder,"result.json").writeText(report.toString(2))
            finish(Activity.RESULT_OK, Bundle().apply { putString("result",report.toString()) })
        } catch (error: Throwable) {
            report.put("success",false).put("failure",error.stackTraceToString())
            File(folder,"result.json").writeText(report.toString(2))
            android.util.Log.e("JavoIntegration","Integration check failed",error)
            finish(Activity.RESULT_CANCELED, Bundle().apply { putString("result",report.toString()) })
        }
    }
}
